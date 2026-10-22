package com.yyh.blogserver.Article.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yyh.blogserver.Article.DTO.ArticleResponse;
import com.yyh.blogserver.Article.DTO.CreateArticleRequest;
import com.yyh.blogserver.Article.DTO.UpdateArticleRequest;
import com.yyh.blogserver.Mapper.ArticleMapper;
import com.yyh.blogserver.Model.Article;
import com.yyh.blogserver.Model.ArticleStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 文章模块的业务逻辑。
 *
 * <p>这里负责文章状态、发布时间、作者归属等业务规则；
 * Controller 只负责接收 HTTP 请求和返回结果。</p>
 */
@Service
public class ArticleService {

    private final ArticleMapper articleMapper;

    public ArticleService(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    /**
     * 创建文章草稿或直接发布文章。
     *
     * @param authorId 从 JWT 中解析出的当前登录用户 ID
     * @param request 前端提交的文章资料
     * @return 新建后的文章
     */
    public ArticleResponse create(Long authorId, CreateArticleRequest request) {
        OffsetDateTime now = nowUtc();

        Article article = new Article();
        article.setTitle(request.title().trim());
        article.setSummary(trimToNull(request.summary()));
        article.setContent(request.content());
        article.setCoverUrl(trimToNull(request.coverUrl()));
        article.setStatus(request.status());
        article.setAuthorId(authorId);
        article.setViewCount(0L);
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        article.setPublishedAt(
                request.status() == ArticleStatus.PUBLISHED ? now : null
        );

        articleMapper.insert(article);

        return ArticleResponse.from(article);
    }

    /**
     * 查询全部已发布文章。
     *
     * <p>第一版暂不做分页，后续文章数量增长后再改为 MyBatis-Plus 的 Page 查询。</p>
     */
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

    /**
     * 查询当前登录用户创建的文章。
     *
     * <p>用户 ID 不由前端传入，而是来自 JWT。这样前端即使篡改请求参数，
     * 也不能借此查看其他用户的草稿。status 为 {@code null} 时返回该用户的全部文章；
     * 传入 {@code DRAFT} 或 {@code PUBLISHED} 时则按状态筛选。</p>
     *
     * @param currentUserId 当前登录用户 ID
     * @param status 可选的文章状态筛选条件
     * @return 当前用户的文章，按最近修改时间倒序排列
     */
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

    /**
     * 查询一篇已经发布的文章。
     *
     * <p>草稿不会通过该接口返回，避免普通读者访问未发布内容。</p>
     *
     * @param articleId 文章 ID
     * @return 已发布文章详情
     */
    public ArticleResponse publishedArticleDetail(Long articleId) {
        Article article = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getId, articleId)
                        .eq(Article::getStatus, ArticleStatus.PUBLISHED)
        );

        if (article == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "文章不存在或尚未发布"
            );
        }

        return ArticleResponse.from(article);
    }

    /**
     * 查询当前用户自己的一篇文章。
     *
     * <p>与公开详情接口不同，这个方法允许作者读取自己的草稿，
     * 但会先校验文章归属。</p>
     *
     * @param currentUserId 当前登录用户 ID
     * @param articleId 文章 ID
     * @return 当前用户拥有的文章详情
     */
    public ArticleResponse myArticleDetail(
            Long currentUserId,
            Long articleId
    ) {
        Article article = findArticle(articleId);
        verifyOwner(currentUserId, article);

        return ArticleResponse.from(article);
    }

    /**
     * 修改自己创建的文章。
     *
     * <p>草稿第一次发布时写入发布时间；已发布文章再次编辑时，
     * 保留首次发布时间。若重新保存为草稿，则清空发布时间以符合数据库约束。</p>
     */
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

    /**
     * 删除自己创建的文章。
     *
     * @param currentUserId 当前登录用户 ID
     * @param articleId 要删除的文章 ID
     */
    public void delete(Long currentUserId, Long articleId) {
        Article article = findArticle(articleId);
        verifyOwner(currentUserId, article);
        articleMapper.deleteById(articleId);
    }

    /** 按主键查询文章，不存在时统一返回 HTTP 404。 */
    private Article findArticle(Long articleId) {
        Article article = articleMapper.selectById(articleId);

        if (article == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }

        return article;
    }

    /** 确保当前用户只能修改、删除自己写的文章。 */
    private void verifyOwner(Long currentUserId, Article article) {
        if (!article.getAuthorId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "无权操作其他用户的文章"
            );
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
