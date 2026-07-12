package com.carrierfraud.application;

import com.carrierfraud.domain.AlertRead;
import com.carrierfraud.domain.Comment;
import com.carrierfraud.infrastructure.AlertReadRepository;
import com.carrierfraud.infrastructure.CommentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertReadService {

    private final AlertReadRepository alertReadRepository;
    private final CommentRepository commentRepository;

    public AlertReadService(AlertReadRepository alertReadRepository,
                            CommentRepository commentRepository) {
        this.alertReadRepository = alertReadRepository;
        this.commentRepository = commentRepository;
    }

    public void markAsRead(String username, String alertId) {
        LocalDateTime now = LocalDateTime.now();
        java.util.List<AlertRead> reads = alertReadRepository.findAllByUsernameAndAlertId(username, alertId);
        if (reads.isEmpty()) {
            alertReadRepository.save(new AlertRead(username, alertId, now));
            return;
        }
        AlertRead first = reads.get(0);
        first.touch(now);
        alertReadRepository.save(first);
        for (int i = 1; i < reads.size(); i++) {
            alertReadRepository.delete(reads.get(i));
        }
    }

    public Map<String, Long> unreadCounts(String username, List<String> alertIds) {
        Map<String, LocalDateTime> lastReadMap = new HashMap<>();
        alertReadRepository.findByUsername(username)
                .forEach(read -> lastReadMap.put(read.getAlertId(), read.getLastReadAt()));

        Map<String, Long> result = new HashMap<>();
        for (String alertId : alertIds) {
            LocalDateTime lastRead = lastReadMap.get(alertId);
            List<Comment> comments = commentRepository.findByAlertIdOrderByCreatedAtAsc(alertId);
            long unread = comments.stream()
                    .filter(c -> !c.getAuthor().equals(username))
                    .filter(c -> lastRead == null || c.getCreatedAt().isAfter(lastRead))
                    .count();
            result.put(alertId, unread);
        }
        return result;
    }
}
