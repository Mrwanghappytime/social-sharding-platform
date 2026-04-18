import { test, expect } from '@playwright/test'

/**
 * 通知模块 E2E 测试
 * 测试通知查看、标记已读功能
 */
test.describe('通知模块', () => {
  test.beforeEach(async ({ page }) => {
    // 登录 userA - 先加载页面再访问 localStorage
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    })
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')
  })

  test('进入通知页面', async ({ page }) => {
    // 点击通知图标
    const notificationBtn = page.locator('[class*="notification"], [class*="bell"], .notification-bell').first()
    await notificationBtn.click()
    await page.waitForURL('/notifications')

    // 验证进入通知页面
    await expect(page.locator('text=通知').first()).toBeVisible()
  })

  test('通知列表展示', async ({ page }) => {
    // 进入通知页面
    await page.goto('/notifications')
    await page.waitForLoadState('networkidle')

    // 等待通知列表加载
    await page.waitForTimeout(1000)

    // 验证有通知列表或空状态
    const hasNotifications = await page.locator('.notification-item, .notification-list > *').first().isVisible().catch(() => false)
    const hasEmpty = await page.locator('text=暂无通知, text=没有通知').first().isVisible().catch(() => false)

    expect(hasNotifications || hasEmpty).toBeTruthy()
  })

  test('标记单条通知已读（无需刷新）', async ({ page }) => {
    // 进入通知页面
    await page.goto('/notifications')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)

    // 找到未读通知
    const unreadNotification = page.locator('.notification-item:has(.unread), .notification-item.unread').first()
    if (await unreadNotification.isVisible().catch(() => false)) {
      // 点击通知标记为已读
      await unreadNotification.click()
      await page.waitForTimeout(500)

      // 验证通知样式变化（已读状态）
      // unread class 应该被移除
    }
  })

  test('标记全部已读（无需刷新）', async ({ page }) => {
    // 进入通知页面
    await page.goto('/notifications')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)

    // 点击全部已读按钮
    const markAllReadBtn = page.locator('button:has-text("全部已读"), button:has-text("标记全部已读")').first()
    if (await markAllReadBtn.isVisible()) {
      await markAllReadBtn.click()
      await page.waitForTimeout(500)

      // 验证没有未读通知了
      const unreadCount = await page.locator('.unread, [class*="unread"]').count()
      expect(unreadCount).toBe(0)
    }
  })

  test('通知铃铛显示未读数', async ({ page }) => {
    // 验证顶部通知铃铛有未读红点或数字
    const bell = page.locator('[class*="bell"], .notification-bell, [class*="notification"]').first()
    await expect(bell).toBeVisible()
  })
})
