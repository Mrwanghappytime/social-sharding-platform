# Private Message Design

Date: 2026-06-28

## Goal

Add first-class private messaging to the social platform. A user can open another user's profile, click a private-message button, and exchange text or image messages. Private message reminders appear in the existing notification page as a `MESSAGE` notification. A conversation reuses one notification record: new messages in the same conversation update that notification instead of creating one notification per message.

## Scope

Included in the first version:

- A new `message-service` for one-to-one conversations and messages.
- A private-message button on other users' profile pages.
- Text messages.
- Image messages.
- Default client-side image compression.
- Optional original-image sending.
- A notification-page inbox item for private messages.
- One `MESSAGE` notification per recipient and conversation.
- A reusable private-message WebSocket that joins the active conversation and closes after an idle timeout.
- Conversation/message read state and unread counts.

Excluded from the first version:

- Group chat.
- Message recall.
- Read receipts shown to the sender.
- Typing indicators.
- Blocking/reporting.
- Sensitive-word filtering.
- Image moderation.
- Follow-only or mutual-follow messaging restrictions.

## Architecture

Use a new independent `message-service`.

Service responsibilities:

- `message-service`
  - Owns conversations, messages, conversation unread state, and private-message WebSocket delivery.
  - Does not aggregate user names or avatars.
  - Does not own notification-list display.

- `notification-service`
  - Owns normal notifications and `MESSAGE` notification reminders.
  - Does not store or query concrete private-message content.
  - Provides upsert/read methods for conversation notifications.

- `facade-service`
  - Exposes HTTP APIs to the frontend.
  - Calls `message-service` for conversations/messages.
  - Calls `notification-service` to upsert/read `MESSAGE` notifications.
  - Calls `user-service` to enrich response DTOs with names and avatars.
  - Aggregates notification records with post titles or conversation summaries.

- `gateway`
  - Keeps `/ws/notify -> notification-service`.
  - Adds `/ws/message -> message-service`.

This preserves the current microservice principle: business services return their own data, and facade performs cross-service aggregation.

## Notification Model

Add `MESSAGE` to `NotificationType`.

A private-message notification uses:

- `type = MESSAGE`
- `targetType = CONVERSATION`
- `targetId = conversationId`
- `recipientId = message receiver`
- `actorId = last sender`
- `actorUsername = last sender username snapshot`
- `actorAvatar = last sender avatar snapshot`

A conversation reuses one notification per recipient.

When user A sends messages to user B in conversation C:

1. If B has no existing `MESSAGE + CONVERSATION + C` notification, create one.
2. If it exists, update it:
   - actor fields become the latest sender.
   - `isRead = false`.
   - `updatedAt = now`.
3. Sort notifications by `updatedAt DESC`, so reused message notifications return to the top.

Clicking this notification opens:

```text
/messages/conversations/{conversationId}
```

Entering the chat marks both the conversation messages and corresponding `MESSAGE` notification as read.

## Data Model

### `conversations`

One row per one-to-one pair.

```sql
CREATE TABLE conversations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user1_id BIGINT NOT NULL,
  user2_id BIGINT NOT NULL,
  last_message_id BIGINT NULL,
  last_message_type VARCHAR(20) NULL,
  last_message_preview VARCHAR(255) NULL,
  last_message_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_conversation_users (user1_id, user2_id),
  KEY idx_user1_updated (user1_id, updated_at),
  KEY idx_user2_updated (user2_id, updated_at)
);
```

`user1_id` is always the smaller user id and `user2_id` is always the larger user id. This guarantees A-B and B-A map to the same conversation.

### `messages`

```sql
CREATE TABLE messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  message_type VARCHAR(20) NOT NULL,
  content VARCHAR(2000) NULL,
  image_url VARCHAR(500) NULL,
  original_image_url VARCHAR(500) NULL,
  is_read BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_conversation_created (conversation_id, created_at),
  KEY idx_receiver_read (receiver_id, is_read),
  KEY idx_sender_receiver_created (sender_id, receiver_id, created_at)
);
```

Supported message types:

- `TEXT`
- `IMAGE`

For image messages:

- `image_url` stores the compressed image used in chat bubbles.
- `original_image_url` stores the original image when the user chooses original-image sending.
- If original-image sending is not chosen, `original_image_url` is null.

## Common API and DTOs

Add `MessageService`:

```java
public interface MessageService {
    ConversationDTO getOrCreateConversation(Long currentUserId, Long targetUserId);

    MessageDTO sendTextMessage(Long senderId, Long conversationId, String content);

    MessageDTO sendImageMessage(Long senderId, Long conversationId, String imageUrl, String originalImageUrl);

    PageResult<MessageDTO> getMessages(Long conversationId, Long currentUserId, Integer page, Integer size);

    List<ConversationDTO> getConversationsByIds(List<Long> conversationIds, Long currentUserId);

    void markConversationAsRead(Long conversationId, Long currentUserId);

    Long getUnreadMessageCount(Long currentUserId);
}
```

Add `ConversationDTO`:

```java
private Long id;
private Long user1Id;
private Long user2Id;
private Long peerUserId;
private Long lastMessageId;
private String lastMessageType;
private String lastMessagePreview;
private LocalDateTime lastMessageAt;
private Long unreadCount;
```

Add `MessageDTO`:

```java
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
```

Extend `NotificationService`:

```java
void upsertConversationNotification(
    Long recipientId,
    Long actorId,
    Long conversationId,
    String actorUsername,
    String actorAvatar
);

void markConversationNotificationAsRead(Long recipientId, Long conversationId);
```

Add repository support for:

```java
Optional<Notification> findByRecipientIdAndTypeAndTargetTypeAndTargetId(
    Long recipientId,
    NotificationType type,
    String targetType,
    Long targetId
);
```

## Facade HTTP APIs

### Create or get a conversation

```http
POST /api/messages/conversations

{
  "targetUserId": 123
}
```

Rules:

- Login required.
- Target user must exist.
- Target user cannot be the current user.
- Any logged-in user can message any other user.

Response includes peer user data enriched by facade.

### Get messages

```http
GET /api/messages/conversations/{conversationId}/messages?page=1&size=30
```

Only participants can query messages.

### Send text message

```http
POST /api/messages/conversations/{conversationId}/messages/text

{
  "content": "你好"
}
```

Facade flow:

1. Call `messageService.sendTextMessage`.
2. Load sender profile from `user-service`.
3. Call `notificationService.upsertConversationNotification` for the receiver.
4. Return the saved message.

### Send image message

```http
POST /api/messages/conversations/{conversationId}/messages/image

{
  "imageUrl": "/files/images/compressed.jpg",
  "originalImageUrl": "/files/images/original.jpg"
}
```

Facade uses the same post-send notification flow as text messages.

### Mark conversation as read

```http
PUT /api/messages/conversations/{conversationId}/read
```

Facade flow:

1. `messageService.markConversationAsRead(conversationId, currentUserId)`.
2. `notificationService.markConversationNotificationAsRead(currentUserId, conversationId)`.

## Notification Page Aggregation

The notification page continues to call:

```http
GET /api/notifications
```

Facade behavior:

1. Load notification records from `notification-service`.
2. For `POST` targets, batch query `post-service` for titles.
3. For `CONVERSATION` targets, batch query `message-service` for conversation summaries.
4. Enrich user names/avatars through `user-service` where needed.
5. Return unified notification items.

A `MESSAGE` item includes conversation summary data:

```json
{
  "id": 88,
  "type": "MESSAGE",
  "actorId": 12,
  "actorUsername": "李四",
  "actorAvatar": "/files/images/a.png",
  "targetId": 10001,
  "targetType": "CONVERSATION",
  "conversation": {
    "id": 10001,
    "peerUserId": 12,
    "peerUsername": "李四",
    "peerAvatar": "/files/images/a.png",
    "lastMessageType": "IMAGE",
    "lastMessagePreview": "[图片]",
    "unreadCount": 3
  },
  "isRead": false,
  "createdAt": "...",
  "updatedAt": "..."
}
```

Frontend behavior:

- `LIKE/COMMENT + POST`: open post detail.
- `FOLLOW + USER`: open user profile.
- `MESSAGE + CONVERSATION`: open chat detail.

## WebSocket Design

There are two WebSocket routes.

### Notification WebSocket

Existing long-lived route:

```text
/ws/notify
```

It remains connected after login and sends all notification reminders, including `MESSAGE` notifications.

### Private Message WebSocket

New reusable route:

```text
/ws/message
```

It is not connected immediately after login. It connects when the user first enters a chat page.

The private-message socket is user-level, not conversation-level. It is reused across chat pages.

Client commands:

```json
{ "type": "JOIN_CONVERSATION", "conversationId": 10001 }
```

```json
{ "type": "LEAVE_CONVERSATION", "conversationId": 10001 }
```

```json
{ "type": "ping" }
```

Server behavior:

- Authenticate through Gateway `X-User-Id`, with JWT query fallback for local debugging.
- On `JOIN_CONVERSATION`, verify the user is a participant.
- Maintain `userId -> session` and `session -> activeConversationId`.
- Push concrete `MessageDTO` only if the receiver's active conversation matches the message conversation.
- If the receiver is not in that conversation, do not push concrete content; the notification WebSocket handles the reminder.

Client lifecycle:

- Login: only `/ws/notify` connects.
- First chat page entry: connect `/ws/message`, then join the conversation.
- Switching conversations: reuse the same socket and send leave/join commands.
- Leaving chat pages: send leave and start a 5-minute idle close timer.
- Re-entering chat within 5 minutes: reuse the existing socket.
- Idle beyond 5 minutes: close `/ws/message`.
- If the socket drops while a conversation is active: reconnect and rejoin.
- If the socket drops while idle: do not reconnect.

## Frontend Pages and Stores

Add:

```text
frontend/src/api/message.ts
frontend/src/stores/message.ts
frontend/src/stores/messageSocket.ts
frontend/src/views/message/ChatDetailPage.vue
```

Modify:

```text
frontend/src/views/user/UserProfilePage.vue
frontend/src/router/index.ts
frontend/src/views/notification/NotificationPage.vue
frontend/src/components/notification/NotificationItem.vue
frontend/src/stores/notification.ts
```

Profile page:

- Non-own profile shows `私聊` next to the follow button.
- Clicking it creates/gets the conversation and routes to `/messages/conversations/{id}`.

Chat detail page:

- Shows peer user name/avatar.
- Loads paged history.
- Sends text with Enter, newline with Shift+Enter.
- Supports image selection.
- Shows an `原图` option.
- Displays compressed image in the bubble and opens `originalImageUrl || imageUrl` in preview.

Image sending:

- Default path: compress locally, upload compressed image, send image message.
- Original path: compress locally, upload compressed image and original image, then send image message with both URLs.
- Compression target: max side 1280px, quality 0.8, JPEG output for first version.

## Error Handling

- Cannot message yourself: return bad request.
- Target user missing: return user-not-found error.
- Non-participant conversation access: forbidden.
- Empty text message: bad request.
- Oversized text message: bad request.
- Missing image URL for image message: bad request.
- Image compression failure: frontend shows an error and does not send.
- Original-image upload failure when original mode is selected: frontend shows an error and does not send.
- Redis/WebSocket push failure after message persistence: log the failure; persisted messages remain the source of truth.
- Notification upsert failure in facade send flow: fail the send request in first version, so missing notification problems are visible during development.

## Acceptance Criteria

### Profile entry

- Other users' profile pages show a private-message button.
- Own profile does not show the button.
- Unauthenticated users are redirected to login.
- Clicking the button creates or gets the one-to-one conversation and opens the chat page.

### Text messages

- User A sends a text message to B.
- The message is persisted.
- The conversation last-message summary updates.
- B gets one `MESSAGE` notification.
- Multiple unread messages in the same conversation reuse the same notification.
- The notification returns to the top when new messages arrive.

### Image messages

- Default image sending uploads and sends a compressed image.
- Original-image mode uploads both compressed and original image.
- The chat bubble displays the compressed image.
- Image preview opens the original image when available.

### Notification page

- `MESSAGE` notifications appear alongside likes, comments, and follows.
- A message notification displays the peer and latest message summary.
- Clicking a message notification opens the chat detail page.
- Entering the chat page marks both the conversation messages and the notification as read.

### WebSocket behavior

- `/ws/notify` remains the global notification connection.
- `/ws/message` is created only after the user enters a chat page.
- Switching chat conversations does not recreate `/ws/message`; it sends leave/join commands.
- Leaving chat pages keeps `/ws/message` idle for 5 minutes.
- Re-entering chat within 5 minutes reuses the connection.
- Idle beyond 5 minutes closes the private-message connection.
- A user only receives concrete private-message payloads for the active joined conversation.
- If the user is not in the target conversation, they only receive a notification reminder.

### Service boundaries

- `message-service` does not call `user-service`.
- `message-service` does not aggregate user display data.
- `notification-service` does not store or query private-message content.
- `facade-service` performs cross-service aggregation.
