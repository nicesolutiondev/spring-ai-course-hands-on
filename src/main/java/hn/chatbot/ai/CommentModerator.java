package hn.chatbot.ai;

/**
 * 파이프라인 3단계 — ModerationModel 로 댓글의 카테고리별 점수를 받는다.
 *
 * 판정은 isFlagged() 가 아니라 ModerationProperties.categories() 에 적힌 카테고리의
 * 점수만 임계값과 비교해 낸다.
 *
 * 그 목록은 설정값이라 바뀔 수 있고, 모델이 채우지 않는 이름이 들어 있을 수 있다.
 * 점수를 찾지 못한 카테고리를 그대로 비교하면 판정 자체가 실패한다.
 */
public interface CommentModerator {

    ModerationVerdict inspect(String text);
}
