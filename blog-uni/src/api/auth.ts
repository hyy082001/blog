import { request } from './client'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  User,
} from '../types/auth'

export function register(data: RegisterRequest) {
  return request<User>('/api/auth/register', {
    method: 'POST',
    data,
  })
}

export function login(data: LoginRequest) {
  return request<AuthResponse>('/api/auth/login', {
    method: 'POST',
    data,
  })
}

export function getCurrentUser() {
  return request<User>('/api/auth/me')
}
