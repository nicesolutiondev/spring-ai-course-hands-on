package hn.chatbot.service.chat;

/** getComments 도구의 반환 타입. text 는 마스킹을 거친 뒤의 값이다. */
public record CommentView(long commentId, String by, int depth, String text, boolean moderationFlagged) {
}
