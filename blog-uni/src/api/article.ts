import { request } from './client'
import type {
  Article,
  ArticleStatus,
  CreateArticlePayload,
} from '../types/article'

/** 获取所有已发布文章。 */
export function getPublishedArticles() {
  return request<Article[]>('/api/articles')
}

/** 根据文章 ID 获取一篇已发布文章的详情。 */
export function getArticleDetail(articleId: number) {
  return request<Article>(`/api/articles/${articleId}`)
}

/** 新建文章；status 为 DRAFT 时保存草稿，为 PUBLISHED 时直接发布。 */
export function createArticle(payload: CreateArticlePayload) {
  return request<Article>('/api/articles', {
    method: 'POST',
    data: payload,
  })
}

/**
 * 修改自己的文章。发布草稿时，将原文章完整内容携带并把 status 改为 PUBLISHED。
 * 后端会根据 JWT 确保只能操作当前用户的文章。
 */
export function updateMyArticle(articleId: number, payload: CreateArticlePayload) {
  return request<Article>(`/api/articles/${articleId}`, {
    method: 'PUT',
    data: payload,
  })
}

/** 删除自己的文章。后端成功时返回 204 No Content。 */
export function deleteMyArticle(articleId: number) {
  return request<void>(`/api/articles/${articleId}`, {
    method: 'DELETE',
  })
}

/**
 * 查询当前登录用户自己的文章。
 * status 为空时返回全部文章，DRAFT 和 PUBLISHED 分别筛选草稿与已发布文章。
 */
export function getMyArticles(status?: ArticleStatus) {
  const query = status ? `?status=${status}` : ''
  return request<Article[]>(`/api/articles/mine${query}`)
}

/** 获取当前登录用户自己的文章详情，草稿也可以读取。 */
export function getMyArticleDetail(articleId: number) {
  return request<Article>(`/api/articles/mine/${articleId}`)
}
