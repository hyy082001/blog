package com.yyh.blogserver.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yyh.blogserver.Model.Article;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link Article} 的数据访问接口。
 *
 * <p>继承 {@link BaseMapper} 后，MyBatis-Plus 会自动提供新增、按主键查询、
 * 条件查询、更新和删除等基础 CRUD 能力，因此不需要手写实现类。</p>
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
