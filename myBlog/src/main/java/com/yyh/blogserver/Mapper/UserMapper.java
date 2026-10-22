package com.yyh.blogserver.Mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yyh.blogserver.Model.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link User} 记录的数据访问边界。
 *
 * <p>这里不需要，也不应该手写 {@code UserMapperImpl}。应用启动时，MyBatis-Plus 会创建
 * 该接口的运行时代理实现。</p>
 *
 * <p>继承 {@link BaseMapper} 后即可获得 {@code insert}、{@code selectById}、
 * {@code selectOne}、{@code selectCount}、{@code updateById} 和 {@code deleteById}
 * 等常用操作。认证场景的条件查询在 Service 中通过 Lambda 条件构造器完成；未来复杂且可复用的
 * SQL 可以在此处声明。</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
