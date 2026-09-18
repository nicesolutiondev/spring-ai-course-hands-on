package hn.chatbot.search;

import java.util.List;

/**
 * 적합성 판단 이전의 후보.
 *
 * 제목과 요약은 StoryIndexQuery.hydrate 가 채우고, matchedPlans 는 SearchService 가 채운다.
 */
public record Candidate(long storyId, String title, String summary, List<Integer> matchedPlans) {
}
