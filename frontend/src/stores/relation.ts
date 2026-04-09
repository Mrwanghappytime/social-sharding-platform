import { defineStore } from 'pinia'
import { ref } from 'vue'
import { followUser, unfollowUser, getFollowers, getFollowing } from '@/api/relation'

export const useRelationStore = defineStore('relation', () => {
  const following = ref<number[]>([])
  const followers = ref<number[]>([])
  const loading = ref(false)

  const isFollowing = (userId: number) => following.value.includes(userId)

  const follow = async (userId: number) => {
    try {
      await followUser(userId)
      if (!following.value.includes(userId)) {
        following.value.push(userId)
      }
      return true
    } catch (error) {
      console.error('Failed to follow:', error)
      return false
    }
  }

  const unfollow = async (userId: number) => {
    try {
      await unfollowUser(userId)
      following.value = following.value.filter(id => id !== userId)
      return true
    } catch (error) {
      console.error('Failed to unfollow:', error)
      return false
    }
  }

  const fetchFollowing = async (userId: number) => {
    loading.value = true
    try {
      const res = await getFollowing(userId)
      following.value = res.data || []
    } catch (error) {
      console.error('Failed to fetch following:', error)
    } finally {
      loading.value = false
    }
  }

  const fetchFollowers = async (userId: number) => {
    loading.value = true
    try {
      const res = await getFollowers(userId)
      followers.value = res.data || []
    } catch (error) {
      console.error('Failed to fetch followers:', error)
    } finally {
      loading.value = false
    }
  }

  const setFollowingList = (list: number[]) => {
    following.value = list
  }

  return {
    following,
    followers,
    loading,
    isFollowing,
    follow,
    unfollow,
    fetchFollowing,
    fetchFollowers,
    setFollowingList
  }
})
