package hn.chatbot.service.setup;

/** 스토리 하나의 처리 결과. 뼈대가 진행 상황의 완료 · 제외 건수로 옮긴다. */
public enum StoryOutcome {
    /** 8단계까지 적재했다. */
    COMPLETED,
    /** 정제 · 본문 추출 · 적합성 판정에서 제외했다. 원본과 사유는 저장했다. */
    EXCLUDED,
    /** 이미 처리한 스토리라 건너뛰었다. */
    SKIPPED
}
