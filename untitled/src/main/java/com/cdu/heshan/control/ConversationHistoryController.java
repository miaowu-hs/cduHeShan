package com.cdu.heshan.control;

import com.cdu.heshan.model.ConversationHistory;
import com.cdu.heshan.model.Result;
import com.cdu.heshan.model.User;
import com.cdu.heshan.service.ConversationHistoryService;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 对话历史记录控制器
 */
@Controller
@RequestMapping("/history")
public class ConversationHistoryController {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationHistoryController.class);

    @Autowired
    private ConversationHistoryService conversationHistoryService;

    /**
     * 显示用户对话历史记录页面
     */
    @GetMapping("/list")
    public String showHistoryList(HttpServletRequest request, Model model) {
        // 从Session中获取当前登录用户
        User user= (User) request.getSession().getAttribute("user");
        if (user == null) {
            LOG.warn("用户未登录，重定向到登录页面");
            return "redirect:/user/login";
        }

        // 获取用户ID
        Long userId = getUserIdFromSessionUser(user);
        if (userId == null || userId <= 0) {
            LOG.error("无效的用户ID: {}", userId);
            model.addAttribute("error", "用户信息异常，请重新登录");
            return "error";
        }

        model.addAttribute("user", user);

        // 调用服务获取对话历史记录
        Result<List<ConversationHistory>> result = conversationHistoryService.findByUserId(userId);

        // 处理服务返回结果
        if (result == null) {
            LOG.error("服务返回结果为空，userId: {}", userId);
            model.addAttribute("error", "系统错误，请稍后重试");
            return "error";
        }

        if (!result.isSuccess()) {
            LOG.warn("获取对话历史记录失败，code: {}, message: {}", result.getCode(), result.getMessage());
            model.addAttribute("error", result.getMessage());
            return "error";
        }

        List<ConversationHistory> historyList = result.getData();
        historyList.forEach((conversationHistory -> {
            conversationHistory.setAiResponse(markdownToHtml(conversationHistory.getAiResponse()));
        }));
        if (historyList != null && !historyList.isEmpty()) {
            Collections.reverse(historyList);
            model.addAttribute("conversationHistory", historyList);
            LOG.info("成功获取用户ID为{}的对话历史记录，共{}条", userId, historyList.size());
        } else {
            LOG.info("用户ID为{}的对话历史记录为空", userId);
            model.addAttribute("emptyMessage", "暂无对话历史记录");
        }

        return "conversationHistory";
    }

    /**
     * 处理删除历史记录请求
     */
    @PostMapping("/delete")
    public String deleteHistory(@RequestParam("id") Long id,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        // 验证用户登录状态
        Object userObj = request.getSession().getAttribute("user");
        if (userObj == null) {
            LOG.warn("用户未登录，重定向到登录页面");
            return "redirect:/user/login";
        }

        // 安全检查：确保用户只能删除自己的记录
        Long userId = getUserIdFromSessionUser(userObj);

        // 调用服务删除记录
        Result<Void> result = conversationHistoryService.delete(id);

        // 处理服务返回结果
        if (result == null) {
            LOG.error("服务返回结果为空，id: {}", id);
            redirectAttributes.addFlashAttribute("errorMsg", "系统错误，请稍后重试");
            return "redirect:/history/list";
        }

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("successMsg", "对话记录已成功删除");
            LOG.info("用户{}成功删除对话记录，id: {}", userId, id);
        } else {
            // 根据服务层返回的错误码提供更具体的错误信息
            String errorMsg = getErrorMessageByCode(result.getCode());
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
            LOG.error("删除对话记录失败，id: {}, code: {}, message: {}", id, result.getCode(), result.getMessage());
        }

        return "redirect:/history/list";
    }

    @GetMapping("/error")
    public String showErrorPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String errorDetails,
            HttpServletRequest request,
            Model model) {

        // 从Session中获取当前登录用户
        Object userObj = request.getSession().getAttribute("user");
        if (userObj != null) {
            model.addAttribute("user", userObj);
        }

        // 设置错误信息
        model.addAttribute("error", error != null ? error : "发生了未知错误，请稍后重试");
        model.addAttribute("errorCode", errorCode);
        model.addAttribute("errorDetails", errorDetails);

        // 记录错误日志
        LOG.warn("显示错误页面 - 错误码: {}, 错误信息: {}", errorCode, error);

        return "error";
    }

    /**
     * 根据错误码获取错误消息
     */
    private String getErrorMessageByCode(String code) {
        switch (code) {
            case "703":
                return "无效的记录ID";
            case "704":
                return "记录不存在或已被删除";
            default:
                return "操作失败，请重试";
        }
    }

    /**
     * 从Session中的用户对象获取userId
     */
    private Long getUserIdFromSessionUser(Object userObj) {
        // 根据实际的User类结构调整
        if (userObj instanceof com.cdu.heshan.model.User) {
            return ((com.cdu.heshan.model.User) userObj).getId();
        }
        LOG.error("Session中的用户对象类型不匹配: {}", userObj.getClass().getName());
        return null;
    }
    private String markdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(parser.parse(markdown));
    }
}