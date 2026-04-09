import request from '@/utils/axios'
import { PostType } from '@/types'

// 获取首页动态流
export const getFeed = (params: { page?: number; size?: number }) => {
  return request.get('/posts/feed', { params })
}

// 获取用户动态列表
export const getUserPosts = (userId: number, params?: { page?: number; size?: number }) => {
  return request.get(`/posts/user/${userId}`, { params })
}

// 获取动态详情
export const getPostById = (postId: number) => {
  return request.get(`/posts/${postId}`)
}

// 发布动态
export const createPost = (data: {
  title: string
  content: string
  type: PostType
  imageUrls?: string[]
  videoUrl?: string
}) => {
  return request.post('/posts', data)
}

// 删除动态
export const deletePost = (postId: number) => {
  return request.delete(`/posts/${postId}`)
}

// 搜索动态
export const searchPosts = (params: { keyword?: string; page?: number; size?: number }) => {
  return request.get('/posts/search', { params })
}
