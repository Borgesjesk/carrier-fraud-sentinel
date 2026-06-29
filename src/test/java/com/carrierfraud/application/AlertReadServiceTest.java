package com.carrierfraud.application;

import com.carrierfraud.domain.AlertRead;
import com.carrierfraud.domain.Comment;
import com.carrierfraud.infrastructure.AlertReadRepository;
import com.carrierfraud.infrastructure.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertReadServiceTest {

    @Mock private AlertReadRepository alertReadRepository;
    @Mock private CommentRepository commentRepository;

    private AlertReadService service;

    @BeforeEach
    void setUp() {
        service = new AlertReadService(alertReadRepository, commentRepository);
    }

    @Test
    void markAsRead_existingRecord_updatesTimestamp() {
        AlertRead existing = new AlertRead("client1", "A1", LocalDateTime.now().minusDays(1));
        when(alertReadRepository.findByUsernameAndAlertId("client1", "A1"))
                .thenReturn(Optional.of(existing));

        service.markAsRead("client1", "A1");

        verify(alertReadRepository).save(existing);
    }

    @Test
    void markAsRead_newRecord_createsAlertRead() {
        when(alertReadRepository.findByUsernameAndAlertId("client1", "A2"))
                .thenReturn(Optional.empty());

        service.markAsRead("client1", "A2");

        verify(alertReadRepository).save(any(AlertRead.class));
    }

    @Test
    void unreadCounts_excludesCommentsAuthoredByUser() {
        when(alertReadRepository.findByUsername("client1")).thenReturn(List.of());

        Comment ownComment = new Comment("A1", "client1", "CLIENT", "my comment");
        Comment otherComment = new Comment("A1", "admin", "ADMIN", "from staff");

        when(commentRepository.findByAlertIdOrderByCreatedAtAsc("A1"))
                .thenReturn(List.of(ownComment, otherComment));

        Map<String, Long> result = service.unreadCounts("client1", List.of("A1"));

        assertThat(result.get("A1")).isEqualTo(1L);
    }

    @Test
    void unreadCounts_afterLastReadAt_countsOnlyNewer() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        AlertRead read = new AlertRead("client1", "A1", cutoff);
        when(alertReadRepository.findByUsername("client1")).thenReturn(List.of(read));

        Comment old = new Comment("A1", "admin", "ADMIN", "old");
        Comment fresh = new Comment("A1", "admin", "ADMIN", "fresh");

        when(commentRepository.findByAlertIdOrderByCreatedAtAsc("A1"))
                .thenReturn(List.of(old, fresh));

        Map<String, Long> result = service.unreadCounts("client1", List.of("A1"));

        assertThat(result.get("A1")).isEqualTo(2L);
    }
}