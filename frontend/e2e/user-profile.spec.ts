import { test, expect } from '@playwright/test'

/**
 * 用户主页模块 E2E 测试
 * 测试个人主页、编辑资料功能
 */
test.describe('用户主页模块', () => {
  test.beforeEach(async ({ page }) => {
    // 登录 - 先加载页面再访问 localStorage
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

  test('进入自己的个人主页', async ({ page }) => {
    // 点击用户菜单进入主页
    await page.locator('.user-menu-trigger, [class*="user-menu"]').first().click()
    await page.waitForTimeout(500) // Wait for dropdown to open
    await page.locator('text=个人主页').click()
    await page.waitForURL(/\/user\/\d+/)

    // 验证是 userA 的主页
    await expect(page.locator('text=userA').first()).toBeVisible()
  })

  test('查看自己的粉丝数和关注数', async ({ page }) => {
    // 进入 userA 的主页
    await page.goto('/user/18')
    await page.waitForLoadState('networkidle')

    // 验证显示关注数和粉丝数
    await expect(page.locator('text=关注').first()).toBeVisible()
    await expect(page.locator('text=粉丝').first()).toBeVisible()
  })

  test('点击粉丝数进入粉丝列表', async ({ page }) => {
    // 进入主页
    await page.goto('/user/18')
    await page.waitForLoadState('networkidle')

    // 点击粉丝数
    await page.locator('text=粉丝').first().click()
    await page.waitForURL(/\/user\/\d+\/followers/)

    // 验证进入粉丝列表
    await expect(page.locator('text=粉丝').first()).toBeVisible()
  })

  test('点击关注数进入关注列表', async ({ page }) => {
    // 进入主页
    await page.goto('/user/18')
    await page.waitForLoadState('networkidle')

    // 点击关注数
    await page.locator('text=关注').first().click()
    await page.waitForURL(/\/user\/\d+\/following/)

    // 验证进入关注列表
    await expect(page.locator('text=关注').first()).toBeVisible()
  })

  test('进入编辑资料页面', async ({ page }) => {
    // 点击用户菜单
    await page.locator('.user-menu-trigger, [class*="user-menu"]').first().click()
    await page.waitForTimeout(500) // Wait for dropdown to open
    await page.locator('text=设置').click()
    await page.waitForURL('/settings')

    // 验证进入设置页
    await expect(page.locator('.settings-page').first()).toBeVisible()
  })

  test('查看他人主页显示关注按钮', async ({ page }) => {
    // 进入 userB 的主页（不是自己的）
    await page.goto('/user/19')
    await page.waitForLoadState('networkidle')

    // 验证显示关注/已关注按钮
    const hasFollowButton = await page.locator('button:has-text("关注"), button:has-text("已关注")').first().isVisible()
    expect(hasFollowButton).toBeTruthy()
  })
})
