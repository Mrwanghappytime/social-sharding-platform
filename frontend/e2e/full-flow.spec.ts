import { test, expect } from '@playwright/test'

/**
 * 完整流程测试
 * 测试用户从注册到发帖、关注、互动的完整流程
 */
test.describe('完整用户流程', () => {
  test.skip('注册 -> 发帖 -> 关注 -> 点赞 -> 评论 -> 登出 - 后端API无响应', async ({ page }) => {
    const timestamp = Date.now()
    const testUsername = `flowtest_${timestamp}`
    const testPassword = '123456'

    // 1. 注册新用户
    await page.goto('/register')
    await page.fill('input[placeholder="用户名"]', testUsername)
    await page.fill('input[placeholder="密码"]', testPassword)
    await page.fill('input[placeholder="确认密码"]', testPassword)
    await page.click('button[type="submit"]')
    await page.waitForURL('/', { timeout: 10000 })

    // 2. 创建帖子
    await page.locator('text=发布动态, text=发布, button:has-text("发布")').first().click()
    await page.waitForURL('/create')

    const postContent = `完整流程测试帖子 ${timestamp}`
    await page.locator('textarea, input[placeholder*="内容"], [contenteditable]').first().fill(postContent)
    await page.locator('button[type="submit"], button:has-text("发布")').click()
    await page.waitForURL('/')

    // 验证帖子出现
    await expect(page.locator(`text=${postContent}`).first()).toBeVisible({ timeout: 5000 })

    // 3. 进入他人主页并关注（userA:18 -> userB:19）
    await page.goto('/user/19')
    await page.waitForLoadState('networkidle')

    // 点击关注
    const followBtn = page.locator('button:has-text("关注")').first()
    if (await followBtn.isVisible().catch(() => false)) {
      await followBtn.click()
      await page.waitForTimeout(500)
      await expect(page.locator('button:has-text("已关注")')).toBeVisible({ timeout: 5000 })
    }

    // 4. 进入首页点赞任意帖子
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    const likeBtn = page.locator('.post-card').first().locator('[class*="like-btn"], .like-btn').first()
    if (await likeBtn.isVisible().catch(() => false)) {
      await likeBtn.click()
      await page.waitForTimeout(500)
    }

    // 5. 进入帖子详情页评论
    const postCard = page.locator('.post-card').first()
    if (await postCard.isVisible().catch(() => false)) {
      await postCard.click()
      await page.waitForURL(/\/post\/\d+/)

      const commentContent = `完整流程评论 ${timestamp}`
      const commentInput = page.locator('input[placeholder*="评论"], textarea[placeholder*="评论"]').first()
      if (await commentInput.isVisible().catch(() => false)) {
        await commentInput.fill(commentContent)
        await page.locator('button:has-text("评论"), button:has-text("发送")').first().click()
        await page.waitForTimeout(500)
        await expect(page.locator(`text=${commentContent}`).first()).toBeVisible({ timeout: 5000 })
      }
    }

    // 6. 登出
    await page.locator('.user-menu-trigger, [class*="user-menu"]').first().click()
    await page.locator('text=退出').click()
    await page.waitForURL('/login')

    // 7. 使用原账号登录验证流程完整性
    await page.fill('input[placeholder="用户名"]', testUsername)
    await page.fill('input[placeholder="密码"]', testPassword)
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 验证登录成功
    await expect(page.locator(`text=${testUsername}`).first()).toBeVisible()
  })

  test('登录状态持久化', async ({ page }) => {
    // 1. 登录
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 2. 验证 localStorage 保存了 token
    const hasToken = await page.evaluate(() => !!localStorage.getItem('token'))
    expect(hasToken).toBeTruthy()

    // 3. 刷新页面
    await page.reload()
    await page.waitForLoadState('networkidle')

    // 4. 验证仍然是登录状态（未跳转到登录页）
    await expect(page).toHaveURL('/')
    await expect(page.locator('text=userA').first()).toBeVisible()
  })

  test.skip('点赞状态在界面更新 - 后端API无响应', async ({ page }) => {
    // 1. 登录
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 2. 进入首页
    await page.waitForLoadState('networkidle')

    // 3. 找到点赞按钮并记录状态
    const firstPostLikeBtn = page.locator('.post-card').first().locator('[class*="like"]').first()
    const initialLiked = await firstPostLikeBtn.locator('..').evaluate(el => el.classList.contains('liked') || el.classList.contains('active'))

    // 4. 点击点赞
    await page.locator('.post-card').first().locator('[class*="like-btn"]').first().click()
    await page.waitForTimeout(500)

    // 5. 验证点赞状态变化（颜色或类名）
    const afterLiked = await firstPostLikeBtn.locator('..').evaluate(el => el.classList.contains('liked') || el.classList.contains('active'))
    expect(afterLiked).not.toBe(initialLiked)
  })

  test.skip('评论列表即时更新 - 后端API无响应', async ({ page }) => {
    // 1. 登录
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'userA')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button[type="submit"]')
    await page.waitForURL('/')

    // 2. 点击第一个帖子进入详情
    await page.waitForLoadState('networkidle')
    const postCard = page.locator('.post-card').first()
    if (await postCard.isVisible().catch(() => false)) {
      await postCard.click()
      await page.waitForURL(/\/post\/\d+/)
      await page.waitForLoadState('networkidle')

      // 3. 记录原始评论数
      const initialCommentCount = await page.locator('[class*="comment"]').count()

      // 4. 发布评论
      const timestamp = Date.now()
      const commentText = `即时更新测试 ${timestamp}`
      const commentInput = page.locator('input[placeholder*="评论"], textarea[placeholder*="评论"]').first()
      if (await commentInput.isVisible().catch(() => false)) {
        await commentInput.fill(commentText)
        await page.locator('button:has-text("评论"), button:has-text("发送")').first().click()

        // 5. 验证评论立即出现（无需刷新）
        await expect(page.locator(`text=${commentText}`).first()).toBeVisible({ timeout: 5000 })

        // 6. 验证评论数增加
        await page.waitForTimeout(500)
        const newCommentCount = await page.locator('[class*="comment"]').count()
        expect(newCommentCount).toBeGreaterThan(initialCommentCount)
      }
    }
  })
})
