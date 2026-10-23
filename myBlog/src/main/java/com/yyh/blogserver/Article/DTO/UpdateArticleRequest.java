package com.yyh.blogserver.Article.DTO;

import com.yyh.blogserver.Article.enums.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 前端完整修改文章时提交的请求体。
 *
 * <p>该对象对应 HTTP {@code PUT}，因此要求提交完整文章内容。
 * 如果以后需要只更新单个字段，再单独增加 {@code PATCH} 接口。</p>
 */
public record UpdateArticleRequest(

        @NotBlank(message = "文章标题不能为空")
        @Size(max = 200, message = "文章标题不能超过 200 个字符")
        String title,

        @Size(max = 500, message = "文章摘要不能超过 500 个字符")
        String summary,

        @NotBlank(message = "文章正文不能为空")
        String content,

        @Size(max = 500, message = "封面地址不能超过 500 个字符")
        String coverUrl,

        @NotNull(message = "文章状态不能为空")
        ArticleStatus status
) {
}
