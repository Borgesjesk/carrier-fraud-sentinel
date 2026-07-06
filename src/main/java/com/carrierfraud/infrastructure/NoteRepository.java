package com.carrierfraud.infrastructure;

import com.carrierfraud.domain.Note;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends MongoRepository<Note, String> {
    List<Note> findByAlertIdOrderByCreatedAtAsc(String alertId);
}
