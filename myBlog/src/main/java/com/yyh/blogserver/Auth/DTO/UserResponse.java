package com.yyh.blogserver.Auth.DTO;

import com.yyh.blogserver.Auth.enums.Role;

/**
 * 返回给前端的安全用户信息。
 *
 * <p>此对象故意不包含密码哈希。响应 DTO 将数据库实体与公开接口数据分离，避免未来在
 * {@code User} 中新增私密字段后，意外通过登录或个人信息接口泄露。</p>
 *
 * @param id 后续认证请求内部使用的数据库主键
 * @param username 登录使用的账号名，第一版不提供修改功能
 * @param nickname 博客页面公开展示的昵称
 * @param email 账号邮箱，用于联系和未来账号找回，不参与登录
 * @param role 当前授权角色
 */
public record UserResponse(
        Long id,
        String username,
        String nickname,
        String email,
        Role role
) {
}
