package com.social.common.api;

import com.social.common.dto.PostDTO;
import java.util.List;

public interface PostService {

    PostDTO getPostById(Long postId);

    boolean isPostExists(Long postId);

    Long getUserIdByPostId(Long postId);

    void incrementLikeCount(Long postId);

    void decrementLikeCount(Long postId);

    void incrementCommentCount(Long postId);

    void decrementCommentCount(Long postId);

    boolean hasUserLikedPost(Long postId, Long userId);
}
