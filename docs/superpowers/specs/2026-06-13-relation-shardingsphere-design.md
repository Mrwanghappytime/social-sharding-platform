# 关注/粉丝关系 ShardingSphere 分表改造设计

日期：2026-06-13

## 背景

项目当前已经在 `sql/schema.sql` 中创建了关注关系分表：

- `following_0 ~ following_15`：我关注的人，按 `follower_id % 16` 分片
- `followers_0 ~ followers_15`：关注我的人，按 `following_id % 16` 分片

同时 `relation-service` 中存在 `TableShardingUtil`，也定义了相同的分片规则。

但是当前实际业务代码仍通过 JPA 实体和 Repository 操作逻辑单表：

- `Following` 映射到 `following`
- `Follower` 映射到 `followers`
- `RelationServiceImpl` 通过 `FollowingRepository` / `FollowerRepository` 执行保存、查询、删除

因此当前分表结构没有被真正使用。

## 目标

使用 ShardingSphere-JDBC 在 `relation-service` 中实现单库分表，让现有 JPA Repository 操作逻辑表时，由 ShardingSphere 自动路由到真实分表。

启用后：

- 写入 `following` 逻辑表时，实际写入 `following_{follower_id % 16}`
- 写入 `followers` 逻辑表时，实际写入 `followers_{following_id % 16}`
- 查询关注列表、粉丝列表、关注状态时走对应分表
- 保持 Dubbo API 和 facade API 不变

## 非目标

本次不做以下事项：

- 不迁移旧单表 `following` / `followers` 中的历史数据
- 不实现双读过渡
- 不分库，只做单 MySQL 库内分表
- 不重构 relation-service 对外接口
- 不替换 JPA 为 MyBatis 或 JdbcTemplate

## 数据处理策略

用户已确认：**不迁移旧单表数据**。

因此改造后：

- 旧 `following` 表数据不会被 ShardingSphere 路由读取
- 旧 `followers` 表数据不会被 ShardingSphere 路由读取
- 关注关系以 `following_0~15`、`followers_0~15` 为准

这意味着测试环境中旧关注关系可能不可见，需要重新建立关注关系进行验证。

## 分片规则

### following 逻辑表

含义：某个用户关注了哪些人。

- 逻辑表：`following`
- 真实表：`following_0 ~ following_15`
- 分片键：`follower_id`
- 分片表达式：`following_${follower_id % 16}`

示例：

```text
follower_id = 7  -> following_7
follower_id = 18 -> following_2
```

### followers 逻辑表

含义：某个用户被哪些人关注。

- 逻辑表：`followers`
- 真实表：`followers_0 ~ followers_15`
- 分片键：`following_id`
- 分片表达式：`followers_${following_id % 16}`

示例：

```text
following_id = 8  -> followers_8
following_id = 23 -> followers_7
```

## 持久层设计

### 继续保留逻辑表实体

`Following` 继续保持：

```java
@Table(name = "following")
```

`Follower` 继续保持：

```java
@Table(name = "followers")
```

在 ShardingSphere 中，这两个表名是逻辑表名，不应改成具体物理表名。

### Repository 保持基本不变

当前 Repository 查询大多带有分片键，可以被 ShardingSphere 精确路由：

| 查询 | 表 | 分片键是否存在 | 说明 |
|---|---|---|---|
| `findByFollowerIdAndFollowingId` | `following` | 是，`follower_id` | 精确路由 |
| `findByFollowerIdOrderByCreatedAtDesc` | `following` | 是，`follower_id` | 精确路由 |
| `countByFollowerId` | `following` | 是，`follower_id` | 精确路由 |
| `findFollowingIdsByFollowerIdAndTargetIds` | `following` | 是，`follower_id` | 精确路由 |
| `findByFollowingIdOrderByCreatedAtDesc` | `followers` | 是，`following_id` | 精确路由 |
| `countByFollowingId` | `followers` | 是，`following_id` | 精确路由 |

需要注意：`FollowingRepository.countByFollowingId` 在 `following` 逻辑表上按 `following_id` 查询，不包含 `following` 的分片键 `follower_id`，会触发全分片路由。当前主业务路径不依赖它，应避免新增使用。

## 事务设计

`follow()` 会写入：

1. `following_{follower_id % 16}`
2. `followers_{following_id % 16}`
3. `user_relation_count`

当前所有表仍在同一个 MySQL 数据库中，属于单库分表。Spring 本地事务可以覆盖这些写操作，不引入分布式事务。

未来如果改成分库，需要重新设计事务策略，不属于本次范围。

## 配置设计

只在 `relation-service` 引入 ShardingSphere 依赖和配置，避免影响其他服务。

需要在 `relation-service/pom.xml` 添加 ShardingSphere-JDBC Spring Boot Starter。

需要在 `relation-service/src/main/resources/application.yml` 中配置：

- `spring.shardingsphere.datasource.names=ds0`
- `ds0` 指向原 MySQL 数据源
- `following` / `followers` 两张逻辑表的 actual-data-nodes
- inline 分片算法
- SQL 显示配置用于验证

注意：必须提供完整 ShardingSphere 配置，避免再次出现之前的自动配置空指针启动失败。

## 验证方案

### 启动验证

- `mvn clean package -pl relation-service -am -DskipTests`
- 构建 relation-service Docker 镜像
- 重启 relation-service 容器
- 确认服务启动成功，没有 ShardingSphere 自动配置异常

### 路由写入验证

使用测试关系：

```text
followerId = 7
followingId = 8
```

执行关注后验证：

```sql
SELECT * FROM following_7 WHERE follower_id = 7 AND following_id = 8;
SELECT * FROM followers_8 WHERE follower_id = 7 AND following_id = 8;
```

预期：两条记录存在。

同时抽查错误分表：

```sql
SELECT * FROM following_0 WHERE follower_id = 7 AND following_id = 8;
SELECT * FROM followers_0 WHERE follower_id = 7 AND following_id = 8;
```

预期：无记录。

### 删除验证

执行取消关注后：

```sql
SELECT * FROM following_7 WHERE follower_id = 7 AND following_id = 8;
SELECT * FROM followers_8 WHERE follower_id = 7 AND following_id = 8;
```

预期：无记录。

### 查询验证

验证以下接口或 Dubbo 方法结果正确：

- `isFollowing(7, 8)`
- `getFollowingListPaged(7, page, size)`
- `getFollowersListPaged(8, page, size)`
- `getRelationCounts(7)` 和 `getRelationCounts(8)`

## 风险与应对

### 风险 1：ShardingSphere 与 Spring Boot 版本兼容

应对：选用支持 Spring Boot 3 / Jakarta 的 ShardingSphere 版本，并以 relation-service 启动成功作为第一验证门槛。

### 风险 2：配置不完整导致启动失败

应对：只在 relation-service 添加依赖；配置完整数据源、真实节点、分片算法；打包后先单独启动 relation-service 验证。

### 风险 3：旧数据不可见

应对：这是已确认策略。不迁移旧单表数据，测试时重新建立关系。

### 风险 4：不带分片键查询触发全路由

应对：当前主业务路径都带分片键；避免新增依赖 `FollowingRepository.countByFollowingId` 这类非分片键查询。

## 完成标准

- relation-service 启动成功
- 新关注关系写入正确分表
- 取消关注能从正确分表删除数据
- 关注列表、粉丝列表、关注状态、计数功能正确
- 旧单表 `following` / `followers` 不再作为新关系写入目标
