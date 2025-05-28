package com.cdu.heshan.dataobj;

import com.cdu.heshan.model.ConversationHistory;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ConversationHistoryDO implements Serializable {

    private long id;               // 唯一标识每条记录
    private String userInput;      // 用户输入的提示词
    private String aiResponse;     // AI 的回复
    private LocalDateTime timestamp;  // 创建时间戳
    private long userId;           // 关联的用户ID

    // Getter 和 Setter 方法

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    /**
     * DO 转换为 Model
     *
     * @return User 对象
     */
    public ConversationHistory toModel() {
        ConversationHistory conversationHistory = new ConversationHistory();
        conversationHistory.setId(getId());
        conversationHistory.setUserInput(getUserInput());
        conversationHistory.setAiResponse(getAiResponse());
        conversationHistory.setTimestamp(getTimestamp());
        conversationHistory.setUserId(getUserId());
        return conversationHistory;
    }
    public static ConversationHistoryDO fromModel(ConversationHistory model) {
        if (model == null) {
            return null;
        }

        ConversationHistoryDO historyDO = new ConversationHistoryDO();
        historyDO.setId(model.getId());
        historyDO.setUserInput(model.getUserInput());
        historyDO.setAiResponse(model.getAiResponse());
        historyDO.setTimestamp(model.getTimestamp());
        historyDO.setUserId(model.getUserId());

        return historyDO;
    }
}
