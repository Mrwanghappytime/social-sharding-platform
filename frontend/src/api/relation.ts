import request from '@/utils/axios'

// 关注用户
export const followUser = (userId: number) => {
  return request.post(`/relations/follow/${userId}`)
}

// 取消关注
export const unfollowUser = (userId: number) => {
  return request.delete(`/relations/follow/${userId}`)
}

// 获取关注列表
export const getFollowing = (userId: number) => {
  return request.get('/relations/following', { params: { userId } })
}

// 获取粉丝列表
export const getFollowers = (userId: number) => {
  return request.get('/relations/followers', { params: { userId } })
}
