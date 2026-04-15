## 1. Bug 修复

- [x] 1.1 修复 FollowButton.vue 第40行，将 `relationStore.fetchFollowing` 改为 `relationStore.fetchFollowing(props.userId)`

## 2. 测试验证 (手动测试)

- [ ] 2.1 启动前端开发服务器
- [ ] 2.2 登录用户 A
- [ ] 2.3 进入用户 B 的主页
- [ ] 2.4 点击"关注"按钮，验证按钮变为"已关注"
- [ ] 2.5 刷新页面，验证按钮仍显示"已关注"
