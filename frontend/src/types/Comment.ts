export interface Comment {
  commentId: string;
  alertId: string;
  author: string;
  authorRole: string;
  content: string;
  createdAt: string;
}

export interface CommentRequest {
  content: string;
}
