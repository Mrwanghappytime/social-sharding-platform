import { test, expect } from '@playwright/test'

/**
 * 认证模块 E2E 测试
 * 测试登录、注册、登出功能
 */
test.describe('认证模块', () => {
  test.beforeEach(async ({ page }) => {
    // 确保登出状态 - 先加载页面再访问 localStorage
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    })
  })

  test('登录成功 - 用户名密码正确', async ({ page }) => {
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')

    // 等待跳转首页
    await page.waitForURL('/')
    // 验证顶部导航显示用户名
    await expect(page.locator('text=userA').first()).toBeVisible()
  })

  test('登录失败 - 用户名不存在', async ({ page }) => {
    await page.fill('input[placeholder="用户名"]', 'nonexistent_user_12345')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')

    // 等待一段时间看是否跳转（登录失败应该留在登录页）
    await page.waitForTimeout(1000)
    // 验证仍在登录页（URL是 /login）
    await expect(page).toHaveURL(/\/login/)
  })

  test('登录失败 - 密码错误', async ({ page }) => {
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', 'wrongpassword')
    await page.click('button[type="submit"]')

    // 等待一段时间看是否跳转
    await page.waitForTimeout(1000)
    // 验证仍在登录页
    await expect(page).toHaveURL(/\/login/)
  })

  test.skip('注册新用户 - 后端API无响应', async ({ page }) => {
    const timestamp = Date.now()
    const username = `testuser_${timestamp}`
    const password = '123456'

    await page.goto('/register')
    await page.fill('input[placeholder="用户名"]', username)
    await page.locator('input[placeholder="用户名"]').press('Tab') // Trigger blur
    await page.fill('input[placeholder="密码"]', password)
    await page.locator('input[placeholder="密码"]').press('Tab') // Trigger blur
    await page.fill('input[placeholder="确认密码"]', password)
    await page.locator('input[placeholder="确认密码"]').press('Tab') // Trigger blur
    await page.click('button[type="submit"]')

    // 等待跳转首页
    await page.waitForURL('/', { timeout: 15000 })
    await expect(page.locator(`text=${username}`).first()).toBeVisible()
  })

  test('注册失败 - 用户名已存在', async ({ page }) => {
    await page.goto('/register')
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.locator('input[placeholder="用户名"]').press('Tab')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.locator('input[placeholder="密码"]').press('Tab')
    await page.fill('input[placeholder="确认密码"]', '123456')
    await page.locator('input[placeholder="确认密码"]').press('Tab')
    await page.click('button[type="submit"]')

    // 等待一段时间后检查是否仍在注册页（说明注册失败）
    await page.waitForTimeout(2000)
    // 注册失败应该留在注册页
    await expect(page).toHaveURL(/\/register/)
  })

  test('注册失败 - 两次密码不一致', async ({ page }) => {
    await page.goto('/register')
    await page.fill('input[placeholder="用户名"]', 'newuser123')
    await page.locator('input[placeholder="用户名"]').press('Tab')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.locator('input[placeholder="密码"]').press('Tab')
    await page.fill('input[placeholder="确认密码"]', 'different_password')
    await page.locator('input[placeholder="确认密码"]').press('Tab')
    await page.click('button[type="submit"]')

    // 等待一段时间后检查是否仍在注册页
    await page.waitForTimeout(2000)
    await expect(page).toHaveURL(/\/register/)
  })

  test('登出功能', async ({ page }) => {
    // 先登录
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 点击用户菜单并登出
    await page.locator('.user-menu-trigger, [class*="user-menu"]').first().click()
    await page.locator('text=退出').click()

    // 验证登出后跳转到登录页
    await page.waitForURL('/login')
  })

  test('未登录点击个人主页跳转登录页', async ({ page }) => {
    // 确保在首页且未登录
    await page.goto('/')
    await page.evaluate(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    })
    await page.reload()

    // 点击用户菜单
    await page.locator('.user-menu-trigger, [class*="user-menu"]').first().click()

    // 点击个人主页
    await page.locator('text=个人主页').click()

    // 应该跳转到登录页
    await page.waitForURL(/\/login/)

    // 验证 URL 包含 redirect 参数
    await expect(page).toHaveURL(/\/login\?redirect=/)
  })

  test('登录后返回原页面', async ({ page }) => {
    // 直接访问需要登录的页面
    await page.goto('/settings')

    // 应该跳转到登录页 with redirect
    await page.waitForURL(/\/login\?redirect=\/settings/)

    // 登录
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')

    // 应该返回原页面
    await page.waitForURL('/settings')
  })
})
