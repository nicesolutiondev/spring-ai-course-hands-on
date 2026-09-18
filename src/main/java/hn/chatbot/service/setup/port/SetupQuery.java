package hn.chatbot.service.setup.port;

import hn.chatbot.service.setup.model.Exclusions;

/** 진행 상황 산출과 중복 확인에 필요한 조회 포트. */
public interface SetupQuery {

    /**
     * 이미 처리한 스토리인가. article 행의 존재 여부가 기준이며,
     * 성공·실패와 무관하게 있으면 이후 단계를 건너뛴다.
     */
    boolean isProcessed(long storyId);

    /** 검색 대상 스토리 수. SELECT count(*) FROM analysis WHERE suitable */
    int countSearchableStories();

    /** 기계적 정제와 LLM 판정으로 제외된 것의 사유별 집계. */
    Exclusions exclusions();
}
