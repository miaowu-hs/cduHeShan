package com.cdu.heshan.dao;

import com.cdu.heshan.dataobj.ConversationHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConversationHistoryDAO {

    /**
     * 批量插入对话历史记录
     * @param conversationHistoryList 对话历史记录列表
     * @return 插入的记录数
     */
    int batchAdd(@Param("list") List<ConversationHistoryDO> conversationHistoryList);

    /**
     * 根据多个 ID 查询对话历史记录
     * @param ids 对话历史记录ID列表
     * @return 对话历史记录列表
     */
    List<ConversationHistoryDO> findByIds(@Param("ids") List<Long> ids);

    /**
     * 插入一条对话历史记录
     * @param conversationHistory 对话历史记录对象
     * @return 插入的记录数
     */
    int add(ConversationHistoryDO conversationHistory);

    /**
     * 更新一条对话历史记录
     * @param conversationHistory 对话历史记录对象
     * @return 更新的记录数
     */
    int update(ConversationHistoryDO conversationHistory);

    /**
     * 删除一条对话历史记录
     * @param id 对话历史记录ID
     * @return 删除的记录数
     */
    int delete(@Param("id") long id);

    /**
     * 查询所有对话历史记录
     * @return 对话历史记录列表
     */
    List<ConversationHistoryDO> findAll();

    /**
     * 根据用户ID查询对话历史记录
     * @param userId 用户ID
     * @return 对话历史记录列表
     */
    List<ConversationHistoryDO> findByUserId(@Param("userId") long userId);

    /**
     * 根据关键字进行查询
     * @param keyWord 关键字
     * @return 对话历史记录列表
     */
    List<ConversationHistoryDO> query(@Param("keyWord") String keyWord);

    /**
     * 根据时间范围和关键字进行搜索
     * @param keyWord 关键字
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @return 对话历史记录列表
     */
    List<ConversationHistoryDO> search(@Param("keyWord") String keyWord,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
}
