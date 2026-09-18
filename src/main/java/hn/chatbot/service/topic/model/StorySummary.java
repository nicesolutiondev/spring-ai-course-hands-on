package hn.chatbot.service.topic.model;

import java.util.List;

/**
 * 목록에 뿌리는 스토리 한 건. listStories 도구도 이것을 반환한다.
 *
 * url 에서 파생한 표시값 domain 은 여기 없다. web/dto 의 StoryBrief 에만 있다.
 */
public record StorySummary(long storyId, String title, String url, String summary,
                           int score, int commentCount, String category,
                           List<String> keywords, String communityReaction) {
}
