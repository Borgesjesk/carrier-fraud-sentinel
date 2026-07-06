export interface Note {
  noteId: string;
  alertId: string;
  author: string;
  authorRole: string;
  content: string;
  createdAt: string;
}

export interface NoteRequest {
  content: string;
}
