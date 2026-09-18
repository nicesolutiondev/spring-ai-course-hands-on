package hn.chatbot.service.setup.model;

/** 원문 조회 결과. text 는 태그를 걷어낸 정제 텍스트다. */
public record FetchedArticle(String url, FetchOutcome outcome, String text) {
}
