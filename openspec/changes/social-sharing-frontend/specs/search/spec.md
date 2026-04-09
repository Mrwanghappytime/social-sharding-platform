# 搜索模块 (search)

## 概述

动态搜索功能，与后端 post-service 搜索 API 对接。

## 功能列表

### 7.1 搜索动态
- **描述**：按标题关键词搜索动态
- **API 调用**：`GET /api/posts/search?keyword={keyword}&page=1&size=10`
- **响应**：`{ records: [{ id, title, content, userId, createdAt }], total }`
- **搜索方式**：后端 MySQL LIKE 查询

### 7.2 搜索历史
- **描述**：记录用户的搜索关键词
- **存储**：localStorage
- **最大数量**：保留最近 10 条
- **清除**：支持一键清除历史

### 7.3 热门搜索
- **描述**：展示热门搜索关键词（可选）
- **展示**：搜索框下方热词标签

## 组件列表

- `SearchBar.vue` - 搜索框组件
- `SearchResults.vue` - 搜索结果列表组件
- `SearchHistory.vue` - 搜索历史组件

## 状态管理

- `useSearchStore` - 搜索状态管理
