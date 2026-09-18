package hn.chatbot.service.chat;

import java.util.List;

/** getStoryDetail 도구의 반환 타입. */
public record StoryDetail(
        long storyId, String title, String url,
        String summary, String communityReaction, String practicalImplication,
        String techField, String category, List<String> keywords) {
}
