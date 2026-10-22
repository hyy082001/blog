import { request } from './client'
import type { User } from '../types/auth'

/** 修改当前用户基础资料时提交的字段。 */
export interface UpdateProfileRequest {
  nickname: string
  email: string
}

/**
 * 更新当前登录用户的昵称和邮箱。
 *
 * <p>client.ts 会自动添加 JWT，因此前端不传 userId。</p>
 */
export function updateMyProfile(data: UpdateProfileRequest) {
  return request<User>('/api/users/me', {
    method: 'PUT',
    data,
  })
}

/** 修改密码时提交给 PUT /api/users/me/password 的数据。 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

/**
 * 修改当前登录用户的密码。
 *
 * <p>后端成功时返回 HTTP 204，因此泛型使用 void。</p>
 */
export function changeMyPassword(data: ChangePasswordRequest) {
  return request<void>('/api/users/me/password', {
    method: 'PUT',
    data,
  })
}
