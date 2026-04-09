# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Social Sharing Platform - a microservices-based social media backend with posts, comments, likes, follows, and real-time notifications.

**Tech Stack:** Spring Boot 3.2 + Dubbo 3.2 + Spring Cloud 2023.0 + Nacos 2.2.3 + MySQL + Redis + JWT

## Build Commands

```bash
# Build all modules
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Build a specific module
mvn clean package -pl user-service -am

# Run a specific service (from module directory)
mvn spring-boot:run

# Apply database schema
mysql -u root -p < sql/schema.sql
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
nacos-embedded/      # Embedded Nacos for local dev (if present)
```

### Common Utilities

- `JwtUtil`: Token generation/parsing (jjwt library)
- `Result<T>`: Unified API response wrapper
- `BusinessException` + `ErrorCode`: Exception handling
- `RedisKeys`: Redis key prefix constants

## OpenSpec Workflow

This project uses OpenSpec for change management in `openspec/changes/`:

- `/opsx:propose <name>` - Create new change proposal with proposal.md, design.md, tasks.md
- `/opsx:apply <name>` - Implement tasks from tasks.md
- `/opsx:archive <name>` - Mark change as complete

Current active change: `social-sharing-platform` (partially implemented - tasks 1-9 complete, task 10 testing remaining).

## Important Constraints

- Images: max 9 per post, served from `/files/images/{uuid}.{ext}`
- Videos: max 50MB per post, served from `/files/videos/{uuid}.{ext}`
- Images and videos cannot be mixed in the same post
- JWT secret and expiration configured per service in application.yml
