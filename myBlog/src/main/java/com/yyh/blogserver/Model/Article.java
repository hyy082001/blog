package com.yyh.blogserver.Model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 映射 PostgreSQL {@code articles} 表的 MyBatis-Plus 实体。
 *
 * <p>实体负责对应数据库字段；接口返回时不直接使用这个类，
 * 而是转换为 {@code ArticleResponse}，避免数据库结构变化直接影响前端接口。</p>
 */
@Data
@TableName("articles")
public class Article {

    /** PostgreSQL {@code BIGSERIAL} 自动生成的文章主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 文章标题。 */
    private String title;

    /** 在文章列表中展示的简短摘要。 */
    private String summary;

    /** 文章正文，第一版直接保存 Markdown 文本。 */
    private String content;

    /** 文章封面图片地址，可以为空。 */
    @TableField("cover_url")
    private String coverUrl;

    /** 草稿或已发布状态。 */
    private ArticleStatus status;

    /** 当前文章所属用户的 ID，对应 {@code users.id}。 */
    @TableField("author_id")
    private Long authorId;

    /** 文章浏览量，第一版创建文章时初始化为 0。 */
    @TableField("view_count")
    private Long viewCount;

    /** 创建时间，映射 PostgreSQL 的 {@code TIMESTAMPTZ}。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /** 最近一次修改时间。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    /** 首次发布时间；草稿时必须为空。 */
    @TableField("published_at")
    private OffsetDateTime publishedAt;
}
