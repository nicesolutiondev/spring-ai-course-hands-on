package hn.chatbot.service.setup;

import hn.chatbot.domain.ArticleStatus;
import hn.chatbot.service.setup.model.ArticleCheck;
import hn.chatbot.service.setup.model.FetchedArticle;

/** 파이프라인의 정책 값과 4단계 판정. 완성본이다. */
public final class PipelinePolicy {

    /** 정제 후 이 길이 이하면 제외한다(JS 렌더링 · 조회 실패). */
    public static final int MIN_LENGTH = 2_000;

    /** 이 길이를 넘는 부분은 절단한다. 실측 최대가 89,679자라 실제로는 방어선이다. */
    public static final int MAX_LENGTH = 100_000;

    /** 분석에 함께 넘기는 최상위 댓글 수. */
    public static final int TOP_COMMENTS = 5;

    /** 유해 구간을 치환할 문자열. */
    public static final String MASK = "***";

    private PipelinePolicy() {
    }

    /**
     * 4단계 기계적 정제의 판정. 조회 결과를 article 상태와 저장할 정제 텍스트로 바꾼다.
     *
     * - 조회 실패 → FETCH_FAILED, PDF → PDF
     * - 정제 텍스트가 MIN_LENGTH 이하 → TOO_SHORT
     * - 그 외 → PASS. MAX_LENGTH 초과분은 잘라서 돌려준다
     */
    public static ArticleCheck check(FetchedArticle fetched) {
        switch (fetched.outcome()) {
            case FAILED:
                return new ArticleCheck(ArticleStatus.FETCH_FAILED, null);
            case PDF:
                return new ArticleCheck(ArticleStatus.PDF, null);
            default:
                break;
        }
        String text = fetched.text() == null ? "" : fetched.text();
        if (text.length() <= MIN_LENGTH) {
            return new ArticleCheck(ArticleStatus.TOO_SHORT, text);
        }
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH);
        }
        return new ArticleCheck(ArticleStatus.PASS, text);
    }
}
