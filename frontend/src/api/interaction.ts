import request from '@/utils/axios'

// 点赞
export const likePost = (postId: number) => {
  return request.post(`/interactions/posts/${postId}/like`)
}

// 取消点赞
export const unlikePost = (postId: number) => {
  return request.delete(`/interactions/posts/${postId}/like`)
}

// 获取点赞状态
export const getLikeStatus = (postId: number) => {
  return request.get(`/interactions/posts/${postId}/like`)
}

// 评论
export const commentPost = (postId: number, content: string) => {
  return request.post(`/interactions/posts/${postId}/comments`, { content })
}

// 删除评论
export const deleteComment = (commentId: number) => {
  return request.delete(`/interactions/comments/${commentId}`)
}

// 获取评论列表
export const getComments = (postId: number, params?: { page?: number; size?: number }) => {
  return request.get(`/interactions/posts/${postId}/comments`, { params })
}
