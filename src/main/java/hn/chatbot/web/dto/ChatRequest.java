package hn.chatbot.web.dto;

/**
 * conversationId 는 필수다. ChatMemory.CONVERSATION_ID 에 기본값이
 * 없어 누락하면 예외가 난다. 프론트가 세션 시작 시 UUID 를 만들어 유지하고 매 요청에 싣는다.
 *
 * DB 의 conversation_id 가 VARCHAR(36) 이라 UUID 여야 한다.
 */
public record ChatRequest(String conversationId, String question) {
}
