# Docker 部署回归测试基线

**版本：** 1.0
**创建日期：** 2026-04-11
**适用范围：** Docker 容器化部署后功能验收

---

## 测试原则

Docker 部署后，后端服务提供的能力**不得有差别**。以下测试用例作为回归基线，每次功能开发迭代均需执行验证。

---

## 1. API 功能验证

### 1.1 用户服务 (user-service)

| 用例ID | 接口 | 验证点 |
|--------|------|--------|
| U-01 | POST /api/users/register | 用户注册成功，返回用户信息 |
| U-02 | POST /api/users/login | 登录成功，返回 JWT Token |
| U-03 | GET /api/users/{id} | 获取用户信息正常 |
| U-04 | PUT /api/users/avatar | 更新头像成功 |
| U-05 | PUT /api/users/password | 修改密码成功 |

### 1.2 帖子服务 (post-service)

| 用例ID | 接口 | 验证点 |
|--------|------|--------|
| P-01 | POST /api/posts | 创建帖子成功，返回帖子ID |
| P-02 | DELETE /api/posts/{id} | 删除帖子成功 |
| P-03 | GET /api/posts | 获取帖子列表正常（分页） |
| P-04 | GET /api/posts/{id} | 获取帖子详情正常 |
| P-05 | POST /api/posts/media | 上传媒体文件成功 |
| P-06 | GET /api/posts/search | 搜索帖子功能正常 |

### 1.3 互动服务 (interaction-service)

| 用例ID | 接口 | 验证点 |
|--------|------|--------|
| I-01 | POST /api/interactions/posts/{id}/like | 点赞成功 |
| I-02 | DELETE /api/interactions/posts/{id}/unlike | 取消点赞成功 |
| I-03 | POST /api/interactions/posts/{id}/comments | 评论成功 |
| I-04 | GET /api/interactions/posts/{id}/comments | 获取评论列表正常 |
| I-05 | GET /api/interactions/posts/{id}/likes | 获取点赞列表正常 |
| I-06 | GET /api/interactions/posts/{id}/liked | 验证是否点赞正常 |

### 1.4 关系服务 (relation-service)

| 用例ID | 接口 | 验证点 |
|--------|------|--------|
| R-01 | POST /api/relations/follow/{userId} | 关注成功 |
| R-02 | DELETE /api/relations/follow/{userId} | 取关成功 |
| R-03 | GET /api/relations/following/{userId}?page=1&size=10 | 获取关注列表（分页） |
| R-04 | GET /api/relations/followers/{userId}?page=1&size=10 | 获取粉丝列表（分页） |
| R-05 | GET /api/relations/counts/{userId} | 获取关系计数正常 |
| R-06 | GET /api/relations/is-following/{userId} | 验证是否关注正常 |
| R-07 | POST /api/relations/follow/{userId} (重复) | 重复关注返回错误 |
| R-08 | DELETE /api/relations/follow/{userId} (未关注) | 取消未关注返回错误 |
| R-09 | GET /api/relations/following/{userId} | 返回 followingCount, followersCount, isFollowing 字段 |

### 1.5 通知服务 (notification-service)

| 用例ID | 接口 | 验证点 |
|--------|------|--------|
| N-01 | GET /api/notifications | 获取通知列表正常 |
| N-02 | PUT /api/notifications/{id}/read | 标记单条已读成功 |
| N-03 | PUT /api/notifications/read-all | 标记全部已读成功 |

### 1.6 文件服务 (file-service)

| 用例ID | 接口 | 验证点 |
|--------|------|--------|
| F-01 | POST /files/upload | 文件上传成功，返回文件路径 |
| F-02 | GET /files/{type}/{uuid}.{ext} | 文件下载/访问正常 |

---

## 2. 服务间通信验证

### 2.1 Facade 层编排

| 用例ID | 验证场景 | 验证点 |
|--------|----------|--------|
| S-01 | facade → user-service | 用户相关 Dubbo 调用正常 |
| S-02 | facade → post-service | 帖子相关 Dubbo 调用正常 |
| S-03 | facade → interaction-service | 互动相关 Dubbo 调用正常 |
| S-04 | facade → relation-service | 关系相关 Dubbo 调用正常 |
| S-05 | facade → notification-service | 通知相关 Dubbo 调用正常 |

### 2.2 traceId 传播

| 用例ID | 验证场景 | 验证点 |
|--------|----------|--------|
| T-01 | HTTP → Facade | Facade 日志包含原始 traceId |
| T-02 | Facade → Dubbo Provider | Provider 日志包含传播的 traceId |
| T-03 | 完整调用链 | 全链路日志 traceId 一致 |

### 2.3 Nacos 服务注册发现

| 用例ID | 验证点 |
|--------|--------|
| N-01 | Nacos 控制台可见所有 8 个服务实例 |
| N-02 | 服务重启后重新注册正常 |

---

## 3. 日志系统验证

| 用例ID | 验证点 |
|--------|--------|
| L-01 | 日志包含服务名称 |
| L-02 | 日志包含 IP 和端口 |
| L-03 | 日志包含 traceId |
| L-04 | 日志包含线程名称 |
| L-05 | 日志包含日志级别 |
| L-06 | 日志包含来源位置（类名:方法名:行号） |
| L-07 | 日志格式统一，无乱码 |

---

## 4. 文件持久化验证

| 用例ID | 验证点 |
|--------|--------|
| PF-01 | file-service 挂载 /data/files 后上传文件成功 |
| PF-02 | 容器内能读取宿主机 /data/files 目录 |
| PF-03 | 容器重启后文件仍然存在 |

---

## 5. 测试用例汇总

| 类别 | 用例数 |
|------|--------|
| 1.1 用户服务 | 5 |
| 1.2 帖子服务 | 6 |
| 1.3 互动服务 | 6 |
| 1.4 关系服务 | 9 |
| 1.5 通知服务 | 3 |
| 1.6 文件服务 | 2 |
| 2.1 Facade 层编排 | 5 |
| 2.2 traceId 传播 | 3 |
| 2.3 Nacos 注册发现 | 2 |
| 3. 日志系统 | 7 |
| 4. 文件持久化 | 3 |
| **总计** | **51** |

---

## 6. 测试执行记录

| 日期 | 执行人 | 通过数 | 失败数 | 备注 |
|------|--------|--------|--------|------|
| 2026-04-15 | Claude | 12/12 | 0 | 关系服务详细测试（userA/userB），包括关注/取关/分页/数量/isFollowing/错误处理 |

---

## 附录：关系服务详细测试记录 (2026-04-15)

**测试用户：**
- userA (ID=18)
- userB (ID=19)

### 测试用例详情

| 用例编号 | 测试项 | 结果 |
|----------|--------|------|
| TC-R01 | UserA 关注 UserB | ✓ 通过 |
| TC-R02 | 获取关注列表（分页） | ✓ 通过，包含 followingCount、followersCount、isFollowing |
| TC-R03 | 获取粉丝列表（分页） | ✓ 通过 |
| TC-R04 | 获取数量统计 | ✓ 通过，从数量表读取 |
| TC-R05 | UserB 关注 UserA（双向） | ✓ 通过 |
| TC-R06 | 取消关注 | ✓ 通过 |
| TC-R07 | 取消关注后数量更新 | ✓ 通过 |
| TC-R08 | isFollowing 判断 | ✓ 通过 |
| TC-R09 | 重复关注报错 | ✓ 通过 |
| TC-R10 | 取消未关注用户报错 | ✓ 通过 |
| TC-R11 | 取消关注不存在的用户报错 | ✓ 通过 |
| TC-R12 | 空列表返回 | ✓ 通过 |

**详细测试报告：** `docs/test-baseline-relation-api-20260415.md`
