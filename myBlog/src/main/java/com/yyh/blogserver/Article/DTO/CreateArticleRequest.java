package com.yyh.blogserver.Article.DTO;

import com.yyh.blogserver.Model.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 前端创建文章时提交的请求体。
 *
 * <p>第一版由前端明确提交状态：{@code DRAFT} 保存草稿，
 * {@code PUBLISHED} 表示直接发布。</p>
 */
public record CreateArticleRequest(

        /** 标题不能为空，最长 200 个字符。 */
        @NotBlank(message = "文章标题不能为空")
        @Size(max = 200, message = "文章标题不能超过 200 个字符")
        String title,

        /** 摘要可以为空，最长 500 个字符。 */
        @Size(max = 500, message = "文章摘要不能超过 500 个字符")
        String summary,

        /** 正文不能为空，第一版保存 Markdown 文本。 */
        @NotBlank(message = "文章正文不能为空")
        String content,

        /** 封面图片地址可以为空。 */
        @Size(max = 500, message = "封面地址不能超过 500 个字符")
        String coverUrl,

        /** 文章必须明确指定为草稿或已发布。 */
        @NotNull(message = "文章状态不能为空")
        ArticleStatus status
) {
}
