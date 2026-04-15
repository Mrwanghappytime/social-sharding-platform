# Bug 修复：FollowButton 状态初始化

## 概述

修复 `FollowButton.vue` 组件在页面加载时未正确初始化关注状态的 bug。

## 行为变更

- **Before**: `onMounted` 调用 `relationStore.fetchFollowing` 时未传递 `userId` 参数，导致方法未执行
- **After**: `onMounted` 调用 `relationStore.fetchFollowing(props.userId)`，正确获取当前用户的关注列表

## 测试验证

1. 登录用户 A
2. 进入用户 B 的主页
3. 点击"关注"按钮
4. 验证按钮变为"已关注"状态
5. 刷新页面
6. 验证按钮仍显示"已关注"状态
