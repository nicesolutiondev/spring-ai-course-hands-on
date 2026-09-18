package hn.chatbot.domain;

/** 원문 처리 결과. DB 의 article_status ENUM 과 값이 일치한다. */
public enum ArticleStatus {

    /** 본문 확보 */
    PASS,
    /** %PDF 시그니처 */
    PDF,
    /** 정제 후 2000자 이하 (JS 렌더링 등) */
    TOO_SHORT,
    /** 조회 실패 */
    FETCH_FAILED,
    /** LLM 이 본문을 찾지 못함 */
    NO_BODY
}
