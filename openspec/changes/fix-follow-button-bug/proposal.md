# 提案：修复前端关注按钮状态不更新问题

## 为什么

前端 `FollowButton.vue` 组件存在 bug：页面加载时未正确调用 `fetchFollowing(props.userId)` 获取当前用户的关注列表，导致 `following` 数组为空，`isFollowing()` 始终返回 `false`，关注按钮永远不会变成"已关注"状态。

## 变更内容

**Bug 修复：**
- 修改 `frontend/src/components/relation/FollowButton.vue` 第40行
- 将 `relationStore.fetchFollowing` 改为 `relationStore.fetchFollowing(props.userId)`

## 功能 (Capabilities)

### 新增功能
- 无

### 修改功能
- 无（此为 bug 修复，不涉及需求变更。原有 `social-graph` 规范已定义正确行为）

## 影响

**受影响的文件：**
- `frontend/src/components/relation/FollowButton.vue`

**测试验证：**
- 关注用户后，按钮应变为"已关注"状态
- 刷新页面后，关注状态应保持正确
