package com.yyh.blogserver.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yyh.blogserver.Model.AiConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话的数据访问接口。
 *
 * <p>MyBatis-Plus 会在运行时生成代理实现，不需要手写实现类。</p>
 */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}
