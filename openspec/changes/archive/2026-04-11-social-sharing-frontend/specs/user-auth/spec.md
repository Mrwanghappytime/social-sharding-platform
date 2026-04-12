# 用户认证模块 (user-auth)

## 概述

用户注册、登录和 Token 管理的功能规范。

## 功能列表

### 1.1 用户注册
- **描述**：新用户注册账号
- **表单字段**：用户名（唯一）、密码（≥6字符）
- **API 调用**：`POST /api/users/register`
- **请求体**：`{ username, password }`
- **响应**：`{ id, username, avatar }`
- **校验**：用户名唯一性检测，密码长度校验
- **注册成功后**：自动登录并跳转首页

### 1.2 用户登录
- **描述**：用户登录获取 Token
- **表单字段**：用户名、密码
- **API 调用**：`POST /api/users/login`
- **请求体**：`{ username, password }`
- **响应**：`{ token, user: { id, username, avatar } }`
- **Token 存储**：localStorage
- **登录成功后**：跳转首页，显示用户名

### 1.3 Token 刷新
- **描述**：自动刷新过期 Token
- **机制**：请求拦截器检测 401，自动跳转登录页

### 1.4 用户信息展示
- **描述**：展示当前登录用户信息
- **API 调用**：`GET /api/users/info`
- **Header**：携带 JWT Token
- **显示**：用户名、头像

### 1.5 头像更新
- **描述**：更新用户头像
- **API 调用**：`PUT /api/users/avatar`
- **请求体**：`{ avatar: url }`

## 组件列表

- `RegisterForm.vue` - 注册表单组件
- `LoginForm.vue` - 登录表单组件
- `UserAvatar.vue` - 用户头像组件
- `UserMenu.vue` - 用户菜单组件（登录/登出）

## 状态管理

- `useAuthStore` - 认证状态（Token、用户信息）
- `useUserStore` - 用户信息状态
