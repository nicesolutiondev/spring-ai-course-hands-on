package hn.chatbot.ai;

import java.util.List;

/**
 * 마스킹할 구간. LLM 이 원문을 다시 쓰게 하면 내용이 변조되므로
 * 치환할 구간만 받아 애플리케이션이 치환한다.
 *
 * harmfulPhrases 는 null 이 아니다. 유해 구간이 없으면 빈 목록이다.
 * 부르는 쪽이 바로 순회한다.
 */
public record MaskingResult(List<String> harmfulPhrases) {
}
