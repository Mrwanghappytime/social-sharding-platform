package com.social.common.api;

import com.social.common.dto.PostDTO;
import com.social.common.dto.PageResult;
import com.social.common.enums.PostType;

import java.util.List;

public interface PostService {

    PostDTO getPostById(Long postId);

    boolean isPostExists(Long postId);

    Long getUserIdByPostId(Long postId);

    void incrementLikeCount(Long postId);

    void decrementLikeCount(Long postId);

    void incrementCommentCount(Long postId);

    void decrementCommentCount(Long postId);

    PostDTO createPost(Long userId, String title, String content, PostType type, String imageUrls, String videoUrl);

    void deletePost(Long postId, Long userId);

    PageResult<PostDTO> getUserPosts(Long userId, Integer page, Integer size);

    PageResult<PostDTO> getFeed(Integer page, Integer size);

    PageResult<PostDTO> searchPosts(String keyword, Integer page, Integer size);
}
