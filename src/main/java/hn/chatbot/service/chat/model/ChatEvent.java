package hn.chatbot.service.chat.model;

import java.util.List;

/**
 * Q&A 한 턴에서 흘러나오는 사건들. 순서는 plans → evidence → token 반복 → done 이다.
 *
 * 서비스는 이것만 흘린다. SSE 로 어떻게 실어 보낼지는 web/ 이 정한다.
 * 데이터가 없을 때는 Token 과 Completed 만 흐르고 Plans · Evidence 는 나오지 않는다.
 */
public sealed interface ChatEvent {

    /** 검색 계획 4개의 히트 수 · 실행 조건과 중복 제거 후 후보 수. */
    record Plans(List<PlanOutcome> plans, int merged) implements ChatEvent {
    }

    record PlanOutcome(int plan, String name, int hits, String condition) {
    }

    /** 적합성 판단으로 추려낸 최종 근거. */
    record Evidence(int selected, List<EvidenceItem> items) implements ChatEvent {
    }

    /** passage 는 적합성 판단이 원문 앞부분에서 뽑은 대목이다. 없을 수 있다. */
    record EvidenceItem(long storyId, String title, String url,
                        List<Integer> matchedPlans, String category, String summary,
                        String communityReaction, String practicalImplication,
                        String passage) {
    }

    record Token(String text) implements ChatEvent {
    }

    record Completed(String finishReason) implements ChatEvent {
    }
}
