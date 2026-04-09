package com.social.common.constant;

public class RedisKeys {

    private RedisKeys() {}

    // User session
    public static String userSession(Long userId) {
        return "user:session:" + userId;
    }

    // User online status
    public static String userOnline(Long userId) {
        return "user:online:" + userId;
    }

    // Notification channel
    public static String notificationChannel(Long userId) {
        return "notification:channel:" + userId;
    }

    // KOL threshold key
    public static String kolThreshold() {
        return "config:kol:threshold";
    }

    // Post like count
    public static String postLikeCount(Long postId) {
        return "post:like:count:" + postId;
    }

    // User follower count
    public static String userFollowerCount(Long userId) {
        return "user:follower:count:" + userId;
    }

    // User following count
    public static String userFollowingCount(Long userId) {
        return "user:following:count:" + userId;
    }
}
