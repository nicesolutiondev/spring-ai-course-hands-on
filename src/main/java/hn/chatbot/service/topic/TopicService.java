package hn.chatbot.service.topic;

import hn.chatbot.service.topic.model.TopicDistribution;
import hn.chatbot.service.topic.model.TopicStories;
import hn.chatbot.service.topic.port.TopicQuery.StorySort;

/**
 * 주제 탐색. 벡터 검색을 쓰지 않는다 — 메타데이터 집계다.
 *
 * distribution 과 stories 는 SQL 집계라 완성본으로 제공한다.
 * summarize 만 LLM 을 부르므로 ai/TopicSummarizer 에 위임한다.
 */
public interface TopicService {

    TopicDistribution distribution();

    TopicStories stories(String techField, StorySort sort, int limit);

    /** summarizeTopic 도구가 부른다. 주제 탐색 화면은 쓰지 않는다. */
    String summarize(String techField);

    /** countStories 도구가 부른다. null 이거나 빈 인자는 조건에서 제외한다. */
    int count(String techField, String category);
}
