package hn.chatbot.search;

/**
 * 계획 하나의 결과 한 건.
 *
 * score 의 척도는 계획마다 다르다. 계획 1 은 점수가 없고, 계획 2 는 ts_rank,
 * 계획 3·4 는 코사인 거리다. 서로 비교하거나 합치지 않는다.
 * 최종 순위는 LLM 적합성 판단이 정한다.
 */
public record PlanHit(long storyId, double score) {
}
