package com.yyh.blogserver.Model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 映射 PostgreSQL {@code users} 表的领域对象。
 *
 * <p>这是 MyBatis-Plus 实体，不是 JPA 实体。MyBatis-Plus 根据下方的表和字段注解，
 * 通过 {@code UserMapper} 生成基础 CRUD SQL。</p>
 */
@Data
@TableName("users")
public class User {

    /**
     * 由 PostgreSQL {@code BIGSERIAL} 列生成的主键。
     *
     * <p>{@code IdType.AUTO} 表示 INSERT 后由 MyBatis-Plus 获取数据库生成的 ID，
     * 再回填到当前对象。</p>
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录账号，数据库中唯一；注册后第一版不提供修改入口。 */
    private String username;

    /** 博客页面展示的昵称，与登录账号分开保存。 */
    private String nickname;

    /** 注册前统一转为小写的唯一邮箱，用于联系和未来找回账号，不参与登录。 */
    private String email;

    /**
     * 保存于 {@code password_hash} 数据库列的 BCrypt 密码哈希。
     *
     * <p>该字段绝不能保存明文密码，也绝不能出现在接口响应中。显式映射是因为 Java 使用
     * 驼峰命名 {@code passwordHash}，数据库使用下划线命名 {@code password_hash}。</p>
     */
    @TableField("password_hash")
    private String passwordHash;

    /** Spring Security 用于保护资源的授权角色。 */
    private Role role;
}
