package hn.chatbot.web.dto;

import java.util.List;

/** 주제 탐색 탭의 통계. suitable = true 인 스토리만 집계한다. 벡터 검색을 쓰지 않는다. */
public record TopicStats(Stats stats, List<Facet> techFields, List<Facet> categories) {

    public record Stats(int stories, int techFields, int categories, int chunks, int keywords) {
    }
}
