package com.cdu.heshan.service.impl;

import com.cdu.heshan.dao.ConversationHistoryDAO;
import com.cdu.heshan.dataobj.ConversationHistoryDO;
import com.cdu.heshan.model.ConversationHistory;
import com.cdu.heshan.model.Result;
import com.cdu.heshan.service.ConversationHistoryService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConversationHistoryServiceImpl implements ConversationHistoryService {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationHistoryServiceImpl.class);

    @Autowired
    private ConversationHistoryDAO conversationHistoryDAO;

    @Override
    public Result<List<ConversationHistory>> findByUserId(long userId) {
        Result<List<ConversationHistory>> result = new Result<>();

        if (userId <= 0) {
            result.setCode("700");
            result.setMessage("用户ID非法");
            return result;
        }

        List<ConversationHistoryDO> historyDOList = conversationHistoryDAO.findByUserId(userId);

        List<ConversationHistory> models = historyDOList.stream()
                .map(ConversationHistoryDO::toModel)
                .collect(Collectors.toList());

        result.setSuccess(true);
        result.setData(models);
        return result;
    }

    @Override
    public Result<ConversationHistory> add(ConversationHistory conversationHistory) {
        Result<ConversationHistory> result = new Result<>();

        if (conversationHistory == null || conversationHistory.getUserId() <= 0 || StringUtils.isBlank(conversationHistory.getAiResponse())) {
            result.setCode("701");
            result.setMessage("参数非法：用户ID或内容不能为空");
            return result;
        }

        ConversationHistoryDO historyDO = ConversationHistoryDO.fromModel(conversationHistory);
        historyDO.setTimestamp(LocalDateTime.now());

        int rows = conversationHistoryDAO.add(historyDO);

        if (rows > 0) {
            result.setSuccess(true);
            conversationHistory.setId(historyDO.getId()); // 若支持自增ID回填
            result.setData(conversationHistory);
        } else {
            result.setCode("702");
            result.setMessage("添加对话失败");
        }

        return result;
    }

    @Override
    public Result<Void> delete(long id) {
        Result<Void> result = new Result<>();

        if (id <= 0) {
            result.setCode("703");
            result.setMessage("ID非法");
            return result;
        }

        int rows = conversationHistoryDAO.delete(id);

        if (rows > 0) {
            result.setSuccess(true);
        } else {
            result.setCode("704");
            result.setMessage("删除对话记录失败");
        }

        return result;
    }
}
