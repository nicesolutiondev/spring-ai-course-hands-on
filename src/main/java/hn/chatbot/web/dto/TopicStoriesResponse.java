package hn.chatbot.web.dto;

import java.util.List;

public record TopicStoriesResponse(String techField, int total, List<StoryBrief> stories) {
}
