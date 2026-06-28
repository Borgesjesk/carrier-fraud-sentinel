package com.carrierfraud.api;

import com.carrierfraud.domain.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        String commentId,
        String alertId,
        String author,
        String authorRole,
        String content,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getCommentId(),
                comment.getAlertId(),
                comment.getAuthor(),
                comment.getAuthorRole(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
