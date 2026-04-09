<template>
  <div class="explore-page">
    <AppHeader />
    <AppSidebar />
    <AppLayout>
      <div class="explore-container">
        <h2>发现</h2>
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
.explore-page {
  min-height: 100vh;
  background: #fafafa;
}

.explore-container {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px;

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #333;
    margin-bottom: 20px;
  }
}
</style>
