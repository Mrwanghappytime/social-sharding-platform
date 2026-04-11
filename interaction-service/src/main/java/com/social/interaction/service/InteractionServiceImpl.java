package com.social.interaction.service;

import com.social.common.api.InteractionService;
import com.social.common.dto.CommentDTO;
import com.social.common.dto.LikeStatusDTO;
import com.social.common.entity.Comment;
import com.social.common.entity.Like;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.CommentRepository;
import com.social.common.repository.LikeRepository;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@DubboService(interfaceClass = InteractionService.class, version = "1.0.0")
@Service
public class InteractionServiceImpl implements InteractionService {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Override
    @Transactional
    public void likePost(Long postId, Long userId) {
        Like like = new Like();
        like.setPostId(postId);
        like.setUserId(userId);

        try {
            likeRepository.save(like);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED, "已经点赞过该动态");
        }
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long userId) {
        Optional<Like> likeOpt = likeRepository.findByUserIdAndPostId(userId, postId);
        if (likeOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_LIKED, "未点赞该动态");
        }
        likeRepository.delete(likeOpt.get());
    }

    @Override
    public LikeStatusDTO getLikeStatus(Long postId, Long userId) {
        long likeCount = likeRepository.countByPostId(postId);
        boolean userLiked = likeRepository.existsByUserIdAndPostId(userId, postId).isPresent();

        LikeStatusDTO dto = new LikeStatusDTO();
        dto.setLiked(userLiked);
        dto.setLikeCount(likeCount);

        return dto;
    }

    @Override
    @Transactional
    public CommentDTO commentOnPost(Long postId, Long userId, String content) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);

        Comment savedComment = commentRepository.save(comment);

        return toCommentDTO(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "评论不存在"));

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该评论");
        }

        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentDTO> getComments(Long postId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findByPostId(postId, pageRequest);

        return commentPage.getContent().stream()
                .map(this::toCommentDTO)
                .toList();
    }

    private CommentDTO toCommentDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPostId());
        dto.setUserId(comment.getUserId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
}
