package hn.chatbot.ai;

/**
 * 적합성 판단이 고른 근거 하나.
 *
 * passage 는 그 스토리의 원문 앞부분에서 질문과 관련된 대목이다. 원문에 그대로 있는
 * 문장이어야 하고, 찾지 못하면 null 이다. 원문 포함 여부는 부르는 쪽의 완성본 코드가
 * 다시 확인한다.
 */
public record RelevantStory(long storyId, String passage) {
}
