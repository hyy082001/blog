package com.yyh.blogserver.Article.Service;

import com.yyh.blogserver.Article.DTO.ArticleResponse;
import com.yyh.blogserver.Article.DTO.CreateArticleRequest;
import com.yyh.blogserver.Article.DTO.UpdateArticleRequest;
import com.yyh.blogserver.Article.enums.ArticleStatus;

import java.util.List;

/**
 * 文章模块的业务接口。
 *
 * <p>负责文章状态、发布时间、作者归属等业务规则；Controller 只依赖本接口，
 * 具体实现由 {@link com.yyh.blogserver.Article.Service.impl.ArticleServiceImpl}
 * 提供。</p>
 */
public interface ArticleService {

    /**
     * 创建文章草稿或直接发布文章。
     *
     * @param authorId 从 JWT 中解析出的当前登录用户 ID
     * @param request 前端提交的文章资料
     * @return 新建后的文章
     */
    ArticleResponse create(Long authorId, CreateArticleRequest request);

    /**
     * 查询全部已发布文章。
     *
     * @return 已发布文章列表，按发布时间倒序排列
     */
    List<ArticleResponse> publishedArticles();

    /**
     * 查询当前登录用户创建的文章。
     *
     * <p>用户 ID 不由前端传入，而是来自 JWT。这样前端即使篡改请求参数，
     * 也不能借此查看其他用户的草稿。</p>
     *
     * @param currentUserId 当前登录用户 ID
     * @param status 可选的文章状态筛选条件；为 {@code null} 时查询全部
     * @return 当前用户的文章，按最近修改时间倒序排列
     */
    List<ArticleResponse> myArticles(Long currentUserId, ArticleStatus status);

    /**
     * 查询一篇已经发布的文章。
     *
     * @param articleId 文章 ID
     * @return 已发布文章详情
     */
    ArticleResponse publishedArticleDetail(Long articleId);

    /**
     * 查询当前用户自己的一篇文章，允许作者读取自己的草稿。
     *
     * @param currentUserId 当前登录用户 ID
     * @param articleId 文章 ID
     * @return 当前用户拥有的文章详情
     */
    ArticleResponse myArticleDetail(Long currentUserId, Long articleId);

    /**
     * 修改自己创建的文章。
     *
     * @param currentUserId 当前登录用户 ID
     * @param articleId 文章 ID
     * @param request 前端提交的文章资料
     * @return 更新后的文章
     */
    ArticleResponse update(
            Long currentUserId,
            Long articleId,
            UpdateArticleRequest request
    );

    /**
     * 删除自己创建的文章。
     *
     * @param currentUserId 当前登录用户 ID
     * @param articleId 要删除的文章 ID
     */
    void delete(Long currentUserId, Long articleId);
}
