import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/HomePage.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginPage.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterPage.vue'),
    meta: { guest: true }
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: () => import('@/views/post/PostDetailPage.vue')
  },
  {
    path: '/user/:id',
    name: 'UserProfile',
    component: () => import('@/views/user/UserProfilePage.vue')
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/user/SettingsPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user/:id/following',
    name: 'Following',
    component: () => import('@/views/relation/FollowingPage.vue')
  },
  {
    path: '/user/:id/followers',
    name: 'Followers',
    component: () => import('@/views/relation/FollowersPage.vue')
  },
  {
    path: '/create',
    name: 'CreatePost',
    component: () => import('@/views/post/CreatePostPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/search/SearchPage.vue')
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/notification/NotificationPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/explore',
    name: 'Explore',
    component: () => import('@/views/explore/ExplorePage.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn()) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.guest && authStore.isLoggedIn()) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
