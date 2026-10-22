package com.yyh.blogserver.Model;

/**
 * 文章发布状态。
 *
 * <p>枚举名称会直接保存到 PostgreSQL 的 {@code articles.status} 字段中，
 * 因此必须与数据库约束中的 {@code DRAFT}、{@code PUBLISHED} 保持一致。</p>
 */
public enum ArticleStatus {

    /** 草稿：作者可以继续编辑，暂时不出现在文章列表中。 */
    DRAFT,

    /** 已发布：可以出现在首页和文章列表中。 */
    PUBLISHED
}
