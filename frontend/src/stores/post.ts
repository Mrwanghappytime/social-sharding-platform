import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getFeed, getPostById, createPost, deletePost, searchPosts, getUserPosts } from '@/api/post'
import { likePost, unlikePost } from '@/api/interaction'
import type { Post } from '@/types'

export const usePostStore = defineStore('post', () => {
  const posts = ref<Post[]>([])
  const currentPost = ref<Post | null>(null)
  const loading = ref(false)
  const hasMore = ref(true)
  const page = ref(1)

  const fetchFeed = async (reset = false) => {
    if (loading.value || (!hasMore.value && !reset)) return

    loading.value = true
    try {
      if (reset) {
        page.value = 1
        posts.value = []
        hasMore.value = true
      }

      const res = await getFeed({ page: page.value, size: 10 })
      const newPosts = res.data?.records || res.data?.list || res.data || []

      if (reset) {
        posts.value = newPosts
      } else {
        posts.value.push(...newPosts)
      }

      hasMore.value = newPosts.length === 10
      page.value++
    } catch (error) {
      console.error('Failed to fetch feed:', error)
    } finally {
      loading.value = false
    }
  }

  const fetchPostById = async (postId: number) => {
    loading.value = true
    try {
      const res = await getPostById(postId)
      currentPost.value = res.data || res
    } catch (error) {
      console.error('Failed to fetch post:', error)
    } finally {
      loading.value = false
    }
  }

  const create = async (data: { title: string; content: string; type: any; imageUrls?: string[]; videoUrl?: string }) => {
    const res = await createPost(data)
    return res.data || res
  }

  const remove = async (postId: number) => {
    await deletePost(postId)
    posts.value = posts.value.filter(p => p.id !== postId)
    if (currentPost.value?.id === postId) {
      currentPost.value = null
    }
  }

  const search = async (keyword: string, reset = false) => {
    if (loading.value) return

    loading.value = true
    try {
      if (reset) {
        page.value = 1
        posts.value = []
        hasMore.value = true
      }

      const res = await searchPosts({ keyword, page: page.value, size: 10 })
      const newPosts = res.data?.records || res.data?.list || res.data || []

      if (reset) {
        posts.value = newPosts
      } else {
        posts.value.push(...newPosts)
      }

      hasMore.value = newPosts.length === 10
      page.value++
    } catch (error) {
      console.error('Failed to search posts:', error)
    } finally {
      loading.value = false
    }
  }

  const fetchUserPosts = async (userId: number, reset = false) => {
    if (loading.value) return

    loading.value = true
    try {
      if (reset) {
        page.value = 1
        posts.value = []
        hasMore.value = true
      }

      const res = await getUserPosts(userId, { page: page.value, size: 10 })
      const newPosts = res.data?.records || res.data?.list || res.data || []

      if (reset) {
        posts.value = newPosts
      } else {
        posts.value.push(...newPosts)
      }

      hasMore.value = newPosts.length === 10
      page.value++
    } catch (error) {
      console.error('Failed to fetch user posts:', error)
    } finally {
      loading.value = false
    }
  }

  const toggleLike = async (postId: number) => {
    const post = currentPost.value
    if (!post || post.id !== postId) {
      console.log('toggleLike early return:', { post, postId })
      return
    }

    const wasLiked = post.isLiked === true
    const originalCount = post.likeCount || 0
    console.log('toggleLike before:', { wasLiked, originalCount, isLiked: post.isLiked })

    // Optimistic update - replace with new object to trigger reactivity
    const updatedPost = { ...post }
    updatedPost.isLiked = !wasLiked
    updatedPost.likeCount = wasLiked
      ? Math.max(0, originalCount - 1)
      : originalCount + 1
    currentPost.value = updatedPost
    console.log('toggleLike after:', { isLiked: updatedPost.isLiked, likeCount: updatedPost.likeCount })

    try {
      if (wasLiked) {
        await unlikePost(postId)
      } else {
        await likePost(postId)
      }
    } catch (error) {
      // Revert on failure
      currentPost.value = { ...post }
      throw error
    }
  }

  return {
    posts,
    currentPost,
    loading,
    hasMore,
    fetchFeed,
    fetchPostById,
    create,
    remove,
    search,
    fetchUserPosts,
    toggleLike
  }
})
