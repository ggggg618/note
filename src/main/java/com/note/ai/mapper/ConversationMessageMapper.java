package com.note.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.ai.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
}
