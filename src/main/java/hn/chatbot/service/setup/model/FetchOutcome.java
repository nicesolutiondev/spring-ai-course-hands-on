package hn.chatbot.service.setup.model;

/**
 * 원문 조회 결과의 성격. HTTP 상태 코드가 아니라 파이프라인이 판단에 쓰는 값이다.
 *
 * 길이 판정(2,000자 하한 · 100,000자 상한)은 여기 없다. 그것은 수집처와 무관한
 * 우리 정책이므로 서비스 쪽 PipelinePolicy.check 가 정한다.
 */
public enum FetchOutcome {

    /** 본문 텍스트를 받았다. 길이 판정은 PipelinePolicy.check 가 한다. */
    OK,
    /** PDF 바이너리다. */
    PDF,
    /** 조회 자체가 실패했다. */
    FAILED
}
