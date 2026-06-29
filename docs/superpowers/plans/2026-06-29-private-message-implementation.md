# Private Message Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one-to-one private messaging with text/image messages, reusable conversation WebSocket, and conversation-backed `MESSAGE` notifications in the existing notification page.

**Architecture:** Add an independent `message-service` that owns conversations, messages, unread counts, and private-message WebSocket delivery. Keep notification reminders in `notification-service` as one reusable `MESSAGE` notification per recipient/conversation, and let `facade-service` aggregate users, post titles, and conversation summaries for frontend pages.

**Tech Stack:** Spring Boot 3.2, Dubbo 3.2, Spring Cloud Gateway, Nacos, MySQL/JPA, Redis Pub/Sub, Vue 3, Pinia, Element Plus, existing file-service upload API.

---

## File Structure

### Backend common module

- Create `common/src/main/java/com/social/common/enums/MessageType.java`
  - Defines `TEXT` and `IMAGE`.
- Modify `common/src/main/java/com/social/common/enums/NotificationType.java`
  - Adds `MESSAGE`.
- Create `common/src/main/java/com/social/common/entity/Conversation.java`
  - JPA entity for one-to-one conversation metadata.
- Create `common/src/main/java/com/social/common/entity/Message.java`
  - JPA entity for private messages.
- Create `common/src/main/java/com/social/common/repository/ConversationRepository.java`
  - Conversation lookup by normalized users and by participant.
- Create `common/src/main/java/com/social/common/repository/MessageRepository.java`
  - Message pagination and unread queries.
- Modify `common/src/main/java/com/social/common/repository/NotificationRepository.java`
  - Adds conversation notification lookup.
- Create `common/src/main/java/com/social/common/api/MessageService.java`
  - Dubbo API for message-service.
- Modify `common/src/main/java/com/social/common/api/NotificationService.java`
  - Adds conversation notification upsert/read methods.
- Create `common/src/main/java/com/social/common/dto/ConversationDTO.java`
- Create `common/src/main/java/com/social/common/dto/MessageDTO.java`
- Create `common/src/main/java/com/social/common/dto/CreateConversationRequest.java`
- Create `common/src/main/java/com/social/common/dto/SendTextMessageRequest.java`
- Create `common/src/main/java/com/social/common/dto/SendImageMessageRequest.java`

### SQL and deployment

- Modify `sql/schema.sql`
  - Adds `conversations` and `messages` tables.
  - Adds migration block for `NotificationType.MESSAGE` usage by code only; no enum DDL required because notifications store strings.
- Modify root `pom.xml`
  - Adds `message-service` module.
- Create `message-service/pom.xml`
- Create `message-service/src/main/resources/application.yml`
- Create `message-service/src/main/resources/logback-spring.xml`
- Create `docker/Dockerfile.message-service`
- Modify `scripts/docker-build.sh`
  - Adds `message-service` to `SERVICES`.
- Modify `scripts/docker-run.sh`
  - Adds `message-service` port `8088`.
- Modify `gateway/src/main/resources/application.yml`
  - Adds `/ws/message` route.

### message-service module

- Create `message-service/src/main/java/com/social/message/MessageServiceApplication.java`
- Create `message-service/src/main/java/com/social/message/service/MessageServiceImpl.java`
- Create `message-service/src/main/java/com/social/message/config/RedisConfig.java`
- Create `message-service/src/main/java/com/social/message/websocket/MessageWebSocketConfig.java`
- Create `message-service/src/main/java/com/social/message/websocket/MessageWebSocketHandler.java`

### notification-service module

- Modify `notification-service/src/main/java/com/social/notification/service/NotificationServiceImpl.java`
  - Adds `upsertConversationNotification`.
  - Adds `markConversationNotificationAsRead`.
  - Sorts list by `updatedAt DESC`.

### facade-service module

- Create `facade-service/src/main/java/com/social/facade/controller/MessageFacadeController.java`
- Create `facade-service/src/main/java/com/social/facade/dto/ConversationFacadeResponse.java`
- Create `facade-service/src/main/java/com/social/facade/dto/MessageFacadeResponse.java`
- Modify `facade-service/src/main/java/com/social/facade/dto/NotificationFacadeResponse.java`
  - Adds `conversation` field.
- Modify `facade-service/src/main/java/com/social/facade/controller/NotificationFacadeController.java`
  - Aggregates `CONVERSATION` target summaries from `message-service`.

### frontend

- Create `frontend/src/api/message.ts`
- Create `frontend/src/stores/message.ts`
- Create `frontend/src/stores/messageSocket.ts`
- Create `frontend/src/views/message/ChatDetailPage.vue`
- Modify `frontend/src/router/index.ts`
  - Adds chat route.
- Modify `frontend/src/views/user/UserProfilePage.vue`
  - Adds private-message button.
- Modify `frontend/src/api/notification.ts`
  - Adds `MESSAGE` notification conversation fields.
- Modify `frontend/src/stores/notification.ts`
  - Adds `MESSAGE` handling text.
- Modify `frontend/src/views/notification/NotificationPage.vue`
  - Routes `MESSAGE + CONVERSATION` to chat.
- Modify `frontend/src/components/notification/NotificationItem.vue`
  - Displays private-message summary and unread badge.
- Modify `frontend/src/types/index.ts`
  - Adds conversation/message types.
- Modify `frontend/src/api/upload.ts`
  - Reuses current upload function for compressed/original image upload; no endpoint change.

---

## Task 1: Common domain model and SQL schema

**Files:**
- Create: `common/src/main/java/com/social/common/enums/MessageType.java`
- Modify: `common/src/main/java/com/social/common/enums/NotificationType.java`
- Create: `common/src/main/java/com/social/common/entity/Conversation.java`
- Create: `common/src/main/java/com/social/common/entity/Message.java`
- Create: `common/src/main/java/com/social/common/repository/ConversationRepository.java`
- Create: `common/src/main/java/com/social/common/repository/MessageRepository.java`
- Modify: `common/src/main/java/com/social/common/repository/NotificationRepository.java`
- Create: `common/src/main/java/com/social/common/dto/ConversationDTO.java`
- Create: `common/src/main/java/com/social/common/dto/MessageDTO.java`
- Create: `common/src/main/java/com/social/common/dto/CreateConversationRequest.java`
- Create: `common/src/main/java/com/social/common/dto/SendTextMessageRequest.java`
- Create: `common/src/main/java/com/social/common/dto/SendImageMessageRequest.java`
- Modify: `sql/schema.sql`

- [ ] **Step 1: Create message type enum**

Create `common/src/main/java/com/social/common/enums/MessageType.java`:

```java
package com.social.common.enums;

public enum MessageType {
    TEXT,
    IMAGE
}
```

- [ ] **Step 2: Add MESSAGE notification type**

Modify `common/src/main/java/com/social/common/enums/NotificationType.java` to exactly:

```java
package com.social.common.enums;

public enum NotificationType {
    COMMENT,   // 评论
    LIKE,      // 点赞
    FOLLOW,    // 关注
    MESSAGE    // 私聊
}
```

- [ ] **Step 3: Create Conversation entity**

Create `common/src/main/java/com/social/common/entity/Conversation.java`:

```java
package com.social.common.entity;

import com.social.common.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversation_users", columnNames = {"user1_id", "user2_id"}),
        indexes = {
                @Index(name = "idx_user1_updated", columnList = "user1_id, updated_at"),
                @Index(name = "idx_user2_updated", columnList = "user2_id, updated_at")
        }
)
public class Conversation extends BaseEntity {

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_message_type", length = 20)
    private MessageType lastMessageType;

    @Column(name = "last_message_preview", length = 255)
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
}
```

- [ ] **Step 4: Create Message entity**

Create `common/src/main/java/com/social/common/entity/Message.java`:

```java
package com.social.common.entity;

import com.social.common.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_conversation_created", columnList = "conversation_id, created_at"),
                @Index(name = "idx_receiver_read", columnList = "receiver_id, is_read"),
                @Index(name = "idx_sender_receiver_created", columnList = "sender_id, receiver_id, created_at")
        }
)
public class Message extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(name = "content", length = 2000)
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "original_image_url", length = 500)
    private String originalImageUrl;

    @Column(name = "is_read")
    private Boolean isRead = false;
}
```

- [ ] **Step 5: Create ConversationRepository**

Create `common/src/main/java/com/social/common/repository/ConversationRepository.java`:

```java
package com.social.common.repository;

import com.social.common.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    @Query("SELECT c FROM Conversation c WHERE c.user1Id = :userId OR c.user2Id = :userId ORDER BY c.updatedAt DESC")
    Page<Conversation> findByParticipant(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.id IN :ids AND (c.user1Id = :userId OR c.user2Id = :userId)")
    List<Conversation> findByIdsAndParticipant(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
```

- [ ] **Step 6: Create MessageRepository**

Create `common/src/main/java/com/social/common/repository/MessageRepository.java`:

```java
package com.social.common.repository;

import com.social.common.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationId(Long conversationId, Pageable pageable);

    long countByConversationIdAndReceiverIdAndIsReadFalse(Long conversationId, Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.receiverId = :receiverId AND m.isRead = false")
    int markConversationAsRead(@Param("conversationId") Long conversationId, @Param("receiverId") Long receiverId);
}
```

- [ ] **Step 7: Extend NotificationRepository**

Modify `common/src/main/java/com/social/common/repository/NotificationRepository.java` to import `NotificationType` and `Optional`, then add the method below before the closing brace:

```java
Optional<Notification> findByRecipientIdAndTypeAndTargetTypeAndTargetId(
        Long recipientId,
        NotificationType type,
        String targetType,
        Long targetId
);
```

The import block should include:

```java
import com.social.common.enums.NotificationType;
import java.util.Optional;
```

- [ ] **Step 8: Create DTOs and request classes**

Create `common/src/main/java/com/social/common/dto/ConversationDTO.java`:

```java
package com.social.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ConversationDTO implements Serializable {
    private Long id;
    private Long user1Id;
    private Long user2Id;
    private Long peerUserId;
    private Long lastMessageId;
    private String lastMessageType;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

Create `common/src/main/java/com/social/common/dto/MessageDTO.java`:

```java
package com.social.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MessageDTO implements Serializable {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String messageType;
    private String content;
    private String imageUrl;
    private String originalImageUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
```

Create `common/src/main/java/com/social/common/dto/CreateConversationRequest.java`:

```java
package com.social.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {
    @NotNull(message = "目标用户不能为空")
    private Long targetUserId;
}
```

Create `common/src/main/java/com/social/common/dto/SendTextMessageRequest.java`:

```java
package com.social.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendTextMessageRequest {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字")
    private String content;
}
```

Create `common/src/main/java/com/social/common/dto/SendImageMessageRequest.java`:

```java
package com.social.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendImageMessageRequest {
    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    private String originalImageUrl;
}
```

- [ ] **Step 9: Add SQL tables and indexes**

Append this migration block to `sql/schema.sql`:

```sql
-- =====================================================
-- Migration: Add private message conversations and messages
-- Date: 2026-06-29
-- Description: 私聊会话、私聊消息、MESSAGE 通知类型由应用枚举支持
-- =====================================================
CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user1_id BIGINT NOT NULL COMMENT '较小的用户ID',
    user2_id BIGINT NOT NULL COMMENT '较大的用户ID',
    last_message_id BIGINT NULL COMMENT '最后一条消息ID',
    last_message_type VARCHAR(20) NULL COMMENT '最后一条消息类型',
    last_message_preview VARCHAR(255) NULL COMMENT '最后一条消息摘要',
    last_message_at DATETIME NULL COMMENT '最后一条消息时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_users (user1_id, user2_id),
    KEY idx_user1_updated (user1_id, updated_at),
    KEY idx_user2_updated (user2_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊会话表';

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    message_type VARCHAR(20) NOT NULL COMMENT '消息类型：TEXT/IMAGE',
    content VARCHAR(2000) NULL COMMENT '文本消息内容',
    image_url VARCHAR(500) NULL COMMENT '压缩图片地址',
    original_image_url VARCHAR(500) NULL COMMENT '原图地址',
    is_read BOOLEAN DEFAULT FALSE COMMENT '接收者是否已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_conversation_created (conversation_id, created_at),
    KEY idx_receiver_read (receiver_id, is_read),
    KEY idx_sender_receiver_created (sender_id, receiver_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊消息表';
```

- [ ] **Step 10: Compile common**

Run:

```bash
mvn -q -DskipTests -pl common compile
```

Expected: command exits `0`.

- [ ] **Step 11: Commit common model changes**

Run:

```bash
git add common/src/main/java/com/social/common sql/schema.sql
git commit -m "feat: add private message common model"
```

---

## Task 2: MessageService API and message-service module skeleton

**Files:**
- Create: `common/src/main/java/com/social/common/api/MessageService.java`
- Modify: `pom.xml`
- Create: `message-service/pom.xml`
- Create: `message-service/src/main/java/com/social/message/MessageServiceApplication.java`
- Create: `message-service/src/main/resources/application.yml`
- Create: `message-service/src/main/resources/logback-spring.xml`
- Create: `message-service/src/main/java/com/social/message/config/RedisConfig.java`

- [ ] **Step 1: Create MessageService Dubbo API**

Create `common/src/main/java/com/social/common/api/MessageService.java`:

```java
package com.social.common.api;

import com.social.common.dto.ConversationDTO;
import com.social.common.dto.MessageDTO;
import com.social.common.dto.PageResult;

import java.util.List;

public interface MessageService {

    ConversationDTO getOrCreateConversation(Long currentUserId, Long targetUserId);

    MessageDTO sendTextMessage(Long senderId, Long conversationId, String content);

    MessageDTO sendImageMessage(Long senderId, Long conversationId, String imageUrl, String originalImageUrl);

    PageResult<MessageDTO> getMessages(Long conversationId, Long currentUserId, Integer page, Integer size);

    List<ConversationDTO> getConversationsByIds(List<Long> conversationIds, Long currentUserId);

    ConversationDTO getConversationById(Long conversationId, Long currentUserId);

    void markConversationAsRead(Long conversationId, Long currentUserId);

    Long getUnreadMessageCount(Long currentUserId);
}
```

- [ ] **Step 2: Add message-service module to root pom**

Modify root `pom.xml` `<modules>` to include `message-service` after `notification-service`:

```xml
<module>notification-service</module>
<module>message-service</module>
<module>file-service</module>
```

- [ ] **Step 3: Create message-service pom**

Create `message-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.social</groupId>
        <artifactId>social-sharding-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>message-service</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.social</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.nacos</groupId>
            <artifactId>nacos-client</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Create application class**

Create `message-service/src/main/java/com/social/message/MessageServiceApplication.java`:

```java
package com.social.message;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
@EntityScan("com.social.common.entity")
@EnableJpaRepositories("com.social.common.repository")
@ComponentScan(basePackages = {"com.social.message", "com.social.common"})
public class MessageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
```

- [ ] **Step 5: Create message-service application.yml**

Create `message-service/src/main/resources/application.yml` by matching existing service style:

```yaml
server:
  port: 8088

spring:
  application:
    name: message-service
  cloud:
    nacos:
      discovery:
        enabled: true
        server-addr: localhost:8848
        namespace: public
        group: DEFAULT_GROUP
  datasource:
    url: jdbc:mysql://localhost:3306/social_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      password: root
      database: 0
  main:
    allow-bean-definition-overriding: true
  jpa:
    open-in-view: false

dubbo:
  application:
    name: message-service
  protocol:
    name: dubbo
    port: 20888
  registry:
    address: nacos://localhost:8848
  scan:
    base-packages: com.social.message
  consumer:
    check: false
    filter: dubboTraceFilter
  provider:
    filter: dubboTraceFilter

jpa:
  hibernate:
    ddl-auto: update
  show-sql: true
  properties:
    hibernate:
      format_sql: true
      dialect: org.hibernate.dialect.MySQLDialect

jwt:
  secret: social-sharing-platform-secret-key-change-in-production

logging:
  config: classpath:logback-spring.xml
  level:
    root: INFO
    com.social.message: INFO
  log-path: ${LOG_PATH:logs}

service:
  name: message-service
  ip: ${HOST_IP:127.0.0.1}
  port: 8088
```

- [ ] **Step 6: Create logback config**

Copy the current corrected logback style from another service and create `message-service/src/main/resources/logback-spring.xml` with `SERVICE_NAME` defaulting to `message-service` and `%caller{1}` in `LOG_PATTERN`.

Use this pattern line:

```xml
<property name="LOG_PATTERN" value="[${SERVICE_NAME}[${SERVICE_IP}:${SERVER_PORT}]][%d{HH:mm:ss.SSS}][%X{traceId:-}][%t][%-5level] %msg%nopex%caller{1}"/>
```

- [ ] **Step 7: Create RedisConfig**

Create `message-service/src/main/java/com/social/message/config/RedisConfig.java`:

```java
package com.social.message.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
```

- [ ] **Step 8: Compile message-service skeleton**

Run:

```bash
mvn -q -DskipTests -pl common,message-service -am compile
```

Expected: command exits `0`.

- [ ] **Step 9: Commit skeleton**

Run:

```bash
git add pom.xml common/src/main/java/com/social/common/api/MessageService.java message-service
git commit -m "feat: add message service module"
```

---

## Task 3: Implement message-service domain logic

**Files:**
- Create: `message-service/src/main/java/com/social/message/service/MessageServiceImpl.java`

- [ ] **Step 1: Create MessageServiceImpl with conversation normalization**

Create `message-service/src/main/java/com/social/message/service/MessageServiceImpl.java`:

```java
package com.social.message.service;

import com.social.common.api.MessageService;
import com.social.common.dto.ConversationDTO;
import com.social.common.dto.MessageDTO;
import com.social.common.dto.PageResult;
import com.social.common.entity.Conversation;
import com.social.common.entity.Message;
import com.social.common.enums.MessageType;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.ConversationRepository;
import com.social.common.repository.MessageRepository;
import com.social.common.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@DubboService(interfaceClass = MessageService.class, version = "1.0.0")
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String MESSAGE_CHANNEL_PREFIX = "message:conversation:";

    @Override
    @Transactional
    public ConversationDTO getOrCreateConversation(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户不能为空");
        }
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能给自己发私信");
        }

        long user1Id = Math.min(currentUserId, targetUserId);
        long user2Id = Math.max(currentUserId, targetUserId);

        return conversationRepository.findByUser1IdAndUser2Id(user1Id, user2Id)
                .map(conversation -> toConversationDTO(conversation, currentUserId))
                .orElseGet(() -> createConversation(currentUserId, user1Id, user2Id));
    }

    private ConversationDTO createConversation(Long currentUserId, Long user1Id, Long user2Id) {
        try {
            Conversation conversation = new Conversation();
            conversation.setUser1Id(user1Id);
            conversation.setUser2Id(user2Id);
            Conversation saved = conversationRepository.save(conversation);
            return toConversationDTO(saved, currentUserId);
        } catch (DataIntegrityViolationException e) {
            Conversation existing = conversationRepository.findByUser1IdAndUser2Id(user1Id, user2Id)
                    .orElseThrow(() -> e);
            return toConversationDTO(existing, currentUserId);
        }
    }
```

- [ ] **Step 2: Add send methods**

Continue the same class with:

```java
    @Override
    @Transactional
    public MessageDTO sendTextMessage(Long senderId, Long conversationId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能为空");
        }
        if (content.length() > 2000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能超过2000字");
        }
        Conversation conversation = getConversationForParticipant(conversationId, senderId);
        Long receiverId = getPeerUserId(conversation, senderId);
        Message message = saveMessage(conversation, senderId, receiverId, MessageType.TEXT, content.trim(), null, null);
        updateConversationLastMessage(conversation, message, content.trim());
        publishMessage(message);
        return toMessageDTO(message);
    }

    @Override
    @Transactional
    public MessageDTO sendImageMessage(Long senderId, Long conversationId, String imageUrl, String originalImageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片地址不能为空");
        }
        Conversation conversation = getConversationForParticipant(conversationId, senderId);
        Long receiverId = getPeerUserId(conversation, senderId);
        Message message = saveMessage(conversation, senderId, receiverId, MessageType.IMAGE, null, imageUrl, originalImageUrl);
        updateConversationLastMessage(conversation, message, "[图片]");
        publishMessage(message);
        return toMessageDTO(message);
    }

    private Message saveMessage(Conversation conversation, Long senderId, Long receiverId, MessageType type,
                                String content, String imageUrl, String originalImageUrl) {
        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageType(type);
        message.setContent(content);
        message.setImageUrl(imageUrl);
        message.setOriginalImageUrl(originalImageUrl);
        message.setIsRead(false);
        return messageRepository.save(message);
    }

    private void updateConversationLastMessage(Conversation conversation, Message message, String preview) {
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageType(message.getMessageType());
        conversation.setLastMessagePreview(truncatePreview(preview));
        conversation.setLastMessageAt(message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private String truncatePreview(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 100 ? value.substring(0, 100) : value;
    }
```

- [ ] **Step 3: Add query and read methods**

Continue the class with:

```java
    @Override
    public PageResult<MessageDTO> getMessages(Long conversationId, Long currentUserId, Integer page, Integer size) {
        getConversationForParticipant(conversationId, currentUserId);
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Message> messagePage = messageRepository.findByConversationId(conversationId, pageRequest);
        List<MessageDTO> records = messagePage.getContent().stream()
                .map(this::toMessageDTO)
                .toList();
        return PageResult.of(records, messagePage.getTotalElements(), page, size);
    }

    @Override
    public List<ConversationDTO> getConversationsByIds(List<Long> conversationIds, Long currentUserId) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyList();
        }
        return conversationRepository.findByIdsAndParticipant(conversationIds, currentUserId).stream()
                .map(conversation -> toConversationDTO(conversation, currentUserId))
                .toList();
    }

    @Override
    public ConversationDTO getConversationById(Long conversationId, Long currentUserId) {
        Conversation conversation = getConversationForParticipant(conversationId, currentUserId);
        return toConversationDTO(conversation, currentUserId);
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long conversationId, Long currentUserId) {
        getConversationForParticipant(conversationId, currentUserId);
        int count = messageRepository.markConversationAsRead(conversationId, currentUserId);
        log.info("Conversation messages marked as read: conversationId={}, userId={}, count={}, traceId={}",
                conversationId, currentUserId, count, LogUtil.getTraceId());
    }

    @Override
    public Long getUnreadMessageCount(Long currentUserId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(currentUserId);
    }
```

- [ ] **Step 4: Add helper methods and close class**

Finish the class with:

```java
    public boolean isConversationParticipant(Long conversationId, Long userId) {
        getConversationForParticipant(conversationId, userId);
        return true;
    }

    private Conversation getConversationForParticipant(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return conversation;
    }

    private Long getPeerUserId(Conversation conversation, Long currentUserId) {
        if (conversation.getUser1Id().equals(currentUserId)) {
            return conversation.getUser2Id();
        }
        if (conversation.getUser2Id().equals(currentUserId)) {
            return conversation.getUser1Id();
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
    }

    private ConversationDTO toConversationDTO(Conversation conversation, Long currentUserId) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setUser1Id(conversation.getUser1Id());
        dto.setUser2Id(conversation.getUser2Id());
        dto.setPeerUserId(getPeerUserId(conversation, currentUserId));
        dto.setLastMessageId(conversation.getLastMessageId());
        dto.setLastMessageType(conversation.getLastMessageType() != null ? conversation.getLastMessageType().name() : null);
        dto.setLastMessagePreview(conversation.getLastMessagePreview());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setUnreadCount(messageRepository.countByConversationIdAndReceiverIdAndIsReadFalse(conversation.getId(), currentUserId));
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        return dto;
    }

    private MessageDTO toMessageDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setMessageType(message.getMessageType().name());
        dto.setContent(message.getContent());
        dto.setImageUrl(message.getImageUrl());
        dto.setOriginalImageUrl(message.getOriginalImageUrl());
        dto.setIsRead(message.getIsRead());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    private void publishMessage(Message message) {
        try {
            String channel = MESSAGE_CHANNEL_PREFIX + message.getConversationId();
            String payload = message.getId() + ":" + message.getConversationId() + ":" + message.getSenderId()
                    + ":" + message.getReceiverId();
            stringRedisTemplate.convertAndSend(channel, payload);
        } catch (Exception e) {
            log.warn("Failed to publish message websocket event: messageId={}, error={}", message.getId(), e.getMessage());
        }
    }
}
```

- [ ] **Step 5: Compile message-service domain logic**

Run:

```bash
mvn -q -DskipTests -pl common,message-service -am compile
```

Expected: command exits `0`. If `ErrorCode.NOT_FOUND` is not defined, use `ErrorCode.BAD_REQUEST` for missing conversation and keep `ErrorCode.FORBIDDEN` for non-participant access.

- [ ] **Step 6: Commit message domain service**

Run:

```bash
git add message-service/src/main/java/com/social/message/service/MessageServiceImpl.java
git commit -m "feat: implement private message domain service"
```

---

## Task 4: Implement conversation notification upsert/read

**Files:**
- Modify: `common/src/main/java/com/social/common/api/NotificationService.java`
- Modify: `notification-service/src/main/java/com/social/notification/service/NotificationServiceImpl.java`

- [ ] **Step 1: Extend NotificationService API**

Add these methods before the closing brace in `common/src/main/java/com/social/common/api/NotificationService.java`:

```java
void upsertConversationNotification(Long recipientId, Long actorId, Long conversationId,
                                    String actorUsername, String actorAvatar);

void markConversationNotificationAsRead(Long recipientId, Long conversationId);
```

- [ ] **Step 2: Sort notification list by updatedAt**

In `notification-service/src/main/java/com/social/notification/service/NotificationServiceImpl.java`, change:

```java
PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
```

to:

```java
PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
```

- [ ] **Step 3: Implement upsertConversationNotification**

Add this method to `NotificationServiceImpl`:

```java
@Override
public void upsertConversationNotification(Long recipientId, Long actorId, Long conversationId,
                                           String actorUsername, String actorAvatar) {
    log.info(">>> upsertConversationNotification ENTER | recipientId={} | actorId={} | conversationId={}",
            recipientId, actorId, conversationId);
    Notification notification = notificationRepository
            .findByRecipientIdAndTypeAndTargetTypeAndTargetId(
                    recipientId,
                    NotificationType.MESSAGE,
                    "CONVERSATION",
                    conversationId
            )
            .orElseGet(() -> {
                Notification created = new Notification();
                created.setRecipientId(recipientId);
                created.setType(NotificationType.MESSAGE);
                created.setTargetType("CONVERSATION");
                created.setTargetId(conversationId);
                return created;
            });

    notification.setActorId(actorId);
    notification.setActorUsername(actorUsername);
    notification.setActorAvatar(actorAvatar);
    notification.setIsRead(false);
    Notification saved = notificationRepository.save(notification);

    String channel = RedisKeys.notificationChannel(recipientId);
    String message = saved.getId() + ":" + NotificationType.MESSAGE.name() + ":" + actorId + ":"
            + conversationId + ":CONVERSATION:" + actorUsername + ":"
            + (StringUtil.isNullOrEmpty(actorAvatar) ? " " : actorAvatar);
    stringRedisTemplate.convertAndSend(channel, message);
    log.info("<<< upsertConversationNotification EXIT | recipientId={} | notificationId={} | traceId={}",
            recipientId, saved.getId(), LogUtil.getTraceId());
}
```

- [ ] **Step 4: Implement markConversationNotificationAsRead**

Add this method to `NotificationServiceImpl`:

```java
@Override
public void markConversationNotificationAsRead(Long recipientId, Long conversationId) {
    log.debug(">>> markConversationNotificationAsRead ENTER | recipientId={} | conversationId={}", recipientId, conversationId);
    notificationRepository.findByRecipientIdAndTypeAndTargetTypeAndTargetId(
            recipientId,
            NotificationType.MESSAGE,
            "CONVERSATION",
            conversationId
    ).ifPresent(notification -> {
        notification.setIsRead(true);
        notificationRepository.save(notification);
    });
    log.info("<<< markConversationNotificationAsRead EXIT | recipientId={} | conversationId={} | traceId={}",
            recipientId, conversationId, LogUtil.getTraceId());
}
```

- [ ] **Step 5: Compile notification integration**

Run:

```bash
mvn -q -DskipTests -pl common,notification-service -am compile
```

Expected: command exits `0`.

- [ ] **Step 6: Commit notification changes**

Run:

```bash
git add common/src/main/java/com/social/common/api/NotificationService.java notification-service/src/main/java/com/social/notification/service/NotificationServiceImpl.java
git commit -m "feat: add conversation message notifications"
```

---

## Task 5: Implement message-service WebSocket reuse and active conversation join

**Files:**
- Create: `message-service/src/main/java/com/social/message/websocket/MessageWebSocketConfig.java`
- Create: `message-service/src/main/java/com/social/message/websocket/MessageWebSocketHandler.java`
- Modify: `message-service/src/main/java/com/social/message/service/MessageServiceImpl.java`

- [ ] **Step 1: Create WebSocket config**

Create `message-service/src/main/java/com/social/message/websocket/MessageWebSocketConfig.java`:

```java
package com.social.message.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Component
@EnableWebSocket
public class MessageWebSocketConfig implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;

    public MessageWebSocketConfig(MessageWebSocketHandler messageWebSocketHandler) {
        this.messageWebSocketHandler = messageWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, "/ws/message")
                .setAllowedOrigins("*");
    }
}
```

- [ ] **Step 2: Create WebSocket handler skeleton**

Create `message-service/src/main/java/com/social/message/websocket/MessageWebSocketHandler.java`:

```java
package com.social.message.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.common.entity.Message;
import com.social.common.repository.MessageRepository;
import com.social.message.service.MessageServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final MessageServiceImpl messageService;
    private final MessageRepository messageRepository;
    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    private final Map<String, Long> activeConversations = new ConcurrentHashMap<>();
    private final Map<Long, MessageListener> conversationListeners = new ConcurrentHashMap<>();

    private static final String MESSAGE_CHANNEL_PREFIX = "message:conversation:";
```

- [ ] **Step 3: Add connection and message handling**

Continue `MessageWebSocketHandler` with:

```java
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        userSessions.put(userId, session);
        sessionUsers.put(session.getId(), userId);
        log.info("Message WebSocket connected: userId={}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.path("type").asText();
        if ("ping".equals(type)) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            return;
        }
        if ("JOIN_CONVERSATION".equals(type)) {
            joinConversation(session, json.path("conversationId").asLong());
            return;
        }
        if ("LEAVE_CONVERSATION".equals(type)) {
            leaveConversation(session, json.path("conversationId").asLong());
        }
    }

    private void joinConversation(WebSocketSession session, Long conversationId) throws Exception {
        Long userId = sessionUsers.get(session.getId());
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        messageService.isConversationParticipant(conversationId, userId);
        activeConversations.put(session.getId(), conversationId);
        subscribeConversation(conversationId);
        session.sendMessage(new TextMessage("{\"type\":\"JOINED\",\"conversationId\":" + conversationId + "}"));
    }

    private void leaveConversation(WebSocketSession session, Long conversationId) {
        Long active = activeConversations.get(session.getId());
        if (active != null && active.equals(conversationId)) {
            activeConversations.remove(session.getId());
        }
    }
```

- [ ] **Step 4: Add Redis subscription and push logic**

Continue `MessageWebSocketHandler` with:

```java
    private void subscribeConversation(Long conversationId) {
        conversationListeners.computeIfAbsent(conversationId, id -> {
            MessageListener listener = (redisMessage, pattern) -> handleRedisMessage(id, new String(redisMessage.getBody(), StandardCharsets.UTF_8));
            listenerContainer.addMessageListener(listener, new ChannelTopic(MESSAGE_CHANNEL_PREFIX + id));
            return listener;
        });
    }

    private void handleRedisMessage(Long conversationId, String body) {
        try {
            String[] parts = body.split(":");
            Long messageId = Long.parseLong(parts[0]);
            Message message = messageRepository.findById(messageId).orElse(null);
            if (message == null) {
                return;
            }
            WebSocketSession receiverSession = userSessions.get(message.getReceiverId());
            if (receiverSession == null || !receiverSession.isOpen()) {
                return;
            }
            Long activeConversationId = activeConversations.get(receiverSession.getId());
            if (!conversationId.equals(activeConversationId)) {
                return;
            }
            Map<String, Object> payload = Map.of(
                    "type", "MESSAGE",
                    "conversationId", conversationId,
                    "message", Map.of(
                            "id", message.getId(),
                            "conversationId", message.getConversationId(),
                            "senderId", message.getSenderId(),
                            "receiverId", message.getReceiverId(),
                            "messageType", message.getMessageType().name(),
                            "content", message.getContent() == null ? "" : message.getContent(),
                            "imageUrl", message.getImageUrl() == null ? "" : message.getImageUrl(),
                            "originalImageUrl", message.getOriginalImageUrl() == null ? "" : message.getOriginalImageUrl(),
                            "isRead", message.getIsRead(),
                            "createdAt", message.getCreatedAt() != null ? message.getCreatedAt().toString() : ""
                    )
            );
            receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            log.error("Failed to push private message: conversationId={}, body={}, error={}", conversationId, body, e.getMessage());
        }
    }
```

- [ ] **Step 5: Add close and authentication helpers**

Finish `MessageWebSocketHandler` with:

```java
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = sessionUsers.remove(session.getId());
        activeConversations.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
        }
        log.info("Message WebSocket closed: userId={}, status={}", userId, status);
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        String headerUserId = session.getHandshakeHeaders().getFirst("X-User-Id");
        if (headerUserId != null && !headerUserId.isEmpty()) {
            try {
                return Long.parseLong(headerUserId);
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header: {}", headerUserId);
            }
        }

        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) {
            return null;
        }
        String token = null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                token = param.substring(6);
                break;
            }
        }
        if (token == null) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.warn("Failed to parse message WebSocket token: {}", e.getMessage());
            return null;
        }
    }
}
```

- [ ] **Step 6: Compile WebSocket implementation**

Run:

```bash
mvn -q -DskipTests -pl common,message-service -am compile
```

Expected: command exits `0`.

- [ ] **Step 7: Commit WebSocket implementation**

Run:

```bash
git add message-service/src/main/java/com/social/message/websocket message-service/src/main/java/com/social/message/service/MessageServiceImpl.java
git commit -m "feat: add reusable private message websocket"
```

---

## Task 6: Add facade message APIs and notification aggregation

**Files:**
- Create: `facade-service/src/main/java/com/social/facade/dto/ConversationFacadeResponse.java`
- Create: `facade-service/src/main/java/com/social/facade/dto/MessageFacadeResponse.java`
- Create: `facade-service/src/main/java/com/social/facade/controller/MessageFacadeController.java`
- Modify: `facade-service/src/main/java/com/social/facade/dto/NotificationFacadeResponse.java`
- Modify: `facade-service/src/main/java/com/social/facade/controller/NotificationFacadeController.java`

- [ ] **Step 1: Create facade DTOs**

Create `facade-service/src/main/java/com/social/facade/dto/ConversationFacadeResponse.java`:

```java
package com.social.facade.dto;

import com.social.common.dto.ConversationDTO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ConversationFacadeResponse implements Serializable {
    private Long id;
    private Long peerUserId;
    private String peerUsername;
    private String peerAvatar;
    private Long lastMessageId;
    private String lastMessageType;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;

    public static ConversationFacadeResponse fromConversationDTO(ConversationDTO dto, String peerUsername, String peerAvatar) {
        ConversationFacadeResponse response = new ConversationFacadeResponse();
        response.setId(dto.getId());
        response.setPeerUserId(dto.getPeerUserId());
        response.setPeerUsername(peerUsername);
        response.setPeerAvatar(peerAvatar);
        response.setLastMessageId(dto.getLastMessageId());
        response.setLastMessageType(dto.getLastMessageType());
        response.setLastMessagePreview(dto.getLastMessagePreview());
        response.setLastMessageAt(dto.getLastMessageAt());
        response.setUnreadCount(dto.getUnreadCount());
        return response;
    }
}
```

Create `facade-service/src/main/java/com/social/facade/dto/MessageFacadeResponse.java`:

```java
package com.social.facade.dto;

import com.social.common.dto.MessageDTO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MessageFacadeResponse implements Serializable {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String messageType;
    private String content;
    private String imageUrl;
    private String originalImageUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static MessageFacadeResponse fromMessageDTO(MessageDTO dto) {
        MessageFacadeResponse response = new MessageFacadeResponse();
        response.setId(dto.getId());
        response.setConversationId(dto.getConversationId());
        response.setSenderId(dto.getSenderId());
        response.setReceiverId(dto.getReceiverId());
        response.setMessageType(dto.getMessageType());
        response.setContent(dto.getContent());
        response.setImageUrl(dto.getImageUrl());
        response.setOriginalImageUrl(dto.getOriginalImageUrl());
        response.setIsRead(dto.getIsRead());
        response.setCreatedAt(dto.getCreatedAt());
        return response;
    }
}
```

- [ ] **Step 2: Create MessageFacadeController**

Create `facade-service/src/main/java/com/social/facade/controller/MessageFacadeController.java`:

```java
package com.social.facade.controller;

import com.social.common.api.MessageService;
import com.social.common.api.NotificationService;
import com.social.common.api.UserService;
import com.social.common.dto.*;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.facade.dto.ConversationFacadeResponse;
import com.social.facade.dto.MessageFacadeResponse;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageFacadeController {

    @DubboReference(version = "1.0.0", check = false)
    private MessageService messageService;

    @DubboReference(version = "1.0.0", check = false)
    private UserService userService;

    @DubboReference(version = "1.0.0", check = false)
    private NotificationService notificationService;

    @PostMapping("/conversations")
    public Result<ConversationFacadeResponse> getOrCreateConversation(
            @RequestHeader("X-User-Id") Long currentUserId,
            @Valid @RequestBody CreateConversationRequest request) {
        if (currentUserId.equals(request.getTargetUserId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能给自己发私信");
        }
        if (!userService.isUserExists(request.getTargetUserId())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        ConversationDTO conversation = messageService.getOrCreateConversation(currentUserId, request.getTargetUserId());
        return Result.success(enrichConversation(conversation));
    }

    @GetMapping("/conversations/{conversationId}")
    public Result<ConversationFacadeResponse> getConversation(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long conversationId) {
        ConversationDTO conversation = messageService.getConversationById(conversationId, currentUserId);
        return Result.success(enrichConversation(conversation));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Result<PageResult<MessageFacadeResponse>> getMessages(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long conversationId,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "30") Integer size) {
        PageResult<MessageDTO> messages = messageService.getMessages(conversationId, currentUserId, page, size);
        List<MessageFacadeResponse> records = messages.getRecords().stream()
                .map(MessageFacadeResponse::fromMessageDTO)
                .toList();
        return Result.success(PageResult.of(records, messages.getTotal(), messages.getPage(), messages.getSize()));
    }

    @PostMapping("/conversations/{conversationId}/messages/text")
    public Result<MessageFacadeResponse> sendTextMessage(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendTextMessageRequest request) {
        MessageDTO message = messageService.sendTextMessage(currentUserId, conversationId, request.getContent());
        upsertMessageNotification(currentUserId, message.getReceiverId(), conversationId);
        return Result.success(MessageFacadeResponse.fromMessageDTO(message));
    }

    @PostMapping("/conversations/{conversationId}/messages/image")
    public Result<MessageFacadeResponse> sendImageMessage(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendImageMessageRequest request) {
        MessageDTO message = messageService.sendImageMessage(currentUserId, conversationId, request.getImageUrl(), request.getOriginalImageUrl());
        upsertMessageNotification(currentUserId, message.getReceiverId(), conversationId);
        return Result.success(MessageFacadeResponse.fromMessageDTO(message));
    }

    @PutMapping("/conversations/{conversationId}/read")
    public Result<Void> markConversationAsRead(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long conversationId) {
        messageService.markConversationAsRead(conversationId, currentUserId);
        notificationService.markConversationNotificationAsRead(currentUserId, conversationId);
        return Result.success();
    }

    private void upsertMessageNotification(Long senderId, Long receiverId, Long conversationId) {
        UserDTO sender = userService.getUserById(senderId);
        String username = sender.getUsername() != null ? sender.getUsername() : "未知用户";
        String avatar = sender.getAvatar() != null ? sender.getAvatar() : "";
        notificationService.upsertConversationNotification(receiverId, senderId, conversationId, username, avatar);
    }

    private ConversationFacadeResponse enrichConversation(ConversationDTO conversation) {
        UserDTO peer = userService.getUserById(conversation.getPeerUserId());
        String username = peer.getUsername() != null ? peer.getUsername() : "未知用户";
        String avatar = peer.getAvatar() != null ? peer.getAvatar() : "";
        return ConversationFacadeResponse.fromConversationDTO(conversation, username, avatar);
    }
}
```

- [ ] **Step 3: Extend NotificationFacadeResponse with conversation**

Modify `facade-service/src/main/java/com/social/facade/dto/NotificationFacadeResponse.java` and add field:

```java
private ConversationFacadeResponse conversation;
```

Also add this setter line in `fromNotificationDTO` if it does not already exist:

```java
response.setTargetTitle(targetTitle);
```

- [ ] **Step 4: Aggregate conversation summaries in NotificationFacadeController**

Modify `facade-service/src/main/java/com/social/facade/controller/NotificationFacadeController.java`:

Add Dubbo reference:

```java
@DubboReference(version = "1.0.0", check = false)
private MessageService messageService;

@DubboReference(version = "1.0.0", check = false)
private UserService userService;
```

Add imports:

```java
import com.social.common.api.MessageService;
import com.social.common.api.UserService;
import com.social.common.dto.ConversationDTO;
import com.social.common.dto.UserDTO;
import com.social.facade.dto.ConversationFacadeResponse;
```

In `getNotificationList`, after `postTitleMap`, create:

```java
Map<Long, ConversationFacadeResponse> conversationMap = getConversationMap(notifications.getRecords(), userId);
```

When mapping response, set conversation:

```java
NotificationFacadeResponse response = NotificationFacadeResponse.fromNotificationDTO(
        notification,
        postTitleMap.get(notification.getTargetId())
);
response.setConversation(conversationMap.get(notification.getTargetId()));
return response;
```

Add helper:

```java
private Map<Long, ConversationFacadeResponse> getConversationMap(List<NotificationDTO> notifications, Long currentUserId) {
    List<Long> conversationIds = notifications.stream()
            .filter(notification -> "CONVERSATION".equals(notification.getTargetType()))
            .map(NotificationDTO::getTargetId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    if (conversationIds.isEmpty()) {
        return Collections.emptyMap();
    }

    return messageService.getConversationsByIds(conversationIds, currentUserId).stream()
            .collect(Collectors.toMap(ConversationDTO::getId, this::enrichConversation, (left, right) -> left));
}

private ConversationFacadeResponse enrichConversation(ConversationDTO conversation) {
    String username = "未知用户";
    String avatar = "";
    try {
        UserDTO user = userService.getUserById(conversation.getPeerUserId());
        username = user.getUsername() != null ? user.getUsername() : "未知用户";
        avatar = user.getAvatar() != null ? user.getAvatar() : "";
    } catch (Exception ignored) {
        username = "未知用户";
        avatar = "";
    }
    return ConversationFacadeResponse.fromConversationDTO(conversation, username, avatar);
}
```

- [ ] **Step 5: Compile facade integration**

Run:

```bash
mvn -q -DskipTests -pl common,message-service,facade-service,notification-service -am compile
```

Expected: command exits `0`.

- [ ] **Step 6: Commit facade APIs**

Run:

```bash
git add facade-service/src/main/java/com/social/facade common/src/main/java/com/social/common/dto common/src/main/java/com/social/common/api
git commit -m "feat: add private message facade APIs"
```

---

## Task 7: Gateway and Docker deployment wiring

**Files:**
- Modify: `gateway/src/main/resources/application.yml`
- Create: `docker/Dockerfile.message-service`
- Modify: `scripts/docker-build.sh`
- Modify: `scripts/docker-run.sh`

- [ ] **Step 1: Add Gateway WebSocket route**

In `gateway/src/main/resources/application.yml`, add route after `notification-websocket`:

```yaml
        - id: message-websocket
          uri: lb:ws://message-service
          predicates:
            - Path=/ws/message
          filters:
            - StripPrefix=0
```

- [ ] **Step 2: Create Dockerfile**

Create `docker/Dockerfile.message-service` by following the existing service Dockerfile style:

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/message-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8088

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

- [ ] **Step 3: Add message-service to docker-build.sh**

In `scripts/docker-build.sh`, add `message-service` to the `SERVICES` array after `notification-service`:

```bash
    "notification-service"
    "message-service"
    "facade-service"
```

- [ ] **Step 4: Add message-service to docker-run.sh**

In `scripts/docker-run.sh`, add:

```bash
["message-service"]="8088:swr.cn-north-4.myhuaweicloud.com/ddn-k8s/social-platform-message-service:latest"
```

Place it after `notification-service`.

- [ ] **Step 5: Compile affected services**

Run:

```bash
mvn -q -DskipTests -pl gateway,message-service -am compile
```

Expected: command exits `0`.

- [ ] **Step 6: Commit deployment wiring**

Run:

```bash
git add gateway/src/main/resources/application.yml docker/Dockerfile.message-service scripts/docker-build.sh scripts/docker-run.sh
git commit -m "feat: wire message service deployment"
```

---

## Task 8: Frontend message API, types, and reusable WebSocket store

**Files:**
- Modify: `frontend/src/types/index.ts`
- Create: `frontend/src/api/message.ts`
- Create: `frontend/src/stores/message.ts`
- Create: `frontend/src/stores/messageSocket.ts`

- [ ] **Step 1: Add frontend types**

Append these interfaces to `frontend/src/types/index.ts`:

```ts
export interface Conversation {
  id: number
  peerUserId: number
  peerUsername: string
  peerAvatar?: string
  lastMessageId?: number
  lastMessageType?: 'TEXT' | 'IMAGE'
  lastMessagePreview?: string
  lastMessageAt?: string
  unreadCount?: number
}

export interface Message {
  id: number
  conversationId: number
  senderId: number
  receiverId: number
  messageType: 'TEXT' | 'IMAGE'
  content?: string
  imageUrl?: string
  originalImageUrl?: string
  isRead: boolean
  createdAt?: string
}
```

- [ ] **Step 2: Create message API**

Create `frontend/src/api/message.ts`:

```ts
import request from '@/utils/axios'

export const createConversation = (targetUserId: number) => {
  return request.post('/messages/conversations', { targetUserId })
}

export const getConversation = (conversationId: number) => {
  return request.get(`/messages/conversations/${conversationId}`)
}

export const getMessages = (conversationId: number, page: number = 1, size: number = 30) => {
  return request.get(`/messages/conversations/${conversationId}/messages`, {
    params: { page, size }
  })
}

export const sendTextMessage = (conversationId: number, content: string) => {
  return request.post(`/messages/conversations/${conversationId}/messages/text`, { content })
}

export const sendImageMessage = (conversationId: number, imageUrl: string, originalImageUrl?: string) => {
  return request.post(`/messages/conversations/${conversationId}/messages/image`, { imageUrl, originalImageUrl })
}

export const markConversationAsRead = (conversationId: number) => {
  return request.put(`/messages/conversations/${conversationId}/read`)
}
```

- [ ] **Step 3: Create message store**

Create `frontend/src/stores/message.ts`:

```ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Conversation, Message } from '@/types'
import {
  createConversation,
  getConversation,
  getMessages,
  sendTextMessage,
  sendImageMessage,
  markConversationAsRead
} from '@/api/message'

export const useMessageStore = defineStore('message', () => {
  const currentConversation = ref<Conversation | null>(null)
  const messages = ref<Message[]>([])
  const loading = ref(false)

  const openConversationWithUser = async (targetUserId: number) => {
    const res = await createConversation(targetUserId)
    currentConversation.value = res.data || res
    return currentConversation.value
  }

  const loadConversation = async (conversationId: number) => {
    const res = await getConversation(conversationId)
    currentConversation.value = res.data || res
    return currentConversation.value
  }

  const loadMessages = async (conversationId: number) => {
    loading.value = true
    try {
      const res = await getMessages(conversationId)
      const records = res.data?.records || []
      messages.value = [...records].reverse()
      return messages.value
    } finally {
      loading.value = false
    }
  }

  const sendText = async (conversationId: number, content: string) => {
    const res = await sendTextMessage(conversationId, content)
    const message = res.data || res
    messages.value.push(message)
    return message
  }

  const sendImage = async (conversationId: number, imageUrl: string, originalImageUrl?: string) => {
    const res = await sendImageMessage(conversationId, imageUrl, originalImageUrl)
    const message = res.data || res
    messages.value.push(message)
    return message
  }

  const appendIncomingMessage = (message: Message) => {
    if (messages.value.some(m => m.id === message.id)) return
    messages.value.push(message)
  }

  const markRead = async (conversationId: number) => {
    await markConversationAsRead(conversationId)
    messages.value.forEach(message => {
      if (message.conversationId === conversationId) {
        message.isRead = true
      }
    })
    if (currentConversation.value?.id === conversationId) {
      currentConversation.value.unreadCount = 0
    }
  }

  return {
    currentConversation,
    messages,
    loading,
    openConversationWithUser,
    loadConversation,
    loadMessages,
    sendText,
    sendImage,
    appendIncomingMessage,
    markRead
  }
})
```

- [ ] **Step 4: Create reusable message WebSocket store**

Create `frontend/src/stores/messageSocket.ts`:

```ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useMessageStore } from '@/stores/message'

const IDLE_CLOSE_MS = 5 * 60 * 1000
const HEARTBEAT_MS = 30 * 1000

const buildMessageWebSocketUrl = (token: string): string => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/message?token=${token}`
}

export const useMessageSocketStore = defineStore('messageSocket', () => {
  const connected = ref(false)
  const activeConversationId = ref<number | null>(null)
  let ws: WebSocket | null = null
  let idleTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null

  const clearIdleTimer = () => {
    if (idleTimer) {
      clearTimeout(idleTimer)
      idleTimer = null
    }
  }

  const clearHeartbeat = () => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  const connect = () => {
    if (ws && ws.readyState === WebSocket.OPEN) return
    const token = localStorage.getItem('token')
    if (!token) return
    ws = new WebSocket(buildMessageWebSocketUrl(token))

    ws.onopen = () => {
      connected.value = true
      clearHeartbeat()
      heartbeatTimer = setInterval(() => {
        if (ws?.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'ping' }))
        }
      }, HEARTBEAT_MS)
      if (activeConversationId.value) {
        joinConversation(activeConversationId.value)
      }
    }

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      if (data.type === 'pong' || data.type === 'JOINED') return
      if (data.type === 'MESSAGE' && data.conversationId === activeConversationId.value) {
        useMessageStore().appendIncomingMessage(data.message)
      }
    }

    ws.onclose = () => {
      connected.value = false
      clearHeartbeat()
      const shouldReconnect = activeConversationId.value != null
      ws = null
      if (shouldReconnect) {
        setTimeout(connect, 1000)
      }
    }
  }

  const joinConversation = (conversationId: number) => {
    clearIdleTimer()
    activeConversationId.value = conversationId
    connect()
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'JOIN_CONVERSATION', conversationId }))
    }
  }

  const leaveConversation = (conversationId: number) => {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'LEAVE_CONVERSATION', conversationId }))
    }
    if (activeConversationId.value === conversationId) {
      activeConversationId.value = null
    }
  }

  const scheduleIdleClose = () => {
    clearIdleTimer()
    idleTimer = setTimeout(() => {
      disconnect()
    }, IDLE_CLOSE_MS)
  }

  const disconnect = () => {
    clearIdleTimer()
    clearHeartbeat()
    activeConversationId.value = null
    if (ws) {
      ws.close()
      ws = null
    }
    connected.value = false
  }

  return {
    connected,
    activeConversationId,
    connect,
    joinConversation,
    leaveConversation,
    scheduleIdleClose,
    disconnect
  }
})
```

- [ ] **Step 5: Run frontend type check**

Run:

```bash
cd frontend && npm run build
```

Expected: `vue-tsc` and Vite build both pass.

- [ ] **Step 6: Commit frontend stores and API**

Run:

```bash
git add frontend/src/types/index.ts frontend/src/api/message.ts frontend/src/stores/message.ts frontend/src/stores/messageSocket.ts
git commit -m "feat: add private message frontend stores"
```

---

## Task 9: Frontend profile entry and chat detail page

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/user/UserProfilePage.vue`
- Create: `frontend/src/views/message/ChatDetailPage.vue`

- [ ] **Step 1: Add chat route**

In `frontend/src/router/index.ts`, add route before `/notifications`:

```ts
{
  path: '/messages/conversations/:id',
  name: 'ChatDetail',
  component: () => import('@/views/message/ChatDetailPage.vue'),
  meta: { requiresAuth: true }
},
```

- [ ] **Step 2: Add private-message button to user profile**

In `frontend/src/views/user/UserProfilePage.vue`, import router and message store:

```ts
import { useRouter } from 'vue-router'
import { useMessageStore } from '@/stores/message'
```

After `const route = useRoute()`, add:

```ts
const router = useRouter()
const messageStore = useMessageStore()
```

Add method:

```ts
const startPrivateMessage = async () => {
  if (!authStore.isLoggedIn()) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  const conversation = await messageStore.openConversationWithUser(userId.value)
  if (conversation?.id) {
    router.push(`/messages/conversations/${conversation.id}`)
  }
}
```

In the template, inside `.profile-actions`, change:

```vue
<FollowButton v-if="!isOwnProfile" :userId="userId" />
```

to:

```vue
<template v-if="!isOwnProfile">
  <FollowButton :userId="userId" />
  <el-button type="primary" @click="startPrivateMessage">私聊</el-button>
</template>
```

- [ ] **Step 3: Create ChatDetailPage.vue**

Create `frontend/src/views/message/ChatDetailPage.vue`:

```vue
<template>
  <div class="chat-detail-page">
    <AppHeader />
    <AppLayout>
      <div class="chat-container" v-if="conversation">
        <div class="chat-header">
          <el-button text @click="router.back()">←</el-button>
          <UserAvatar :user="{ avatar: conversation.peerAvatar, username: conversation.peerUsername }" :size="40" />
          <span class="peer-name">{{ conversation.peerUsername }}</span>
        </div>

        <div class="message-list" ref="messageListRef" v-loading="messageStore.loading">
          <div
            v-for="message in messageStore.messages"
            :key="message.id"
            class="message-row"
            :class="{ mine: message.senderId === authStore.userInfo?.id }"
          >
            <div class="message-bubble text" v-if="message.messageType === 'TEXT'">
              {{ message.content }}
            </div>
            <img
              v-else
              class="message-image"
              :src="message.imageUrl"
              alt="图片消息"
              @click="previewImage(message.originalImageUrl || message.imageUrl || '')"
            />
          </div>
        </div>

        <div class="composer">
          <input ref="imageInputRef" type="file" accept="image/*" hidden @change="handleImageSelected" />
          <el-button @click="imageInputRef?.click()">图片</el-button>
          <el-checkbox v-model="sendOriginal">原图</el-checkbox>
          <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入消息"
            @keydown.enter.exact.prevent="sendText"
          />
          <el-button type="primary" :disabled="!inputText.trim()" @click="sendText">发送</el-button>
        </div>
      </div>
    </AppLayout>

    <teleport to="body">
      <el-image-viewer
        v-if="previewUrl"
        :url-list="[previewUrl]"
        @close="previewUrl = ''"
      />
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import UserAvatar from '@/components/user/UserAvatar.vue'
import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/message'
import { useMessageSocketStore } from '@/stores/messageSocket'
import { uploadFile } from '@/api/upload'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const messageStore = useMessageStore()
const socketStore = useMessageSocketStore()

const conversationId = computed(() => Number(route.params.id))
const conversation = computed(() => messageStore.currentConversation)
const inputText = ref('')
const sendOriginal = ref(false)
const previewUrl = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const loadAll = async () => {
  await messageStore.loadConversation(conversationId.value)
  await messageStore.loadMessages(conversationId.value)
  await messageStore.markRead(conversationId.value)
  socketStore.joinConversation(conversationId.value)
  await scrollToBottom()
}

const sendText = async () => {
  const content = inputText.value.trim()
  if (!content) return
  inputText.value = ''
  await messageStore.sendText(conversationId.value, content)
  await scrollToBottom()
}

const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const objectUrl = URL.createObjectURL(file)
    img.onload = () => {
      const maxSide = 1280
      const scale = Math.min(1, maxSide / Math.max(img.width, img.height))
      const canvas = document.createElement('canvas')
      canvas.width = Math.round(img.width * scale)
      canvas.height = Math.round(img.height * scale)
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        URL.revokeObjectURL(objectUrl)
        reject(new Error('无法压缩图片'))
        return
      }
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      canvas.toBlob(blob => {
        URL.revokeObjectURL(objectUrl)
        if (!blob) {
          reject(new Error('图片压缩失败'))
          return
        }
        resolve(new File([blob], file.name.replace(/\.[^.]+$/, '.jpg'), { type: 'image/jpeg' }))
      }, 'image/jpeg', 0.8)
    }
    img.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('图片读取失败'))
    }
    img.src = objectUrl
  })
}

const handleImageSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  try {
    const compressed = await compressImage(file)
    const compressedRes = await uploadFile(compressed, 'image')
    const imageUrl = compressedRes.data?.url || compressedRes.url
    let originalImageUrl: string | undefined

    if (sendOriginal.value) {
      const originalRes = await uploadFile(file, 'image')
      originalImageUrl = originalRes.data?.url || originalRes.url
    }

    await messageStore.sendImage(conversationId.value, imageUrl, originalImageUrl)
    await scrollToBottom()
  } catch (error: any) {
    ElMessage.error(error.message || '图片发送失败')
  }
}

const previewImage = (url: string) => {
  previewUrl.value = url
}

onMounted(loadAll)

watch(() => route.params.id, async () => {
  socketStore.leaveConversation(conversationId.value)
  await loadAll()
})

onUnmounted(() => {
  socketStore.leaveConversation(conversationId.value)
  socketStore.scheduleIdleClose()
})
</script>

<style scoped lang="scss">
.chat-detail-page {
  min-height: 100vh;
  background: #fafafa;
}

.chat-container {
  max-width: 720px;
  height: calc(100vh - 80px);
  margin: 0 auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 16px 16px 0 0;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;

  .peer-name {
    font-weight: 600;
    color: #333;
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  padding: 16px;
}

.message-row {
  display: flex;
  margin-bottom: 12px;

  &.mine {
    justify-content: flex-end;
  }
}

.message-bubble {
  max-width: 70%;
  padding: 10px 12px;
  border-radius: 14px;
  background: #f2f3f5;
  color: #333;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.mine .message-bubble {
  background: #4CAF82;
  color: #fff;
}

.message-image {
  max-width: 240px;
  max-height: 320px;
  border-radius: 12px;
  object-fit: cover;
  cursor: pointer;
}

.composer {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border-radius: 0 0 16px 16px;
  padding: 12px;
  border-top: 1px solid #f0f0f0;
}
</style>
```

- [ ] **Step 4: Build frontend**

Run:

```bash
cd frontend && npm run build
```

Expected: command exits `0`.

- [ ] **Step 5: Commit chat page**

Run:

```bash
git add frontend/src/router/index.ts frontend/src/views/user/UserProfilePage.vue frontend/src/views/message/ChatDetailPage.vue
git commit -m "feat: add private chat page"
```

---

## Task 10: Notification page support for MESSAGE conversation notifications

**Files:**
- Modify: `frontend/src/api/notification.ts`
- Modify: `frontend/src/stores/notification.ts`
- Modify: `frontend/src/views/notification/NotificationPage.vue`
- Modify: `frontend/src/components/notification/NotificationItem.vue`

- [ ] **Step 1: Extend notification frontend type**

In `frontend/src/api/notification.ts` and `frontend/src/stores/notification.ts`, add optional conversation field to notification interfaces:

```ts
conversation?: {
  id: number
  peerUserId: number
  peerUsername: string
  peerAvatar?: string
  lastMessageType?: 'TEXT' | 'IMAGE'
  lastMessagePreview?: string
  unreadCount?: number
}
```

- [ ] **Step 2: Map conversation in NotificationPage**

In `frontend/src/views/notification/NotificationPage.vue`, include `conversation: n.conversation` in the `newNotifications` map:

```ts
conversation: n.conversation,
```

Update `handleClick` to route conversations:

```ts
const handleClick = (item: any) => {
  markRead(item)
  if (item.targetType === 'CONVERSATION' && item.targetId) {
    router.push(`/messages/conversations/${item.targetId}`)
    return
  }
  if (item.targetType === 'POST' && item.targetId) {
    router.push(`/post/${item.targetId}`)
    return
  }
  if (item.targetType === 'USER' && item.actorId) {
    router.push(`/user/${item.actorId}`)
  }
}
```

- [ ] **Step 3: Update notification message text**

In `frontend/src/stores/notification.ts`, update `getNotificationMessage`:

```ts
case 'MESSAGE':
  return `${username} 给你发了私信`
```

- [ ] **Step 4: Update NotificationItem display**

In `frontend/src/components/notification/NotificationItem.vue`, update action text:

```ts
case 'MESSAGE':
  return '给你发了私信'
```

Add computed summary:

```ts
const conversationPreview = computed(() => {
  if (props.notification.type !== 'MESSAGE') return ''
  const preview = props.notification.conversation?.lastMessagePreview
  return preview || '查看私信'
})

const unreadCount = computed(() => props.notification.conversation?.unreadCount || 0)
```

In template, show this below text:

```vue
<div v-if="notification.type === 'MESSAGE'" class="conversation-preview">
  <span>{{ conversationPreview }}</span>
  <span v-if="unreadCount > 0" class="message-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
</div>
```

Add styles:

```scss
.conversation-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
  color: #666;
  font-size: 13px;
  line-height: 1.4;
}

.message-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  background: #ff7f7f;
  color: #fff;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
```

- [ ] **Step 5: Build frontend**

Run:

```bash
cd frontend && npm run build
```

Expected: command exits `0`.

- [ ] **Step 6: Commit notification UI support**

Run:

```bash
git add frontend/src/api/notification.ts frontend/src/stores/notification.ts frontend/src/views/notification/NotificationPage.vue frontend/src/components/notification/NotificationItem.vue
git commit -m "feat: show private messages in notifications"
```

---

## Task 11: End-to-end verification and regression checklist

**Files:**
- Modify: `docs/superpowers/specs/2026-06-28-private-message-design.md` only if verification reveals design drift.
- No source file changes expected in this task unless a verification failure identifies a concrete bug.

- [ ] **Step 1: Compile all backend services**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: command exits `0`.

- [ ] **Step 2: Build frontend**

Run:

```bash
cd frontend && npm run build
```

Expected: command exits `0`.

- [ ] **Step 3: Apply database migration**

Apply the migration block added to `sql/schema.sql` to the local `social_platform` database. Confirm these tables exist:

```sql
SHOW TABLES LIKE 'conversations';
SHOW TABLES LIKE 'messages';
```

Expected: both queries return one row.

- [ ] **Step 4: Build and start Docker services**

Run:

```bash
./scripts/docker-build.sh message-service
./scripts/docker-run.sh start message-service
./scripts/docker-build.sh gateway
./scripts/docker-run.sh restart gateway
./scripts/docker-build.sh facade-service
./scripts/docker-run.sh restart facade-service
./scripts/docker-build.sh notification-service
./scripts/docker-run.sh restart notification-service
```

Expected: all containers start and register in Nacos.

- [ ] **Step 5: Manual API smoke test for conversation creation**

With a valid JWT for user A, run:

```bash
curl -X POST http://localhost:8080/api/messages/conversations \
  -H "Authorization: Bearer <USER_A_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"targetUserId": <USER_B_ID>}'
```

Expected response:

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "peerUserId": <USER_B_ID>
  }
}
```

- [ ] **Step 6: Manual API smoke test for text sending**

Run:

```bash
curl -X POST http://localhost:8080/api/messages/conversations/<CONVERSATION_ID>/messages/text \
  -H "Authorization: Bearer <USER_A_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"content":"你好，这是一条私信"}'
```

Expected: response data contains `messageType: "TEXT"`, `senderId` as user A, and `receiverId` as user B.

- [ ] **Step 7: Verify one reused MESSAGE notification**

Send a second text message in the same conversation, then query notifications as user B:

```bash
curl http://localhost:8080/api/notifications \
  -H "Authorization: Bearer <USER_B_TOKEN>"
```

Expected:

- One notification with `type: "MESSAGE"` and `targetType: "CONVERSATION"` for this conversation.
- The same notification is updated to the top rather than creating two rows for two messages.
- `conversation.lastMessagePreview` equals the latest message content.

- [ ] **Step 8: Manual frontend flow test**

In the browser:

1. Log in as user A.
2. Open user B's profile.
3. Confirm `私聊` button is visible.
4. Click `私聊`.
5. Confirm chat detail page opens.
6. Send text.
7. Send compressed image.
8. Send original image.
9. Log in as user B in another browser.
10. Confirm notification bell shows unread.
11. Open notification page.
12. Confirm private message item appears.
13. Click it.
14. Confirm chat page opens and unread state clears.

Expected: each step completes without console errors.

- [ ] **Step 9: WebSocket behavior test**

In browser developer tools Network tab:

1. Log in and stay outside chat page.
2. Confirm only `/ws/notify` is connected.
3. Enter a chat page.
4. Confirm `/ws/message` opens.
5. Switch to another chat.
6. Confirm `/ws/message` remains one connection.
7. Leave chat page.
8. Confirm `/ws/message` remains open briefly.
9. Wait 5 minutes.
10. Confirm `/ws/message` closes.

Expected: behavior matches the reusable idle-close design.

- [ ] **Step 10: Final status and commit any verification fixes**

If verification required source fixes, commit them:

```bash
git add <fixed-files>
git commit -m "fix: stabilize private message flow"
```

If no fixes were required, do not create an empty commit.

---

## Self-Review Notes

Spec coverage:

- New `message-service`: Tasks 2, 3, 5, 7.
- Text messages: Tasks 3, 6, 9, 11.
- Image messages with compression/original: Tasks 6, 8, 9, 11.
- `MESSAGE` notifications: Tasks 1, 4, 6, 10, 11.
- One notification per conversation: Task 4 and Task 11 Step 7.
- Reusable WebSocket with JOIN/LEAVE and idle close: Tasks 5, 8, 11.
- Notification page mixed display: Tasks 6 and 10.
- Service boundaries: Tasks 3, 4, and 6 keep user aggregation in facade.
- Deployment: Task 7.

Placeholder scan:

- The plan does not use open-ended implementation placeholders.
- Code snippets define exact classes, fields, methods, routes, and commands.

Type consistency:

- Backend DTO fields match frontend type names: `conversationId`, `messageType`, `imageUrl`, `originalImageUrl`, `unreadCount`.
- Notification target uses `targetType = CONVERSATION` and `targetId = conversationId` throughout.
- WebSocket commands use `JOIN_CONVERSATION`, `LEAVE_CONVERSATION`, and `MESSAGE` consistently.
