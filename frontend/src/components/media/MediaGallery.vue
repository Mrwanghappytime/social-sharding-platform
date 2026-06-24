<template>
  <div class="media-gallery" :class="[`count-${images?.length || videos?.length}`]">
    <template v-if="images?.length">
      <div
        v-for="(img, index) in displayImages"
        :key="index"
        class="media-item image-item"
        @click.stop="preview(index)"
      >
        <img :src="img" alt="" />
        <div v-if="index === 8 && images.length > 9" class="more-overlay">
          +{{ images.length - 9 }}
        </div>
      </div>

      <teleport to="body">
        <el-image-viewer
          v-if="showViewer"
          :url-list="images"
          :initial-index="previewIndex"
          @close="closePreview"
        />
      </teleport>
    </template>

    <template v-if="videos?.length">
      <div
        v-for="(video, index) in videos"
        :key="index"
        class="media-item video-item"
        :style="videoItemStyle(index)"
        @click.stop
      >
        <video
          :ref="el => setVideoRef(el, index)"
          :src="video"
          :controls="playingVideoIndex === index"
          preload="metadata"
          @loadedmetadata="handleVideoMetadata(index)"
          @pause="handleVideoPause(index)"
          @ended="handleVideoPause(index)"
        ></video>
        <button
          v-if="playingVideoIndex !== index"
          class="play-icon"
          type="button"
          aria-label="播放视频"
          @click.stop="playVideo(index)"
        >
          ▶
        </button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  images?: string[]
  videos?: string[]
}>()

const showViewer = ref(false)
const previewIndex = ref(0)
const playingVideoIndex = ref<number | null>(null)
const videoRefs = ref<HTMLVideoElement[]>([])
const videoRatios = ref<Record<number, string>>({})

const displayImages = computed(() => {
  if (!props.images) return []
  return props.images.slice(0, 9)
})

const preview = (index: number) => {
  previewIndex.value = index
  showViewer.value = true
}

const closePreview = () => {
  showViewer.value = false
}

const setVideoRef = (el: Element | null, index: number) => {
  if (el instanceof HTMLVideoElement) {
    videoRefs.value[index] = el
  }
}

const handleVideoMetadata = (index: number) => {
  const video = videoRefs.value[index]
  if (!video?.videoWidth || !video?.videoHeight) return

  videoRatios.value = {
    ...videoRatios.value,
    [index]: `${video.videoWidth} / ${video.videoHeight}`
  }
}

const videoItemStyle = (index: number) => ({
  aspectRatio: videoRatios.value[index] || '16 / 9'
})

const playVideo = async (index: number) => {
  const currentVideo = videoRefs.value[index]
  if (!currentVideo) return

  videoRefs.value.forEach((video, videoIndex) => {
    if (videoIndex !== index && !video.paused) {
      video.pause()
    }
  })

  playingVideoIndex.value = index

  try {
    await currentVideo.play()
  } catch (error) {
    console.error('Failed to play video:', error)
    playingVideoIndex.value = null
  }
}

const handleVideoPause = (index: number) => {
  if (playingVideoIndex.value === index) {
    playingVideoIndex.value = null
  }
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
    transition: transform 0.2s;
  }

  img {
    object-fit: cover;
  }

  video {
    object-fit: cover;
    background: #000;
  }

  &:hover img {
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
      border: 0;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 18px;
      cursor: pointer;
      transition: all 0.2s;
      z-index: 1;
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
