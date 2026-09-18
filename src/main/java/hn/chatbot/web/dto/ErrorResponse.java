package hn.chatbot.web.dto;

/** 오류 응답. 스트림 중 오류는 SSE 의 error 이벤트로 이 형태를 보낸다. */
public record ErrorResponse(String error, String detail) {
}
