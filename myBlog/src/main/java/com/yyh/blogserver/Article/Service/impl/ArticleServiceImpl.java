package com.yyh.blogserver.Article.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yyh.blogserver.Article.DTO.ArticleResponse;
import com.yyh.blogserver.Article.DTO.CreateArticleRequest;
import com.yyh.blogserver.Article.DTO.UpdateArticleRequest;
import com.yyh.blogserver.Article.Service.ArticleService;
import com.yyh.blogserver.Article.enums.ArticleStatus;
import com.yyh.blogserver.Common.enums.ErrorCode;
import com.yyh.blogserver.Common.exception.BusinessException;
import com.yyh.blogserver.Mapper.ArticleMapper;
import com.yyh.blogserver.Model.Article;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * {@link ArticleService} 的默认实现。
 *
 * <p>文章状态、发布时间和作者归属等业务规则都在本类中处理。
 * 第一版暂不做分页，后续文章数量增长后再改为 MyBatis-Plus 的 Page 查询。</p>
 */
@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;

    public ArticleServiceImpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    public ArticleResponse create(Long authorId, CreateArticleRequest request) {
        OffsetDateTime now = nowUtc();

        Article article = new Article(
                request.title().trim(),
                trimToNull(request.summary()),
                request.content(),
                trimToNull(request.coverUrl()),
                request.status(),
                authorId,
                0L,
                now,
                now,
                request.status() == ArticleStatus.PUBLISHED ? now : null
        );

        articleMapper.insert(article);

        return ArticleResponse.from(article);
    }

    @Override
    public List<ArticleResponse> publishedArticles() {
        return articleMapper.selectList(
                        new LambdaQueryWrapper<Article>()
                                .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                                .orderByDesc(Article::getPublishedAt)
                )
                .stream()
                .map(ArticleResponse::from)
                .toList();
    }

    @Override
    public List<ArticleResponse> myArticles(
            Long currentUserId,
            ArticleStatus status
    ) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<Article>()
                // 只读取当前 JWT 对应用户的文章。
                .eq(Article::getAuthorId, currentUserId)
                // 草稿和已发布文章都以最近修改时间排序。
                .orderByDesc(Article::getUpdatedAt);

        // 不传 status 时查询全部；传入状态时再附加筛选条件。
        if (status != null) {
            queryWrapper.eq(Article::getStatus, status);
        }

        return articleMapper.selectList(queryWrapper)
                .stream()
                .map(ArticleResponse::from)
                .toList();
    }

    @Override
    public ArticleResponse publishedArticleDetail(Long articleId) {
        Article article = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getId, articleId)
                        .eq(Article::getStatus, ArticleStatus.PUBLISHED)
        );

        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_PUBLISHED);
        }

        return ArticleResponse.from(article);
    }

    @Override
    public ArticleResponse myArticleDetail(
            Long currentUserId,
            Long articleId
    ) {
        Article article = findArticle(articleId);
        verifyOwner(currentUserId, article);

        return ArticleResponse.from(article);
    }

    @Override
    public ArticleResponse update(
            Long currentUserId,
            Long articleId,
            UpdateArticleRequest request
    ) {
        Article article = findArticle(articleId);
        verifyOwner(currentUserId, article);

        article.setTitle(request.title().trim());
        article.setSummary(trimToNull(request.summary()));
        article.setContent(request.content());
        article.setCoverUrl(trimToNull(request.coverUrl()));
        article.setStatus(request.status());
        article.setUpdatedAt(nowUtc());

        if (request.status() == ArticleStatus.PUBLISHED
                && article.getPublishedAt() == null) {
            article.setPublishedAt(nowUtc());
        }

        if (request.status() == ArticleStatus.DRAFT) {
            article.setPublishedAt(null);
        }

        articleMapper.updateById(article);

        return ArticleResponse.from(article);
    }

    @Override
    public void delete(Long currentUserId, Long articleId) {
        Article article = findArticle(articleId);
        verifyOwner(currentUserId, article);
        articleMapper.deleteById(articleId);
    }

    /** 按主键查询文章，不存在时统一返回 HTTP 404。 */
    private Article findArticle(Long articleId) {
        Article article = articleMapper.selectById(articleId);

        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }

        return article;
    }

    /** 确保当前用户只能修改、删除自己写的文章。 */
    private void verifyOwner(Long currentUserId, Article article) {
        if (!article.getAuthorId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ARTICLE_FORBIDDEN);
        }
    }

    /** 可选字段若只传入空白字符，统一保存为 null。 */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /** 统一使用 UTC 保存带时区时间。 */
    private OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
