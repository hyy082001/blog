package com.yyh.blogserver.Article.controller;

import com.yyh.blogserver.Article.DTO.ArticleResponse;
import com.yyh.blogserver.Article.DTO.CreateArticleRequest;
import com.yyh.blogserver.Article.DTO.UpdateArticleRequest;
import com.yyh.blogserver.Article.Service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.yyh.blogserver.Model.ArticleStatus;

import java.util.List;

/**
 * 文章 REST 接口。
 *
 * <p>当前 SecurityConfig 中的 {@code anyRequest().authenticated()} 会要求所有文章接口
 * 携带 JWT。{@code Authentication} 的 name 是 JwtAuthenticationFilter 写入的用户 ID。</p>
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /** 创建文章。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateArticleRequest request
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());

        return articleService.create(currentUserId, request);
    }

    /** 获取全部已发布文章；第一版暂不分页。 */
    @GetMapping
    public List<ArticleResponse> publishedArticles() {
        return articleService.publishedArticles();
    }

    /**
     * 查询当前登录用户自己的文章。
     *
     * <p>示例：不传 status 查询全部；
     * {@code GET /api/articles/mine?status=DRAFT} 查询草稿；
     * {@code GET /api/articles/mine?status=PUBLISHED} 查询已发布文章。</p>
     */
    @GetMapping("/mine")
    public List<ArticleResponse> myArticles(
            Authentication authentication,
            @RequestParam(required = false) ArticleStatus status
    ) {
        // JwtAuthenticationFilter 已将用户 ID 写入 Authentication。
        Long currentUserId = Long.parseLong(authentication.getName());

        return articleService.myArticles(currentUserId, status);
    }

    /**
     * 查询当前用户自己的一篇文章详情，包括未发布的草稿。
     */
    @GetMapping("/mine/{articleId}")
    public ArticleResponse myArticleDetail(
            Authentication authentication,
            @PathVariable Long articleId
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());

        return articleService.myArticleDetail(currentUserId, articleId);
    }

    /** 获取单篇已发布文章详情。 */
    @GetMapping("/{articleId}")
    public ArticleResponse publishedArticleDetail(
            @PathVariable Long articleId
    ) {
        return articleService.publishedArticleDetail(articleId);
    }

    /** 修改自己的文章。 */
    @PutMapping("/{articleId}")
    public ArticleResponse update(
            Authentication authentication,
            @PathVariable Long articleId,
            @Valid @RequestBody UpdateArticleRequest request
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());

        return articleService.update(currentUserId, articleId, request);
    }

    /** 删除自己的文章，成功时返回 HTTP 204。 */
    @DeleteMapping("/{articleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @PathVariable Long articleId
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        articleService.delete(currentUserId, articleId);
    }
}
