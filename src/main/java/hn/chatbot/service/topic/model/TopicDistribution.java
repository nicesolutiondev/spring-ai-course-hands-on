package hn.chatbot.service.topic.model;

import java.util.List;

/** 주제 분포. suitable = true 인 스토리만 집계한다. 벡터 검색을 쓰지 않는다. */
public record TopicDistribution(TopicCounts counts,
                                List<TopicCount> techFields,
                                List<TopicCount> categories) {
}
