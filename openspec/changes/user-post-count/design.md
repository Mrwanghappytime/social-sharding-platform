## 设计细节

### 数据库表

```sql
CREATE TABLE user_post_count (
    user_id BIGINT PRIMARY KEY,
    post_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id)
);
```

### 接口设计

**PostService Dubbo 接口**
```java
public interface PostService {
    // 现有方法...

    // 新增
    Long getPostCount(Long userId);
}
```

**RelationFacadeController 聚合**
```
GET /relations/counts/{userId}

Response:
{
    "followingCount": 100,
    "followerCount": 50,
    "postsCount": 25  // 新增
}
```

### 事务保证

**重要原则**：`incrementPostCount` 和 `decrementPostCount` 是**内部方法**，不对外暴露，必须与 `createPost`/`deletePost` 在同一事务内调用。

```java
@Transactional
public PostDTO createPost(CreatePostRequest request) {
    // 1. 创建帖子记录
    Post post = postRepository.save(...);

    // 2. 在同一事务内更新数量
    incrementPostCount(post.getUserId());

    return post.toDTO();
}

@Transactional
public void deletePost(Long postId) {
    Post post = postRepository.findById(postId);
    // 1. 删除帖子
    postRepository.delete(postId);

    // 2. 在同一事务内更新数量
    decrementPostCount(post.getUserId());
}

// 私有方法，不对外暴露
private void incrementPostCount(Long userId) { ... }
private void decrementPostCount(Long userId) { ... }
```

### 初始化问题

用户第一次发帖子时，user_post_count 表中没有记录：
- `incrementPostCount` 需要先检查是否存在
- 不存在则 INSERT，存在则 UPDATE

```java
private void incrementPostCount(Long userId) {
    userPostCountRepository.findById(userId)
        .ifPresentOrElse(
            count -> {
                count.setPostCount(count.getPostCount() + 1);
                userPostCountRepository.save(count);
            },
            () -> {
                UserPostCount newCount = new UserPostCount();
                newCount.setUserId(userId);
                newCount.setPostCount(1L);
                userPostCountRepository.save(newCount);
            }
        );
}
```