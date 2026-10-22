export type Role = 'USER' | 'ADMIN'

export interface User {
  id: number
  username: string
  nickname: string
  email: string
  role: Role
}

export interface AuthResponse {
  accessToken: string
  user: User
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}
