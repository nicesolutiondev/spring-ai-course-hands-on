package hn.chatbot.web.dto;

import java.util.List;

/**
 * 목록에 뿌리는 스토리 한 건. 브라우저만 읽는다.
 *
 * listStories 도구는 이것을 반환하지 않는다. service/chat 의 StorySummary 를 반환한다.
 * url 에서 파생한 표시값 domain 은 화면에만 필요하고 모델이 쓸 데가 없다.
 *
 * 목록 단계에서 communityReaction 을 함께 내린다. 화면이 카드마다 요약을
 * 노출하므로 N+1 호출을 막는다.
 */
public record StoryBrief(
        long storyId, String title, String url, String domain,
        int score, int commentCount, String category,
        List<String> keywords, String communityReaction) {
}
