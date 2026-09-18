package hn.chatbot.service.setup.model;

import hn.chatbot.domain.ArticleStatus;

/**
 * 4단계 판정 결과. PipelinePolicy.check 가 만든다.
 *
 * text 는 article.cleaned_text 에 저장할 정제 텍스트다. 절단이 이미 적용돼 있고,
 * 조회 실패와 PDF 는 null 이다.
 */
public record ArticleCheck(ArticleStatus status, String text) {

    /** 5단계로 넘어가도 되는가. */
    public boolean passed() {
        return status == ArticleStatus.PASS;
    }

    /** article.cleaned_len 에 저장할 길이. text 가 없으면 null. */
    public Integer length() {
        return text == null ? null : text.length();
    }
}
