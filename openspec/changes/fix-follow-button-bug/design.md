## 上下文

前端 `FollowButton.vue` 组件在 `onMounted` 钩子中未正确调用 `fetchFollowing` 方法获取当前用户的关注列表，导致组件无法判断当前用户是否已关注目标用户。

## 目标 / 非目标

**目标：**
- 修复 `FollowButton.vue` 第40行的 bug
- 确保页面加载时正确获取关注列表

**非目标：**
- 不涉及后端接口修改
- 不涉及分表逻辑

## 决策

**修改内容：**
```javascript
// 错误代码 (第40行)
relationStore.fetchFollowing

// 正确代码
relationStore.fetchFollowing(props.userId)
```

## 风险 / 权衡

无重大风险。这是单一代码行的修复。
