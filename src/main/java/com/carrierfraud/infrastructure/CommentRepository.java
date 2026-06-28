package com.carrierfraud.infrastructure;

import com.carrierfraud.domain.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByAlertIdOrderByCreatedAtAsc(String alertId);
}
