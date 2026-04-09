package com.social.common.api;

import com.social.common.dto.CommentDTO;
import com.social.common.dto.LikeStatusDTO;

import java.util.List;

public interface InteractionService {

    void likePost(Long postId, Long userId);

    void unlikePost(Long postId, Long userId);

    LikeStatusDTO getLikeStatus(Long postId, Long userId);

    CommentDTO commentOnPost(Long postId, Long userId, String content);

    void deleteComment(Long commentId, Long userId);

    List<CommentDTO> getComments(Long postId, Integer page, Integer size);
}
