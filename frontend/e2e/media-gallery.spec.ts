import { test, expect } from '@playwright/test'

test.describe('媒体图片预览', () => {
  test('点击动态图片后放大展示图片预览层', async ({ page }) => {
    await page.route('**/api/posts/feed**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'success',
          data: {
            records: [
              {
                id: 1001,
                userId: 7,
                username: 'testuser1',
                avatar: '',
                title: '带图片的动态',
                content: '点击图片应该打开预览层',
                type: 'IMAGE',
                imageUrls: ['/files/images/test-preview.jpg'],
                likeCount: 0,
                commentCount: 0,
                isLiked: false,
                createdAt: new Date().toISOString()
              }
            ],
            total: 1,
            page: 1,
            size: 10
          }
        })
      })
    })

    await page.goto('/')

    await expect(page.locator('.image-item img')).toBeVisible()
    await page.locator('.image-item').first().click()

    await expect(page.locator('.el-image-viewer__wrapper')).toBeVisible()
    await expect(page.locator('.el-image-viewer__img')).toHaveAttribute('src', /test-preview\.jpg/)
  })
})
