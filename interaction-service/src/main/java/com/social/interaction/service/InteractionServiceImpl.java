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
import com.social.common.util.LogUtil;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InteractionServiceImpl.class);

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Override
    @Transactional
    public void likePost(Long postId, Long userId) {
        log.info(">>> likePost ENTER | postId={} | userId={}", postId, userId);
        try {
            Like like = new Like();
            like.setPostId(postId);
            like.setUserId(userId);

            try {
                likeRepository.save(like);
                log.info("<<< likePost EXIT | postId={} | userId={} | traceId={}", postId, userId, LogUtil.getTraceId());
            } catch (DataIntegrityViolationException e) {
                throw new BusinessException(ErrorCode.ALREADY_LIKED, "已经点赞过该动态");
            }
        } catch (Exception e) {
            log.error("!!! likePost ERROR | postId={} | userId={} | error={}", postId, userId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long userId) {
        log.info(">>> unlikePost ENTER | postId={} | userId={}", postId, userId);
        try {
            Optional<Like> likeOpt = likeRepository.findByUserIdAndPostId(userId, postId);
            if (likeOpt.isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_LIKED, "未点赞该动态");
            }
            likeRepository.delete(likeOpt.get());
            log.info("<<< unlikePost EXIT | postId={} | userId={} | traceId={}", postId, userId, LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! unlikePost ERROR | postId={} | userId={} | error={}", postId, userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public LikeStatusDTO getLikeStatus(Long postId, Long userId) {
        log.debug(">>> getLikeStatus ENTER | postId={} | userId={}", postId, userId);
        try {
            long likeCount = likeRepository.countByPostId(postId);
            boolean userLiked = likeRepository.existsByUserIdAndPostId(userId, postId).isPresent();

            LikeStatusDTO dto = new LikeStatusDTO();
            dto.setLiked(userLiked);
            dto.setLikeCount(likeCount);

            log.debug("<<< getLikeStatus EXIT | postId={} | userId={} | liked={} | count={} | traceId={}",
                    postId, userId, userLiked, likeCount, LogUtil.getTraceId());
            return dto;
        } catch (Exception e) {
            log.error("!!! getLikeStatus ERROR | postId={} | userId={} | error={}", postId, userId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public CommentDTO commentOnPost(Long postId, Long userId, String content) {
        log.info(">>> commentOnPost ENTER | postId={} | userId={} | contentLength={}", postId, userId, content != null ? content.length() : 0);
        try {
            Comment comment = new Comment();
            comment.setPostId(postId);
            comment.setUserId(userId);
            comment.setContent(content);

            Comment savedComment = commentRepository.save(comment);

            CommentDTO result = toCommentDTO(savedComment);
            log.info("<<< commentOnPost EXIT | postId={} | userId={} | commentId={} | traceId={}",
                    postId, userId, result.getId(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! commentOnPost ERROR | postId={} | userId={} | error={}", postId, userId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.info(">>> deleteComment ENTER | commentId={} | userId={}", commentId, userId);
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "评论不存在"));

            if (!comment.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该评论");
            }

            commentRepository.deleteById(commentId);
            log.info("<<< deleteComment EXIT | commentId={} | userId={} | traceId={}", commentId, userId, LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! deleteComment ERROR | commentId={} | userId={} | error={}", commentId, userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<CommentDTO> getComments(Long postId, Integer page, Integer size) {
        log.debug(">>> getComments ENTER | postId={} | page={} | size={}", postId, page, size);
        try {
            PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "createdAt"));
            Page<Comment> commentPage = commentRepository.findByPostId(postId, pageRequest);

            List<CommentDTO> result = commentPage.getContent().stream()
                    .map(this::toCommentDTO)
                    .toList();
            log.debug("<<< getComments EXIT | postId={} | total={} | traceId={}", postId, commentPage.getTotalElements(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! getComments ERROR | postId={} | error={}", postId, e.getMessage());
            throw e;
        }
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
