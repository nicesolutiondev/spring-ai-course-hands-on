package hn.chatbot.service.topic.model;

import java.util.List;

public record TopicStories(String techField, int total, List<StorySummary> stories) {
}
