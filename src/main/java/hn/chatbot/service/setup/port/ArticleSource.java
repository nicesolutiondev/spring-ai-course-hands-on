package hn.chatbot.service.setup.port;

import hn.chatbot.service.setup.model.FetchedArticle;

/**
 * 원문을 가져오는 포트. 조회와 기계적 정제까지가 어댑터의 몫이다.
 *
 * 실패해도 예외를 던지지 않는다. 조회 실패는 파이프라인이 기록할 사실이지
 * 흐름을 끊을 사건이 아니다.
 */
public interface ArticleSource {

    FetchedArticle fetch(String url);
}
