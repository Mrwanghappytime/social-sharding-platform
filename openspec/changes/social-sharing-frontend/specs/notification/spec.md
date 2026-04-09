# 通知模块 (notification)

## 概述

实时通知推送和通知列表，与后端 notification-service 对接。

## 功能列表

### 6.1 WebSocket 连接
- **描述**：建立 WebSocket 连接接收实时通知
- **URL**：`ws://localhost:8080/ws/notifications`
- **认证**：通过 URL 参数或 Header 携带 Token
- **消息格式**：`{ id, type, actorId, targetId, targetType, createdAt }`
- **通知类型**：LIKE, COMMENT, FOLLOW

### 6.2 通知列表
- **描述**：分页获取通知列表
- **API 调用**：`GET /api/notifications?page=1&size=20`
- **响应**：`{ records: [{ id, type, actorId, targetId, isRead, createdAt }], total }`
- **类型**：LIKE, COMMENT, FOLLOW

### 6.3 标记单条已读
- **描述**：将单条通知标记为已读
- **API 调用**：`PUT /api/notifications/read/{id}`

### 6.4 标记全部已读
- **描述**：将所有通知标记为已读
- **API 调用**：`PUT /api/notifications/read-all`

### 6.5 获取未读数
- **描述**：获取未读通知数量
- **API 调用**：`GET /api/notifications/unread-count`
- **响应**：`{ count }`
- **显示**：红点数字角标

### 6.6 通知推送显示
- **描述**：实时弹窗显示新通知
- **样式**：右下角弹出通知卡片
- **自动消失**：3秒后自动关闭

## 组件列表

- `NotificationBell.vue` - 通知铃铛组件（含未读数角标）
- `NotificationList.vue` - 通知列表组件
- `NotificationItem.vue` - 单条通知组件
- `NotificationToast.vue` - 通知弹窗组件
- `useWebSocket.ts` - WebSocket 组合式函数

## 状态管理

- `useNotificationStore` - 通知状态管理
