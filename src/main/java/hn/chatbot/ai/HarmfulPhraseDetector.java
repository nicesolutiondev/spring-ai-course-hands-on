package hn.chatbot.ai;

/**
 * 파이프라인 3단계 — 검열에 걸린 댓글에서 유해 구간을 추출한다.
 *
 * omni-moderation-latest 는 어느 구간이 문제인지 알려주지 않으므로
 * 부분 마스킹에는 LLM 이 필요하다.
 *
 * 유해 구간이 없으면 빈 목록을 담아 돌려준다. 구조화 출력은 모델이 필드를 빼면
 * null 을 주므로, 컨버터가 준 값을 그대로 넘기지 말고 정규화한다.
 */
public interface HarmfulPhraseDetector {

    MaskingResult detect(String text);
}
