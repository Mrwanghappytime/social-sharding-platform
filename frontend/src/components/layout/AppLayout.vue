<template>
  <div class="main-content">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
.main-content {
  margin-left: 200px;
  margin-top: 60px;
  min-height: calc(100vh - 60px);
  background: #fafafa;
}
</style>
