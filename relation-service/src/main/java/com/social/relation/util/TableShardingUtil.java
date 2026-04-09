package com.social.relation.util;

public class TableShardingUtil {

    private static final int SHARD_COUNT = 16;

    private TableShardingUtil() {
    }

    /**
     * Get sharding table index based on follower ID.
     * Used for following_N tables where follower_id is the sharding key.
     */
    public static int getFollowingTableIndex(Long followerId) {
        return (int) (followerId % SHARD_COUNT);
    }

    /**
     * Get sharding table index based on following ID.
     * Used for followers_N tables where following_id is the sharding key.
     */
    public static int getFollowersTableIndex(Long followingId) {
        return (int) (followingId % SHARD_COUNT);
    }

    /**
     * Get the actual table name for following table based on follower ID.
     */
    public static String getFollowingTableName(Long followerId) {
        return "following_" + getFollowingTableIndex(followerId);
    }

    /**
     * Get the actual table name for followers table based on following ID.
     */
    public static String getFollowersTableName(Long followingId) {
        return "followers_" + getFollowersTableIndex(followingId);
    }
}
