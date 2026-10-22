package com.yyh.blogserver.Article.DTO;

import com.yyh.blogserver.Model.Article;
import com.yyh.blogserver.Model.ArticleStatus;

import java.time.OffsetDateTime;

/**
 * 返回给前端的文章资料。
 *
 * <p>独立 DTO 可以避免数据库实体字段直接成为接口契约，
 * 后续增加数据库内部字段时不会意外暴露给前端。</p>
 */
public record ArticleResponse(
        Long id,
        String title,
        String summary,
        String content,
        String coverUrl,
        ArticleStatus status,
        Long authorId,
        Long viewCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime publishedAt
) {

    /**
     * 将数据库实体转换为接口响应对象。
     *
     * @param article 从数据库查询到的文章实体
     * @return 适合返回给前端的文章资料
     */
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getContent(),
                article.getCoverUrl(),
                article.getStatus(),
                article.getAuthorId(),
                article.getViewCount(),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                article.getPublishedAt()
        );
    }
}
