package com.carrierfraud.api;

import com.carrierfraud.domain.Note;

import java.time.LocalDateTime;

public record NoteResponse(
        String noteId,
        String alertId,
        String author,
        String authorRole,
        String content,
        LocalDateTime createdAt
) {
    public static NoteResponse from(Note note) {
        return new NoteResponse(
                note.getNoteId(),
                note.getAlertId(),
                note.getAuthor(),
                note.getAuthorRole(),
                note.getContent(),
                note.getCreatedAt()
        );
    }
}
