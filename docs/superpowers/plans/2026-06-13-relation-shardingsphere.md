# Relation ShardingSphere Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable real single-database table sharding for relation-service `following` and `followers` logical tables using ShardingSphere-JDBC.

**Architecture:** Keep existing JPA entities and repositories using logical table names (`following`, `followers`). Add ShardingSphere only to `relation-service`, configure one physical datasource `ds0`, and route `following` by `follower_id % 16` and `followers` by `following_id % 16`. Existing old single-table data is intentionally not migrated.

**Tech Stack:** Spring Boot 3.2, Spring Data JPA, Hibernate, MySQL, ShardingSphere-JDBC, Docker, Maven.

---

## File Structure

### Modified files

- `pom.xml`
  - Add a managed property for ShardingSphere version.
  - Add dependency management entry for ShardingSphere starter so the child module can omit explicit version.

- `relation-service/pom.xml`
  - Add ShardingSphere-JDBC Spring Boot starter dependency.
  - Add `spring-boot-starter-test` for integration tests.
  - Add Testcontainers dependencies for MySQL-based routing verification.

- `relation-service/src/main/resources/application.yml`
  - Replace direct `spring.datasource.*` as the active application datasource with `spring.shardingsphere.datasource.*`.
  - Configure logical tables `following` and `followers`.
  - Keep Redis, Dubbo, JWT, logging unchanged.
  - Disable JPA DDL auto-update against sharded logical tables to avoid ShardingSphere/JPA schema-generation surprises.

- `relation-service/src/test/java/com/social/relation/sharding/RelationShardingIntegrationTest.java`
  - New integration test using Testcontainers MySQL.
  - Starts relation-service application context with ShardingSphere config pointed at a temporary MySQL container.
  - Creates shard tables.
  - Verifies `follow()` writes to `following_{followerId % 16}` and `followers_{followingId % 16}`.
  - Verifies `unfollow()` deletes from those exact shards.

- `docs/superpowers/specs/2026-06-13-relation-shardingsphere-design.md`
  - Already created. No change required unless implementation reveals a design adjustment.

### Files intentionally not changed

- `common/src/main/java/com/social/common/entity/Following.java`
  - Keep `@Table(name = "following")` because ShardingSphere needs logical table names.

- `common/src/main/java/com/social/common/entity/Follower.java`
  - Keep `@Table(name = "followers")` because ShardingSphere needs logical table names.

- `relation-service/src/main/java/com/social/relation/service/RelationServiceImpl.java`
  - Keep existing repository-based business logic unless tests show a routing incompatibility.

---

## Task 1: Add a failing integration test proving relation writes must use shard tables

**Files:**
- Create: `relation-service/src/test/java/com/social/relation/sharding/RelationShardingIntegrationTest.java`
- Modify later in Task 2: `relation-service/pom.xml`

- [ ] **Step 1: Create the integration test file**

Create `relation-service/src/test/java/com/social/relation/sharding/RelationShardingIntegrationTest.java` with this content:

```java
package com.social.relation.sharding;

import com.social.common.api.RelationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "dubbo.application.name=relation-service-test",
        "dubbo.registry.address=N/A",
        "dubbo.protocol.port=-1",
        "spring.data.redis.host=127.0.0.1",
        "spring.data.redis.port=6379",
        "spring.jpa.hibernate.ddl-auto=none",
        "jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=false",
        "jpa.show-sql=false",
        "spring.shardingsphere.props.sql-show=true"
})
class RelationShardingIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("social_platform_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.shardingsphere.datasource.ds0.jdbc-url", MYSQL::getJdbcUrl);
        registry.add("spring.shardingsphere.datasource.ds0.username", MYSQL::getUsername);
        registry.add("spring.shardingsphere.datasource.ds0.password", MYSQL::getPassword);
    }

    @Autowired
    private RelationService relationService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        createRelationCountTable();
        createShardTables();
        truncateAllTables();
    }

    @Test
    void followWritesFollowingAndFollowersToExpectedShardTables() {
        long followerId = 7L;
        long followingId = 8L;

        relationService.follow(followerId, followingId);

        assertThat(countRows("following_7", followerId, followingId)).isEqualTo(1);
        assertThat(countRows("followers_8", followerId, followingId)).isEqualTo(1);
        assertThat(countRows("following_0", followerId, followingId)).isZero();
        assertThat(countRows("followers_0", followerId, followingId)).isZero();
    }

    @Test
    void unfollowDeletesFromExpectedShardTables() {
        long followerId = 7L;
        long followingId = 8L;

        relationService.follow(followerId, followingId);
        relationService.unfollow(followerId, followingId);

        assertThat(countRows("following_7", followerId, followingId)).isZero();
        assertThat(countRows("followers_8", followerId, followingId)).isZero();
    }

    @Test
    void isFollowingReadsFromExpectedFollowingShard() {
        long followerId = 18L;
        long followingId = 23L;
        jdbcTemplate.update("""
                INSERT INTO following_2 (follower_id, following_id, created_at, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, followerId, followingId);

        assertThat(relationService.isFollowing(followerId, followingId)).isTrue();
    }

    private void createRelationCountTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_relation_count (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    following_count BIGINT NOT NULL DEFAULT 0,
                    follower_count BIGINT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void createShardTables() {
        for (int i = 0; i < 16; i++) {
            jdbcTemplate.execute(relationTableDdl("following_" + i));
            jdbcTemplate.execute(relationTableDdl("followers_" + i));
        }
    }

    private String relationTableDdl(String tableName) {
        return """
                CREATE TABLE IF NOT EXISTS %s (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    follower_id BIGINT NOT NULL,
                    following_id BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE INDEX uk_follower_following (follower_id, following_id),
                    INDEX idx_following_id (following_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.formatted(tableName);
    }

    private void truncateAllTables() {
        for (int i = 0; i < 16; i++) {
            jdbcTemplate.execute("TRUNCATE TABLE following_" + i);
            jdbcTemplate.execute("TRUNCATE TABLE followers_" + i);
        }
        jdbcTemplate.execute("TRUNCATE TABLE user_relation_count");
    }

    private int countRows(String tableName, long followerId, long followingId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE follower_id = ? AND following_id = ?",
                Integer.class,
                followerId,
                followingId
        );
        return count == null ? 0 : count;
    }
}
```

- [ ] **Step 2: Run test to verify it fails because dependencies are missing**

Run:

```bash
cd G:/claude-project/social-sharding-platform
mvn test -pl relation-service -Dtest=RelationShardingIntegrationTest
```

Expected result:

- FAIL during test compilation.
- Expected missing packages include `org.testcontainers.*`, `org.junit.jupiter.*`, or AssertJ/Spring test dependencies.
- This is the correct RED state because the test expresses the required behavior before infrastructure is added.

- [ ] **Step 3: Commit the failing test only**

Run:

```bash
git add relation-service/src/test/java/com/social/relation/sharding/RelationShardingIntegrationTest.java
git commit -m "test: add relation sharding integration spec"
```

If the user does not want intermediate commits, skip the commit but leave the test file staged or unstaged according to user preference.

---

## Task 2: Add ShardingSphere and test dependencies

**Files:**
- Modify: `pom.xml:27-41`, `pom.xml:112-124`
- Modify: `relation-service/pom.xml:16-73`

- [ ] **Step 1: Add version properties and dependency management in root `pom.xml`**

In `pom.xml`, add these properties inside `<properties>`:

```xml
<shardingsphere.version>5.5.0</shardingsphere.version>
<testcontainers.version>1.19.8</testcontainers.version>
```

Then add these entries inside `<dependencyManagement><dependencies>`:

```xml
<!-- ShardingSphere -->
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>
    <version>${shardingsphere.version}</version>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-bom</artifactId>
    <version>${testcontainers.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

Place the ShardingSphere entry near other managed libraries. Place the Testcontainers BOM with the other imported BOMs.

- [ ] **Step 2: Add dependencies in `relation-service/pom.xml`**

Add this after the JPA dependency:

```xml
<!-- ShardingSphere-JDBC for following/followers table sharding -->
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>
</dependency>
```

Add these test dependencies before `</dependencies>`:

```xml
<!-- Tests -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Run dependency resolution**

Run:

```bash
cd G:/claude-project/social-sharding-platform
mvn -pl relation-service -am dependency:tree -Dincludes=org.apache.shardingsphere,org.testcontainers
```

Expected result:

- BUILD SUCCESS.
- Output includes `org.apache.shardingsphere:shardingsphere-jdbc-core-spring-boot-starter:5.5.0`.
- Output includes Testcontainers dependencies for `junit-jupiter` and `mysql`.

If ShardingSphere 5.5.0 cannot resolve from Maven Central, change `<shardingsphere.version>` to `5.4.1` and rerun the command. Do not continue until dependency resolution succeeds.

- [ ] **Step 4: Run test again to verify it now fails because ShardingSphere config is not present**

Run:

```bash
cd G:/claude-project/social-sharding-platform
mvn test -pl relation-service -Dtest=RelationShardingIntegrationTest
```

Expected result:

- Compilation succeeds.
- Test context fails to start because ShardingSphere datasource configuration is incomplete or missing.
- This is still an expected RED state.

- [ ] **Step 5: Commit dependency changes**

Run:

```bash
git add pom.xml relation-service/pom.xml
git commit -m "build: add relation sharding dependencies"
```

If the user does not want intermediate commits, skip the commit and keep changes for a final commit.

---

## Task 3: Configure ShardingSphere for relation-service

**Files:**
- Modify: `relation-service/src/main/resources/application.yml:4-48`

- [ ] **Step 1: Replace direct datasource config with ShardingSphere datasource config**

In `relation-service/src/main/resources/application.yml`, keep `server`, `spring.application`, `spring.data.redis`, `spring.main`, Dubbo, JWT, logging, and service sections. Replace the existing `spring.datasource` section with this ShardingSphere section under `spring`:

```yaml
spring:
  application:
    name: relation-service
  shardingsphere:
    datasource:
      names: ds0
      ds0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/social_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: root
        password: 123456
    rules:
      sharding:
        tables:
          following:
            actual-data-nodes: ds0.following_$->{0..15}
            table-strategy:
              standard:
                sharding-column: follower_id
                sharding-algorithm-name: following-inline
          followers:
            actual-data-nodes: ds0.followers_$->{0..15}
            table-strategy:
              standard:
                sharding-column: following_id
                sharding-algorithm-name: followers-inline
        sharding-algorithms:
          following-inline:
            type: INLINE
            props:
              algorithm-expression: following_$->{follower_id % 16}
          followers-inline:
            type: INLINE
            props:
              algorithm-expression: followers_$->{following_id % 16}
    props:
      sql-show: true
  data:
    redis:
      host: localhost
      port: 6379
      password: root
      database: 0
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  main:
    allow-bean-definition-overriding: true
```

Remove the separate top-level `jpa:` block currently present at lines 40-47. JPA config must live under `spring.jpa` to avoid split configuration.

- [ ] **Step 2: Make datasource URL override work in Docker**

The existing Docker script passes:

```bash
-Dspring.datasource.url=jdbc:mysql://host.docker.internal:3306/social_platform
```

That no longer overrides ShardingSphere's real datasource. Update the relation-service run configuration later in Task 5 to pass:

```bash
-Dspring.shardingsphere.datasource.ds0.jdbc-url=jdbc:mysql://host.docker.internal:3306/social_platform
-Dspring.shardingsphere.datasource.ds0.username=root
-Dspring.shardingsphere.datasource.ds0.password=123456
```

Do not edit Docker scripts in this task. This step is a reminder for Task 5.

- [ ] **Step 3: Run integration test**

Run:

```bash
cd G:/claude-project/social-sharding-platform
mvn test -pl relation-service -Dtest=RelationShardingIntegrationTest
```

Expected result:

- Test context starts.
- ShardingSphere SQL logs show routing to physical tables.
- Tests pass.

If tests fail with a ShardingSphere property binding error, compare the exact property name in the error with the YAML above. Do not change business code until the datasource starts.

- [ ] **Step 4: Commit ShardingSphere configuration**

Run:

```bash
git add relation-service/src/main/resources/application.yml
git commit -m "feat: configure relation table sharding"
```

If the user does not want intermediate commits, skip the commit.

---

## Task 4: Add a focused safeguard test for repository queries with shard keys

**Files:**
- Modify: `relation-service/src/test/java/com/social/relation/sharding/RelationShardingIntegrationTest.java`

- [ ] **Step 1: Add query behavior tests**

Append these tests inside `RelationShardingIntegrationTest`:

```java
@Test
void getFollowingListPagedReadsOnlyFollowerShard() {
    long followerId = 7L;
    long followingId = 8L;
    jdbcTemplate.update("""
            INSERT INTO following_7 (follower_id, following_id, created_at, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, followerId, followingId);

    var page = relationService.getFollowingListPaged(followerId, 1, 10);

    assertThat(page.getRecords()).hasSize(1);
    assertThat(page.getRecords().get(0).getUserId()).isEqualTo(followingId);
}

@Test
void getFollowersListPagedReadsOnlyFollowingShard() {
    long followerId = 7L;
    long followingId = 8L;
    jdbcTemplate.update("""
            INSERT INTO followers_8 (follower_id, following_id, created_at, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, followerId, followingId);

    var page = relationService.getFollowersListPaged(followingId, 1, 10);

    assertThat(page.getRecords()).hasSize(1);
    assertThat(page.getRecords().get(0).getUserId()).isEqualTo(followerId);
}
```

- [ ] **Step 2: Run test to verify it passes**

Run:

```bash
cd G:/claude-project/social-sharding-platform
mvn test -pl relation-service -Dtest=RelationShardingIntegrationTest
```

Expected result:

- BUILD SUCCESS.
- All tests in `RelationShardingIntegrationTest` pass.

- [ ] **Step 3: Commit query safeguard tests**

Run:

```bash
git add relation-service/src/test/java/com/social/relation/sharding/RelationShardingIntegrationTest.java
git commit -m "test: verify relation shard queries"
```

If the user does not want intermediate commits, skip the commit.

---

## Task 5: Update Docker run script for ShardingSphere datasource overrides

**Files:**
- Modify: `scripts/docker-run.sh:84-96`

- [ ] **Step 1: Add ShardingSphere datasource JVM overrides for relation-service only**

In `scripts/docker-run.sh`, inside `start_service()`, after the existing `JAVA_OPTS_VALUE` assignments, add this conditional block before `DOCKER_RUN_CMD="$DOCKER_RUN_CMD -e JAVA_OPTS=..."`:

```bash
    if [ "$SERVICE" = "relation-service" ]; then
        JAVA_OPTS_VALUE="$JAVA_OPTS_VALUE -Dspring.shardingsphere.datasource.ds0.jdbc-url=jdbc:mysql://host.docker.internal:3306/social_platform"
        JAVA_OPTS_VALUE="$JAVA_OPTS_VALUE -Dspring.shardingsphere.datasource.ds0.username=root"
        JAVA_OPTS_VALUE="$JAVA_OPTS_VALUE -Dspring.shardingsphere.datasource.ds0.password=123456"
    fi
```

Also add relation-service-specific environment variables after the existing datasource environment variable lines:

```bash
    if [ "$SERVICE" = "relation-service" ]; then
        DOCKER_RUN_CMD="$DOCKER_RUN_CMD -e SPRING_SHARDINGSPHERE_DATASOURCE_DS0_JDBC-URL=jdbc:mysql://host.docker.internal:3306/social_platform"
        DOCKER_RUN_CMD="$DOCKER_RUN_CMD -e SPRING_SHARDINGSPHERE_DATASOURCE_DS0_USERNAME=root"
        DOCKER_RUN_CMD="$DOCKER_RUN_CMD -e SPRING_SHARDINGSPHERE_DATASOURCE_DS0_PASSWORD=123456"
    fi
```

Keep the existing `SPRING_DATASOURCE_URL` values because other services still use them.

- [ ] **Step 2: Run shell syntax check**

Run:

```bash
cd G:/claude-project/social-sharding-platform
bash -n scripts/docker-run.sh
```

Expected result:

- No output.
- Exit code 0.

- [ ] **Step 3: Commit Docker override update**

Run:

```bash
git add scripts/docker-run.sh
git commit -m "chore: add relation sharding docker datasource overrides"
```

If the user does not want intermediate commits, skip the commit.

---

## Task 6: Build and run relation-service with ShardingSphere

**Files:**
- No source edits expected.
- Runtime verification uses Docker and MySQL.

- [ ] **Step 1: Package relation-service**

Run:

```bash
cd G:/claude-project/social-sharding-platform
export MAVEN_OPTS="-Xmx512m"
mvn clean package -pl relation-service -am -DskipTests
```

Expected result:

- BUILD SUCCESS.
- `relation-service/target/relation-service-1.0.0-SNAPSHOT.jar` exists.

- [ ] **Step 2: Build relation-service Docker image**

Run:

```bash
cd G:/claude-project/social-sharding-platform
docker build -f docker/Dockerfile.relation-service \
  -t swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-relation-service:latest \
  relation-service
```

Expected result:

- Docker build succeeds.
- Image tag exists locally.

- [ ] **Step 3: Restart relation-service container using script**

Run:

```bash
cd G:/claude-project/social-sharding-platform
bash scripts/docker-run.sh stop
bash scripts/docker-run.sh start
```

If restarting all services is too disruptive, restart only relation-service manually using the same options as the script plus the ShardingSphere datasource overrides from Task 5.

Expected result:

- `relation-service` container stays `Up` for at least 30 seconds.
- Logs include Spring Boot Started message.
- Logs do not include `ShardingSphereAutoConfiguration` bean creation failure.

- [ ] **Step 4: Verify Nacos/Dubbo registration still works**

Run:

```bash
curl -s "http://localhost:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=50" | grep -i RelationService
```

Expected result:

- Output includes `providers:com.social.common.api.RelationService:1.0.0:` or the service appears in Nacos UI.

---

## Task 7: Manual database verification of real shard writes

**Files:**
- No source edits expected.

- [ ] **Step 1: Prepare clean test rows**

Run in MySQL:

```sql
DELETE FROM following_7 WHERE follower_id = 7 AND following_id = 8;
DELETE FROM followers_8 WHERE follower_id = 7 AND following_id = 8;
DELETE FROM following_0 WHERE follower_id = 7 AND following_id = 8;
DELETE FROM followers_0 WHERE follower_id = 7 AND following_id = 8;
DELETE FROM user_relation_count WHERE user_id IN (7, 8);
```

Expected result:

- Rows are deleted or zero rows affected.

- [ ] **Step 2: Trigger follow through the application**

Use the existing API flow. Example if authenticated token is available:

```bash
TOKEN="<JWT for user 7>"
curl -i -X POST "http://localhost:8080/api/relations/8/follow" \
  -H "Authorization: Bearer $TOKEN"
```

If the route differs, inspect `facade-service/src/main/java/com/social/facade/controller/RelationFacadeController.java` and use the actual follow endpoint.

Expected result:

- HTTP status 200.
- Response code indicates success.

- [ ] **Step 3: Verify correct shard rows exist**

Run in MySQL:

```sql
SELECT COUNT(*) AS c FROM following_7 WHERE follower_id = 7 AND following_id = 8;
SELECT COUNT(*) AS c FROM followers_8 WHERE follower_id = 7 AND following_id = 8;
SELECT COUNT(*) AS c FROM following_0 WHERE follower_id = 7 AND following_id = 8;
SELECT COUNT(*) AS c FROM followers_0 WHERE follower_id = 7 AND following_id = 8;
```

Expected result:

- `following_7` count is 1.
- `followers_8` count is 1.
- `following_0` count is 0.
- `followers_0` count is 0.

- [ ] **Step 4: Trigger unfollow through the application**

Run:

```bash
TOKEN="<JWT for user 7>"
curl -i -X DELETE "http://localhost:8080/api/relations/8/follow" \
  -H "Authorization: Bearer $TOKEN"
```

If the route differs, inspect `RelationFacadeController` and use the actual unfollow endpoint.

Expected result:

- HTTP status 200.
- Response code indicates success.

- [ ] **Step 5: Verify correct shard rows are deleted**

Run in MySQL:

```sql
SELECT COUNT(*) AS c FROM following_7 WHERE follower_id = 7 AND following_id = 8;
SELECT COUNT(*) AS c FROM followers_8 WHERE follower_id = 7 AND following_id = 8;
```

Expected result:

- Both counts are 0.

---

## Task 8: Final regression checks and documentation update

**Files:**
- Modify if needed: `docs/regression-tests.md`
- Modify if needed: `CLAUDE.md`

- [ ] **Step 1: Run targeted tests**

Run:

```bash
cd G:/claude-project/social-sharding-platform
export MAVEN_OPTS="-Xmx512m"
mvn test -pl relation-service -Dtest=RelationShardingIntegrationTest
```

Expected result:

- BUILD SUCCESS.

- [ ] **Step 2: Run package for all modules**

Run:

```bash
cd G:/claude-project/social-sharding-platform
export MAVEN_OPTS="-Xmx512m"
mvn clean package -DskipTests
```

Expected result:

- BUILD SUCCESS.

- [ ] **Step 3: Update regression documentation**

Append this section to `docs/regression-tests.md`:

```markdown
## Relation Sharding Regression Baseline

- relation-service starts with ShardingSphere-JDBC enabled.
- `following` logical table routes by `follower_id % 16` to `following_0~15`.
- `followers` logical table routes by `following_id % 16` to `followers_0~15`.
- Follow test `(followerId=7, followingId=8)` writes to `following_7` and `followers_8`.
- Unfollow test deletes from `following_7` and `followers_8`.
- Legacy single tables `following` and `followers` are not migrated and are not used for new relation writes.
```

- [ ] **Step 4: Update CLAUDE.md if implementation details changed from existing guidance**

If the implementation matches the existing CLAUDE.md sharding strategy exactly, no CLAUDE.md update is required.

If you changed any operational command or Docker override, add a short note under Docker or Sharding Strategy explaining relation-service now uses ShardingSphere datasource override keys:

```markdown
Relation-service ShardingSphere Docker overrides:
- `spring.shardingsphere.datasource.ds0.jdbc-url`
- `spring.shardingsphere.datasource.ds0.username`
- `spring.shardingsphere.datasource.ds0.password`
```

- [ ] **Step 5: Final git status review**

Run:

```bash
cd G:/claude-project/social-sharding-platform
git status --short
```

Expected result:

- Only intended files changed:
  - `pom.xml`
  - `relation-service/pom.xml`
  - `relation-service/src/main/resources/application.yml`
  - `relation-service/src/test/java/com/social/relation/sharding/RelationShardingIntegrationTest.java`
  - `scripts/docker-run.sh`
  - `docs/regression-tests.md` if updated
  - `CLAUDE.md` only if operational guidance changed

- [ ] **Step 6: Final commit**

If intermediate commits were skipped, run:

```bash
git add pom.xml relation-service/pom.xml relation-service/src/main/resources/application.yml relation-service/src/test/java/com/social/relation/sharding/RelationShardingIntegrationTest.java scripts/docker-run.sh docs/regression-tests.md CLAUDE.md
git commit -m "feat: enable relation table sharding"
```

If intermediate commits were already made, create no additional commit unless documentation changed.

---

## Self-Review

### Spec coverage

- ShardingSphere-JDBC implementation: covered by Tasks 2 and 3.
- Single-database table sharding: covered by Task 3 config.
- `following` by `follower_id % 16`: covered by Task 3 and Task 1 tests.
- `followers` by `following_id % 16`: covered by Task 3 and Task 1 tests.
- No migration of old single-table data: documented in plan and not implemented.
- Verify write/delete/query behavior: covered by Tasks 1, 4, and 7.
- Docker operation: covered by Task 5 and Task 6.
- Regression documentation: covered by Task 8.

### Placeholder scan

No TBD/TODO placeholders remain. Steps include exact paths, code, commands, and expected results.

### Type consistency

- Test imports use Spring/JUnit/Testcontainers classes introduced in Task 2.
- Sharding keys match existing schema names: `follower_id`, `following_id`.
- Logical table names match entities: `following`, `followers`.
- Physical table names match existing schema: `following_0~15`, `followers_0~15`.
