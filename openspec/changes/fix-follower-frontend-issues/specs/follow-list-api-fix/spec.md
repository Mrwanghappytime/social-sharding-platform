## 新增需求

### 需求:前端关注列表API必须使用正确的路径参数

前端调用 `getFollowing(userId)` 和 `getFollowers(userId)` 时，必须使用后端的 RESTful 路径参数 `/relations/following/{userId}` 和 `/relations/followers/{userId}`，禁止使用 query 参数 `?userId=xxx`。

#### 场景:获取指定用户的关注列表
- **当** 前端调用 `getFollowing(targetUserId)` 获取目标用户的关注列表
- **那么** 实际发送请求到 `GET /relations/following/{targetUserId}`

#### 场景:获取指定用户的粉丝列表
- **当** 前端调用 `getFollowers(targetUserId)` 获取目标用户的粉丝列表
- **那么** 实际发送请求到 `GET /relations/followers/{targetUserId}`

### 需求:关注/粉丝列表必须支持分页

后端接口必须支持 `page` 和 `size` 参数，返回分页结果。

#### 场景:获取关注列表第1页，每页10条
- **当** 调用 `GET /relations/following/{userId}?page=1&size=10`
- **那么** 返回最多10条数据

#### 场景:从粉丝列表进入关注列表
- **当** 用户在粉丝列表页点击某用户的"关注"链接进入该用户的关注列表页
- **那么** 页面正确显示该用户的关注列表（而不是当前登录用户的关注列表）

### 需求:批量查询当前用户对列表中每个用户的是否关注状态

后端必须提供批量查询接口，facade 调用一次即可获取当前用户对列表中每个用户是否关注。

#### 场景:获取关注列表时同时获取关注状态
- **当** 调用 `GET /relations/following/{userId}?page=1&size=10` 且当前登录用户访问自己的列表
- **那么** 返回列表中每个用户包含 `isFollowing` 字段

#### 场景:获取粉丝列表时同时获取关注状态
- **当** 调用 `GET /relations/followers/{userId}?page=1&size=10`
- **那么** 返回列表中每个用户包含 `isFollowing` 字段（表示当前登录用户是否关注了此人）
