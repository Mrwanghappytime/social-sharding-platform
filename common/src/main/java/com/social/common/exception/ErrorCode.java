package com.social.common.exception;

public class ErrorCode {

    private ErrorCode() {}

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int INTERNAL_ERROR = 500;

    // User errors (1000-1999)
    public static final int USER_NOT_FOUND = 1001;
    public static final int USERNAME_EXISTS = 1002;
    public static final int INVALID_CREDENTIALS = 1003;

    // Post errors (2000-2999)
    public static final int POST_NOT_FOUND = 2001;
    public static final int MEDIA_TYPE_MISMATCH = 2002;
    public static final int IMAGE_COUNT_EXCEEDED = 2003;
    public static final int VIDEO_SIZE_EXCEEDED = 2004;

    // Interaction errors (3000-3999)
    public static final int ALREADY_LIKED = 3001;
    public static final int NOT_LIKED = 3002;
    public static final int COMMENT_NOT_FOUND = 3003;

    // Relation errors (4000-4999)
    public static final int CANNOT_FOLLOW_SELF = 4001;
    public static final int ALREADY_FOLLOWING = 4002;
    public static final int NOT_FOLLOWING = 4003;

    // File errors (5000-5999)
    public static final int INVALID_FILE_TYPE = 5001;
    public static final int FILE_TOO_LARGE = 5002;
    public static final int FILE_NOT_FOUND = 5003;
}
