package com.cdu.heshan.control;

import com.cdu.heshan.dataobj.ConversationHistoryDO;
import com.cdu.heshan.model.ConversationHistory;
import com.cdu.heshan.model.User;
import com.cdu.heshan.service.ConversationHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@Controller
@RequestMapping("/api/chat")
public class ChatController {

    private static final String CONVERSATION_CONTEXT_KEY = "conversation:context:";
    private static final String HISTORY_KEY_PREFIX = "history:";
    private static final int MAX_CONTEXT_MESSAGES = 10; // 最大上下文消息数
    private static final int HISTORY_TTL_HOURS = 24; // 历史记录保存时间(小时)

    // 模型配置映射
    private final Map<String, ModelConfig> modelConfigMap = new HashMap<>();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ConversationHistoryService conversationHistoryService;

    @Autowired
    public ChatController(
            @Value("${ai.api.key.deepseek}") String deepseekApiKey,
            @Value("${ai.api.endpoint.deepseek}") String deepseekApiUrl,
            @Value("${ai.api.key.doubao}") String doubaoApiKey,
            @Value("${ai.api.endpoint.doubao}") String doubaoApiUrl
    ) {
        // 初始化模型配置映射
        modelConfigMap.put("deepseek", new ModelConfig(deepseekApiUrl, deepseekApiKey, "deepseek-chat"));
        modelConfigMap.put("doubao", new ModelConfig(doubaoApiUrl, doubaoApiKey, "doubao-pro"));
    }

    @PostMapping("/send")
    public String chatWithAIWeb(
            @RequestParam("prompt") String prompt,
            @RequestParam("model") String model, // 添加模型参数
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = (User) request.getSession().getAttribute("user");
            if (user == null) {
                return "redirect:/user/login";
            }

            String aiResponse = callAiApi(user.getId(), prompt, model);

            ConversationHistory history = createHistoryRecord(user.getId(), prompt, aiResponse);
            saveHistoryRecord(history, user.getId());

            return "redirect:/api/chat/main/dialog";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "AI服务异常: " + e.getMessage());
            return "redirect:/api/chat/main/dialog";
        }
    }

    @GetMapping("/main/dialog")
    public String mainDialog(HttpServletRequest request, Model model) {
        if (!isUserLoggedIn(request)) {
            return "redirect:/user/login";
        }

        User user = (User) request.getSession().getAttribute("user");
        List<Map<String, Object>> formattedHistories = getFormattedHistories(user.getId());

        model.addAttribute("histories", formattedHistories);
        return "dialog";
    }

    @GetMapping("/clear-context")
    public String clearConversationContext(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if (user != null) {
            redisTemplate.delete(CONVERSATION_CONTEXT_KEY + user.getId());
            redisTemplate.delete(HISTORY_KEY_PREFIX + user.getId());
        }
        return "redirect:/api/chat/main/dialog";
    }

    // 修改：添加model参数
    private String callAiApi(Long userId, String prompt, String model) throws IOException {
        // 从配置映射中获取模型信息
        ModelConfig config = modelConfigMap.getOrDefault(model, modelConfigMap.get("deepseek"));

        List<Map<String, String>> messages = getConversationContext(userId);
        messages.add(Map.of("role", "user", "content", prompt));
        messages = truncateContext(messages);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(config.getApiUrl());
            post.setHeader("Authorization", "Bearer " + config.getApiKey());
            post.setHeader("Content-Type", "application/json");

            Map<String, Object> requestBody = Map.of(
                    "model", config.getModelName(),
                    "messages", messages
            );

            String json = new ObjectMapper().writeValueAsString(requestBody);
            post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                Map<String, Object> responseMap = new ObjectMapper().readValue(responseBody, Map.class);

                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String aiResponse = (String) message.get("content");

                messages.add(Map.of("role", "assistant", "content", aiResponse));
                saveConversationContext(userId, messages);

                return aiResponse;
            }
        }
    }

    private ConversationHistory createHistoryRecord(Long userId, String prompt, String aiResponse) {
        ConversationHistory history = new ConversationHistory();
        history.setUserInput(prompt);
        history.setAiResponse(markdownToHtml(aiResponse));
        history.setUserId(userId);
        history.setTimestamp(LocalDateTime.now());
        return history;
    }

    private void saveHistoryRecord(ConversationHistory history, Long userId) {
        // 保存到数据库
        conversationHistoryService.add(history);

        // 保存到Redis
        String historyKey = HISTORY_KEY_PREFIX + userId;
        redisTemplate.opsForList().leftPush(historyKey, history);
        redisTemplate.expire(historyKey, HISTORY_TTL_HOURS, TimeUnit.HOURS);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getConversationContext(Long userId) {
        String key = CONVERSATION_CONTEXT_KEY + userId;
        Object context = redisTemplate.opsForValue().get(key);
        return context != null ? (List<Map<String, String>>) context : new ArrayList<>();
    }

    private void saveConversationContext(Long userId, List<Map<String, String>> messages) {
        String key = CONVERSATION_CONTEXT_KEY + userId;
        redisTemplate.opsForValue().set(key, messages);
        redisTemplate.expire(key, HISTORY_TTL_HOURS, TimeUnit.HOURS);
    }

    private List<Map<String, String>> truncateContext(List<Map<String, String>> messages) {
        if (messages.size() > MAX_CONTEXT_MESSAGES) {
            return messages.subList(messages.size() - MAX_CONTEXT_MESSAGES, messages.size());
        }
        return messages;
    }

    private List<Map<String, Object>> getFormattedHistories(Long userId) {
        String key = HISTORY_KEY_PREFIX + userId;
        List<Object> rawHistoryList = redisTemplate.opsForList().range(key, 0,
                redisTemplate.opsForList().size(key) <= 3 ? -1 : 3);

        List<ConversationHistory> historyList = rawHistoryList.stream()
                .map(item -> {
                    if (item instanceof LinkedHashMap) {
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.registerModule(new JavaTimeModule());
                        return mapper.convertValue(item, ConversationHistory.class);
                    }
                    return (ConversationHistory) item;
                })
                .collect(Collectors.toList());

        Collections.reverse(historyList);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return historyList.stream()
                .map(history -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", history.getId());
                    map.put("userInput", history.getUserInput());
                    map.put("aiResponse", history.getAiResponse());
                    map.put("timestamp", history.getTimestamp().format(formatter));
                    map.put("userId", history.getUserId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private String markdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(parser.parse(markdown));
    }

    private boolean isUserLoggedIn(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        return user != null;
    }

    // 内部类：模型配置
    private static class ModelConfig {
        private final String apiUrl;
        private final String apiKey;
        private final String modelName;

        public ModelConfig(String apiUrl, String apiKey, String modelName) {
            this.apiUrl = apiUrl;
            this.apiKey = apiKey;
            this.modelName = modelName;
        }

        public String getApiUrl() { return apiUrl; }
        public String getApiKey() { return apiKey; }
        public String getModelName() { return modelName; }
    }
}