package com.yyh.blogserver.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yyh.blogserver.Model.AiMessage;
import org.apache.ibatis.annotations.Mapper;

/** AI 消息的数据访问接口。 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}
