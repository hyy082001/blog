/** 文章发布状态，与后端 ArticleStatus 枚举保持一致。 */
export type ArticleStatus = 'DRAFT' | 'PUBLISHED'

/** 后端文章接口返回的数据结构。 */
export interface Article {
  id: number
  title: string
  summary: string | null
  content: string
  coverUrl: string | null
  status: ArticleStatus
  authorId: number
  viewCount: number
  createdAt: string
  updatedAt: string
  publishedAt: string | null
}

/** 创建文章时提交给 POST /api/articles 的请求体。 */
export interface CreateArticlePayload {
  title: string
  summary: string | null
  content: string
  coverUrl: string | null
  status: ArticleStatus
}
