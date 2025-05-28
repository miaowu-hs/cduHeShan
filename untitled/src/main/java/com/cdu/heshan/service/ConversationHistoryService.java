package com.cdu.heshan.service;

import com.cdu.heshan.model.ConversationHistory;
import com.cdu.heshan.model.Result;

import java.util.List;

public interface ConversationHistoryService {

    Result<List<ConversationHistory>>  findByUserId(long userId);

    Result<ConversationHistory> add(ConversationHistory conversationHistory);

    Result<Void> delete(long id);


}
