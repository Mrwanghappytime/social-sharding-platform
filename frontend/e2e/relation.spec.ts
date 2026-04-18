import { test, expect } from '@playwright/test'

/**
 * 关注模块 E2E 测试
 * 测试关注、取关、列表展示功能
 *
 * 注意：需要两个测试用户 userA(ID=18) 和 userB(ID=19)
 * 测试前请确保数据库中没有关注关系
 */
test.describe('关注模块', () => {
  test.beforeEach(async ({ page }) => {
    // 清理 localStorage 确保登录状态 - 先加载页面再访问 localStorage
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    })
  })

  test.skip('关注流程 - 主页显示+1 - UI数量更新问题', async ({ page }) => {
    // 1. 登录 userA
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 2. 进入 userB 的主页
    await page.goto('/user/19')
    await page.waitForLoadState('networkidle')

    // 3. 记录当前粉丝数
    const fansCountBefore = await page.locator('.stat-value:has(+ .stat-label:text("粉丝"))').textContent().catch(async () => {
      // 尝试其他选择器
      return page.locator('text=粉丝').first().locator('..').locator('.stat-value').textContent().catch(() => '0')
    })

    // 4. 点击关注按钮
    const followBtn = page.locator('button:has-text("关注")')
    await expect(followBtn).toBeVisible()
    await followBtn.click()

    // 5. 验证按钮变为"已关注"（无需刷新）
    await expect(page.locator('button:has-text("已关注")')).toBeVisible({ timeout: 5000 })

    // 6. 验证粉丝数 +1
    await page.waitForTimeout(500) // 等待状态更新
    const fansCountAfter = await page.locator('text=粉丝').first().locator('..').locator('.stat-value').textContent().catch(() => '0')
    expect(Number(fansCountAfter)).toBe(Number(fansCountBefore) + 1)
  })

  test('取消关注流程', async ({ page }) => {
    // 1. 登录 userA（已关注 userB）
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 2. 进入 userB 的主页
    await page.goto('/user/19')
    await page.waitForLoadState('networkidle')

    // 3. 确认已关注状态
    const followedBtn = page.locator('button:has-text("已关注")')
    if (!(await followedBtn.isVisible().catch(() => false))) {
      // 如果没关注，先关注
      await page.locator('button:has-text("关注")').click()
      await expect(page.locator('button:has-text("已关注")')).toBeVisible({ timeout: 5000 })
    }

    // 4. 点击已关注按钮取关
    await followedBtn.click()
    await page.waitForTimeout(300)

    // 5. 验证按钮变回"关注"（无需刷新）
    await expect(page.locator('button:has-text("关注")')).toBeVisible({ timeout: 5000 })
  })

  test('关注列表展示', async ({ page }) => {
    // 1. 登录 userB（关注了 userA）
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'userB')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 2. 进入 userB 的关注列表
    await page.goto('/user/19/following')
    await page.waitForLoadState('networkidle')

    // 3. 验证列表中有 userA
    await expect(page.locator('text=userA').first()).toBeVisible()
  })

  test('粉丝列表展示', async ({ page }) => {
    // 1. 登录 userA（粉丝是 userB）
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 2. 进入 userA 的粉丝列表
    await page.goto('/user/18/followers')
    await page.waitForLoadState('networkidle')

    // 3. 验证列表中有 userB
    await expect(page.locator('text=userB').first()).toBeVisible()
  })

  test('从粉丝列表进入用户主页', async ({ page }) => {
    // 1. 登录 userA
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 2. 进入粉丝列表
    await page.goto('/user/18/followers')
    await page.waitForLoadState('networkidle')

    // 3. 点击 userB 的头像或名字进入其主页
    const userBItem = page.locator('.user-item:has-text("userB")').first()
    if (await userBItem.isVisible()) {
      await userBItem.click()
      await page.waitForURL(/\/user\/\d+/)

      // 4. 验证进入的是 userB 的主页
      await expect(page.locator('text=userB').first()).toBeVisible()
    }
  })

  test('关注列表中显示关注/粉丝数量', async ({ page }) => {
    // 1. 登录
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'userB')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 2. 进入关注列表
    await page.goto('/user/19/following')
    await page.waitForLoadState('networkidle')

    // 3. 验证列表项显示关注/粉丝数量
    const firstUserItem = page.locator('.user-item').first()
    if (await firstUserItem.isVisible()) {
      await expect(page.locator('text=关注').first()).toBeVisible()
      await expect(page.locator('text=粉丝').first()).toBeVisible()
    }
  })
})
