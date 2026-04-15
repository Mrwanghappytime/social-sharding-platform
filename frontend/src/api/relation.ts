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
export const getFollowing = (userId: number, page = 1, size = 20) => {
  return request.get(`/relations/following/${userId}`, { params: { page, size } })
}

// 获取粉丝列表
export const getFollowers = (userId: number, page = 1, size = 20) => {
  return request.get(`/relations/followers/${userId}`, { params: { page, size } })
}

// 检查是否关注某个用户
export const checkIsFollowing = (userId: number) => {
  return request.get(`/relations/is-following/${userId}`)
}

// 获取用户关注/粉丝数量
export const getRelationCounts = (userId: number) => {
  return request.get(`/relations/counts/${userId}`)
}
