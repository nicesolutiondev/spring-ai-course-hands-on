package hn.chatbot.ai;

import java.util.Map;

/**
 * 댓글 검열 판정.
 *
 * isFlagged() 를 그대로 쓰지 않고 우리가 고른 카테고리의 점수를 임계값과 비교한다.
 * harassment 계열은 제외한다 — 기술 커뮤니티의 제품·기업 비판이 대량으로 걸린다.
 */
public record ModerationVerdict(boolean flagged, String topCategory, double topScore,
                                Map<String, Double> scores) {
}
