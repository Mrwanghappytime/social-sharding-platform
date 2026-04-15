# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Social Sharing Platform - a microservices-based social media backend with posts, comments, likes, follows, and real-time notifications.

**Tech Stack:** Spring Boot 3.2 + Dubbo 3.2 + Spring Cloud 2023.0 + Nacos 2.2.3 + MySQL + Redis + JWT + Docker

## Build Commands

```bash
# Build all modules
mvn clean package

# Build without tests (fastest for development)
mvn clean package -DskipTests

# Build specific module with dependencies
mvn clean package -pl user-service -am

# Run a specific service locally
mvn spring-boot:run
```

## Architecture

### Services (Dubbo RPC via Nacos)

| Service | Port | Responsibility |
|---------|------|----------------|
| gateway | 8080 | JWT auth, WebSocket, routing |
| user-service | 8081 | User registration, login, profile |
| post-service | 8082 | Posts CRUD, media, search |
| interaction-service | 8083 | Likes, comments |
| relation-service | 8084 | Follow/unfollow (16 sharded tables) |
| notification-service | 8085 | Real-time notifications (WebSocket + Redis Pub/Sub) |
| file-service | 8086 | File upload, static file serving |
| facade-service | 8087 | API facade, aggregation layer |

### Key Patterns

**Service Communication:** Dubbo RPC with Nacos service discovery. Interfaces defined in `common/src/main/java/com/social/common/api/`.

**Sharding Strategy:**
- `following_0~15`: Sharded by `follower_id % 16` (who I follow)
- `followers_0~15`: Sharded by `following_id % 16` (my followers)
- `notifications_0~15`: Sharded by `recipient_id % 16`

**KOL Notification Model:** Users with >= 10,000 followers don't receive real-time WebSocket pushes; they poll on login instead.

**File Storage:** Local disk at `/data/files/{type}/{uuid}.{ext}` served directly by file-service.

### Module Structure

```
common/              # Shared entities, DTOs, enums, API interfaces, utilities (JWT)
gateway/             # Entry point: JWT filter, WebSocket, Dubbo routing
user-service/        # User management with MyBatis Plus
post-service/        # Posts with FULLTEXT search on title
interaction-service/ # Likes (unique per user+post), comments
relation-service/    # Follow/unfollow with sharding util (TableShardingUtil)
notification-service/ # Redis Pub/Sub subscriber, WebSocket pusher
file-service/        # Multipart file upload, static resource serving
facade-service/      # API facade layer
```

## Docker Deployment

### Docker Build and Run

```bash
# Build all Docker images
./scripts/docker-build.sh

# Stop all containers
./scripts/docker-run.sh stop

# Start all containers
./scripts/docker-run.sh start

# Full rebuild and restart
mvn clean package -DskipTests && ./scripts/docker-build.sh && ./scripts/docker-run.sh stop && ./scripts/docker-run.sh start
```

### Windows Docker Desktop Networking

When running Docker on Windows with Docker Desktop, use these patterns:

1. **Host access**: Use `--add-host=host.docker.internal:host-gateway` to access Windows host services
2. **Port mapping**: Always use `-p HOST_PORT:CONTAINER_PORT` (bridge network, not `--network host`)
3. **Volume mounts**: Use `g:/path` format (Unix-style path on G drive), NOT `/g/path`

Example Docker run command:
```bash
docker run -d --name gateway -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e JAVA_OPTS="-Dspring.cloud.nacos.discovery.server-addr=host.docker.internal:8848 ..." \
  -v g:/logs/gateway:/app/logs \
  image-name
```

### Java System Properties for Docker

Override YAML config via JAVA_OPTS:
```bash
-Dspring.cloud.nacos.discovery.server-addr=host.docker.internal:8848
-Dspring.cloud.nacos.config.server-addr=host.docker.internal:8848
-Dspring.datasource.url=jdbc:mysql://host.docker.internal:3306/social_platform
-Dspring.data.redis.host=host.docker.internal
-Dlogging.file.path=/app/logs
-Ddubbo.registry.address=nacos://host.docker.internal:8848
```

## Logging Configuration

### Logback Pattern (CRITICAL)

**Use `%caller{1}` instead of `%logger{36}.%method:%line`**

The pattern `%logger{36}.%method:%line` causes parsing issues where `%n` newline is not recognized after the logger pattern. Use `%caller{1}` which outputs caller location info with proper newline handling.

**Correct pattern:**
```xml
<property name="LOG_PATTERN" value="[${SERVICE_NAME}[${SERVICE_IP}:${SERVER_PORT}]][%d{HH:mm:ss.SSS}][%X{traceId:-}][%t][%-5level] %msg%nopex%caller{1}"/>
```

Note: `%caller{1}` already includes a newline, so do NOT add extra `%n` after it (would create double newlines).

### Logger Levels

- Default/dev profile: `com.social` logger at `INFO` level
- Do NOT use `DEBUG` in production (causes log explosion)

### Log Files

- Location: `/app/logs/{service-name}.log` (mounted to host at `g:/logs/{service-name}/`)
- Rotation: 30MB max per file, 500MB total, 17 days retention
- Error logs: `{service-name}-error.log`

## Common Utilities

- `JwtUtil`: Token generation/parsing (jjwt library)
- `Result<T>`: Unified API response wrapper
- `BusinessException` + `ErrorCode`: Exception handling
- `RedisKeys`: Redis key prefix constants
- `LogUtil`: TraceId generation/management for Dubbo RPC

## Important Constraints

### Development Workflow

**Test-First Development:**
1. Before implementing, clearly understand and define specific test targets/acceptance criteria
2. Implement code to meet the test targets
3. Verify all test targets are met
4. Ensure existing test baseline is not broken (all original tests still pass)
5. Document the new test results as the updated test baseline for future regression comparison
6. Only then consider the task complete

**No Guessing Without Evidence:**
- Never modify code based on猜测 without clear error messages or code analysis
- If insufficient information to locate problem, state: "我没有足够信息，需要你提供XXX才能继续"
- Propose hypotheses for user verification, do not modify code before confirmation

### Functional Constraints

- Images: max 9 per post, served from `/files/images/{uuid}.{ext}`
- Videos: max 50MB per post, served from `/files/videos/{uuid}.{ext}`
- Images and videos cannot be mixed in the same post
- JWT secret and expiration configured per service in application.yml

## OpenSpec Workflow

This project uses OpenSpec for change management in `openspec/changes/`:

- `/opsx:propose <name>` - Create new change proposal with proposal.md, design.md, tasks.md
- `/opsx:apply <name>` - Implement tasks from tasks.md
- `/opsx:archive <name>` - Mark change as complete

## Lessons Learned (Development History)

### Docker Lessons

1. **Dockerfile context matters**: If `.dockerignore` is in the wrong directory, JAR files won't be found. Use service directory as build context: `docker build -f "$DOCKERFILE" -t "$IMAGE_NAME" "${SERVICE}"`

2. **Windows path issues**: Git Bash interprets `/g/path` as Windows path `C:\Program Files\Git\path`. Use `g:/path` format for Docker volume mounts.

3. **Image cleanup**: Use `docker image prune -f` and `docker builder prune -f` to reclaim disk space after rebuilds.

4. **Network instability**: Git push may fail on Windows Docker Desktop due to network issues. Implement retry logic for critical operations.

### Logback Lessons

1. **Pattern parsing bug**: `%logger{36}.%method:%line` breaks `%n` newline handling. The dot and colon in the pattern confuse Logback's pattern parser. Use `%caller{1}` as a workaround.

2. **Log file line endings**: Always verify with `cat -A` or hex dump (`xxd`) to check actual line endings in log files.

3. **Log level change**: Changing log level requires rebuild because config is baked into JAR at build time.

4. **AsyncAppender buffering**: Async appender buffers logs, so logs may not appear immediately in files. Use sync FILE appender for debugging.

### Microservices Lessons

1. **TraceId propagation**: Implement DubboTraceFilter to propagate traceId across service calls for distributed tracing.

2. **Sharding consistency**: When sharding tables, ensure hash key is consistent (e.g., always use `user_id % 16`, not different keys for different operations).

3. **Redis dependency**: Services need Redis running. If Redis is down, notification-service will fail. Consider graceful degradation.

### Testing Lessons

1. **Regression testing**: After Docker deployment changes, create regression test documentation to verify all services work correctly together.

2. **API testing**: Use `curl` or Postman to manually test critical API endpoints after deployment changes.

3. **Log verification**: Check log files to confirm services started correctly and are processing requests.

4. **Test baseline**: Document test results after each milestone. This baseline serves as the reference for future regression testing. When new changes break the baseline, it's a signal to investigate before proceeding.

### Code Review Checklist

Before committing:
- [ ] No debug logs in production code
- [ ] Logback pattern uses `%caller{1}` not `%logger{...}.%method:%line`
- [ ] Logger level is INFO not DEBUG for `com.social` package
- [ ] Docker changes include proper network configuration
- [ ] Environment-specific configs don't contain secrets
- [ ] Test targets defined and verified
- [ ] Existing tests still pass (test baseline not broken)
- [ ] New test results documented as updated test baseline
