<template>
  <div class="home-page">
    <AppHeader />
    <AppSidebar />
    <AppLayout>
      <div class="feed-container">
        <PostList />
      </div>
    </AppLayout>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import PostList from '@/components/post/PostList.vue'
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  background: #fafafa;
}

.feed-container {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px;
}
</style>
