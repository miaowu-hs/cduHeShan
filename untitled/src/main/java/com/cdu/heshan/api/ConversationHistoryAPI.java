package com.cdu.heshan.api;

import com.cdu.heshan.model.ConversationHistory;
import com.cdu.heshan.model.Result;
import com.cdu.heshan.service.ConversationHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author heshan
 * 对话记录的API
 */
@Controller
@RequestMapping("/api/conversation")
public class ConversationHistoryAPI {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationHistoryAPI.class);

    @Autowired
    private ConversationHistoryService conversationHistoryService;

    /**
     * 根据用户ID获取对话记录
     * @param userId 用户ID
     * @param model
     * @return
     */
    @GetMapping("/list")
    public String getConversationHistoryByUserId(@RequestParam("userId") long userId, Model model) {
        Result<List<ConversationHistory>> result = conversationHistoryService.findByUserId(userId);
        LOG.warn("获取用户对话记录结果：{}", result);

        if (result != null && result.isSuccess()) {
            model.addAttribute("conversationHistory", result.getData());
            return "conversationList";  // 返回展示对话记录的页面
        } else {
            model.addAttribute("info", "未找到相关对话记录！");
            return "error";  // 返回错误页面
        }
    }

    /**
     * 添加对话记录
     * @param conversationHistory 对话记录对象
     * @param model
     * @return
     */
    @PostMapping("/add")
    public String addConversationHistory(@ModelAttribute ConversationHistory conversationHistory, Model model) {
        Result<ConversationHistory> result = conversationHistoryService.add(conversationHistory);
        LOG.warn("添加对话记录结果：{}", result);

        if (result != null && result.isSuccess()) {
            model.addAttribute("info", "对话记录添加成功！");
            return "redirect:/api/conversation/list?userId=" + conversationHistory.getUserId();  // 跳转到对话记录列表页面
        } else {
            model.addAttribute("info", "添加对话记录失败！");
            return "error";
        }
    }

    /**
     * 删除对话记录
     * @param id 对话记录ID
     * @param model
     * @return
     */
    @PostMapping("/delete")
    public String deleteConversationHistory(@RequestParam("id") long id, Model model) {
        Result<Void> result = conversationHistoryService.delete(id);
        LOG.warn("删除对话记录结果：{}", result);

        if (result != null && result.isSuccess()) {
            model.addAttribute("info", "对话记录删除成功！");
            return "redirect:/api/conversation/list?userId=" + id;  // 返回到对话记录列表页面
        } else {
            model.addAttribute("info", "删除对话记录失败！");
            return "error";  // 返回错误页面
        }
    }

}
