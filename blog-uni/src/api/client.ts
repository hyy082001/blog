import { getToken } from '../auth/token'
import { API_BASE_URL } from '../config/api'

/** 后端统一错误响应中的可选字段。 */
interface ApiErrorBody {
  status?: number
  message?: string
  detail?: string
  path?: string
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: object
}

/**
 * 前端请求失败时抛出的错误。
 *
 * <p>除错误文案外，保留 HTTP 状态码，后续遇到 401 时可按需统一跳转登录页。</p>
 */
export class ApiError extends Error {
  readonly statusCode: number

  constructor(statusCode: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.statusCode = statusCode
  }
}

/**
 * 从后端 JSON 响应中提取可展示的错误信息。
 *
 * <p>兼容统一错误格式、Spring Boot 默认格式和旧接口可能返回的纯文本，
 * 即使响应体为空也会提供有意义的兜底提示。</p>
 */
function getErrorMessage(body: unknown, statusCode: number): string {
  if (typeof body === 'string' && body.trim()) {
    return body
  }

  if (body && typeof body === 'object') {
    const errorBody = body as ApiErrorBody
    if (errorBody.message) {
      return errorBody.message
    }
    if (errorBody.detail) {
      return errorBody.detail
    }
  }

  if (statusCode === 401) {
    return '未登录或登录状态已失效，请重新登录'
  }
  if (statusCode === 403) {
    return '无权访问此功能'
  }

  return `请求失败，状态码：${statusCode}`
}

/**
 * 封装 uni.request：统一设置后端地址、JSON 请求头和 JWT 请求头，
 * 并把非 2xx 的 JSON 错误响应转换成可直接展示的 ApiError。
 */
export function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const token = getToken()

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${path}`,
      method: options.method ?? 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data as T)
          return
        }

        reject(new ApiError(
          response.statusCode,
          getErrorMessage(response.data, response.statusCode),
        ))
      },
      fail(error) {
        reject(new ApiError(
          0,
          error.errMsg || '网络连接失败，请检查后端是否已启动',
        ))
      },
    })
  })
}
