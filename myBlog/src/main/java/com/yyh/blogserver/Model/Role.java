package com.yyh.blogserver.Model;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 博客系统支持的用户角色。
 *
 * <p>角色决定登录用户可执行的操作。公开注册只能创建 {@link #USER}；
 * {@link #ADMIN} 由系统所有者手动指定，后续才有文章管理权限。</p>
 */
public enum Role {

    /** 普通注册用户。 */
    USER("USER"),

    /** 可以管理受保护资源的博客管理员。 */
    ADMIN("ADMIN");

    /**
     * 保存到 PostgreSQL {@code users.role} 列中的实际字符串。
     *
     * <p>显式定义数据库值后，即使未来重构 Java 枚举名，也不会无意改变数据库中的角色表示。</p>
     */
    @EnumValue
    private final String value;

    Role(String value) {
        this.value = value;
    }

    /**
     * 获取当前角色对应的数据库值。
     *
     * @return {@code USER} 或 {@code ADMIN}
     */
    public String getValue() {
        return value;
    }
}
