const ACCESS_TOKEN_KEY = 'blog_access_token'

/** 保存登录成功后获得的 JWT。 */
export function saveToken(token: string) {
  uni.setStorageSync(ACCESS_TOKEN_KEY, token)
}

/** 读取当前设备中保存的 JWT。 */
export function getToken(): string {
  return uni.getStorageSync<string>(ACCESS_TOKEN_KEY) || ''
}

/** 退出登录或 Token 失效时清除 JWT。 */
export function removeToken() {
  uni.removeStorageSync(ACCESS_TOKEN_KEY)
}
