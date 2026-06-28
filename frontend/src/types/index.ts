export enum PostType {
  TEXT = 0,
  IMAGE = 1,
  VIDEO = 2
}

export interface User {
  id?: number
  userId?: number
  username: string
  avatar?: string
  bio?: string
  followingCount?: number
  followersCount?: number
  postsCount?: number
  createTime?: string
  isFollowing?: boolean
}

export interface Post {
  id: number
  userId: number
  username: string
  userAvatar?: string
  title: string
  content: string
  type: PostType | number
  imageUrls?: string[]
  videoUrl?: string
  videoWidth?: number
  videoHeight?: number
  likeCount: number | null
  commentCount: number | null
  isLiked?: boolean | null
  createTime?: string
  createdAt?: string
  mediaFiles?: MediaFile[]
}

export interface MediaFile {
  id: number
  url: string
  type: string
  sortOrder?: number
  width?: number
  height?: number
}

export interface VideoItem {
  url: string
  width?: number
  height?: number
}

export interface Comment {
  id: number
  postId: number
  userId: number
  username: string
  userAvatar?: string
  content: string
  createTime?: string
  createdAt?: string
}

export interface Notification {
  id: number
  type: string
  actorId: number
  actorUsername?: string
  actorAvatar?: string
  targetId: number
  targetType: string
  targetTitle?: string
  isRead: boolean
  createdAt?: string
}
