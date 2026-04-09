<template>
  <div class="notification-page">
    <AppHeader />
    <AppLayout>
      <div class="notification-container">
        <div class="notification-header">
          <h2>通知</h2>
          <el-button type="text" @click="markAllRead" v-if="unreadCount > 0">
            全部已读
          </el-button>
        </div>

        <div class="notification-list" v-loading="loading">
          <NotificationItem
            v-for="item in notifications"
            :key="item.id"
            :notification="item"
            @click="handleClick(item)"
          />

          <div v-if="notifications.length === 0 && !loading" class="empty">
            <span class="empty-icon">🔔</span>
            <span>暂无通知</span>
          </div>
        </div>

        <div class="pagination" v-if="total > size">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="size"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </AppLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useNotificationStore } from '@/stores/notification'
import { useAuthStore } from '@/stores/auth'
import { getNotificationList, markAllAsRead as apiMarkAllAsRead } from '@/api/notification'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import NotificationItem from '@/components/notification/NotificationItem.vue'

const router = useRouter()
const notificationStore = useNotificationStore()
const authStore = useAuthStore()

const notifications = computed(() => notificationStore.notifications)
const unreadCount = computed(() => notificationStore.unreadCount)

const loading = ref(false)
const currentPage = ref(1)
const size = ref(10)
const total = ref(0)

const fetchNotifications = async (page: number = 1) => {
  const userId = authStore.userInfo?.id
  if (!userId) return

  loading.value = true
  try {
    const res = await getNotificationList(userId, page, size.value)
    if (res.data && res.data.records) {
      // API返回已按时间倒序，直接使用push保持顺序
      const newNotifications = res.data.records.map((n: any) => ({
        id: n.id,
        type: n.type,
        actorId: n.actorId,
        actorUsername: n.actorUsername,
        actorAvatar: n.actorAvatar,
        targetId: n.targetId,
        targetType: n.targetType,
        isRead: n.isRead,
        createdAt: n.createdAt
      }))
      notificationStore.clearNotifications()
      newNotifications.forEach(n => notificationStore.pushNotification(n))
      total.value = res.data.total
    }
  } catch (error) {
    console.error('Failed to fetch notifications:', error)
    ElMessage.error('获取通知失败')
  } finally {
    loading.value = false
  }
}

const markAllRead = async () => {
  const userId = authStore.userInfo?.id
  if (!userId) return
  try {
    await apiMarkAllAsRead(userId)
    notificationStore.markAllAsRead()
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    ElMessage.error('标记失败')
  }
}

const handleClick = (item: any) => {
  const userId = authStore.userInfo?.id
  if (userId) {
    notificationStore.markAsRead(item.id, userId)
  }
  if (item.targetType === 'POST' && item.targetId) {
    router.push(`/post/${item.targetId}`)
  }
}

const handlePageChange = (page: number) => {
  fetchNotifications(page)
}

onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped lang="scss">
.notification-page {
  min-height: 100vh;
  background: #fafafa;
}

.notification-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #333;
  }
}

.notification-list {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  min-height: 200px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.empty {
  text-align: center;
  padding: 48px;
  color: #999;

  .empty-icon {
    font-size: 48px;
    display: block;
    margin-bottom: 16px;
  }
}
</style>
