<template>
  <div class="media-gallery" :class="[`count-${images?.length || videos?.length}`]">
    <template v-if="images?.length">
      <div
        v-for="(img, index) in displayImages"
        :key="index"
        class="media-item image-item"
        @click="preview(index)"
      >
        <img :src="img" alt="" />
        <div v-if="index === 8 && images.length > 9" class="more-overlay">
          +{{ images.length - 9 }}
        </div>
      </div>
    </template>

    <template v-if="videos?.length">
      <div
        v-for="(video, index) in videos"
        :key="index"
        class="media-item video-item"
        @click="playVideo(video)"
      >
        <video :src="video"></video>
        <div class="play-icon">▶</div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  images?: string[]
  videos?: string[]
}>()

const displayImages = computed(() => {
  if (!props.images) return []
  return props.images.slice(0, 9)
})

const preview = (index: number) => {
  // TODO: implement image preview
  console.log('Preview image:', index)
}

const playVideo = (video: string) => {
  // TODO: implement video player
  console.log('Play video:', video)
}
</script>

<style scoped lang="scss">
.media-gallery {
  margin-top: 12px;
  display: grid;
  gap: 4px;
  border-radius: 12px;
  overflow: hidden;

  &.count-1 {
    grid-template-columns: 1fr;
  }

  &.count-2 {
    grid-template-columns: repeat(2, 1fr);
  }

  &.count-3, &.count-4 {
    grid-template-columns: repeat(2, 1fr);
  }

  &.count-5, &.count-6, &.count-7, &.count-8, &.count-9 {
    grid-template-columns: repeat(3, 1fr);
  }
}

.media-item {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  cursor: pointer;

  img, video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.2s;
  }

  &:hover img, &:hover video {
    transform: scale(1.02);
  }

  &.video-item {
    .play-icon {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 48px;
      height: 48px;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 18px;
      transition: all 0.2s;
    }

    &:hover .play-icon {
      background: rgba(76, 175, 130, 0.8);
      transform: translate(-50%, -50%) scale(1.1);
    }
  }
}

.more-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  font-weight: 600;
}
</style>
