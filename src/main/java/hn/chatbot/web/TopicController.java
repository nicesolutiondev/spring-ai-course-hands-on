package hn.chatbot.web;

import hn.chatbot.service.topic.TopicService;
import hn.chatbot.service.topic.model.StorySummary;
import hn.chatbot.service.topic.model.TopicCount;
import hn.chatbot.service.topic.model.TopicDistribution;
import hn.chatbot.service.topic.model.TopicStories;
import hn.chatbot.service.topic.port.TopicQuery.StorySort;
import hn.chatbot.web.dto.Facet;
import hn.chatbot.web.dto.StoryBrief;
import hn.chatbot.web.dto.TopicStats;
import hn.chatbot.web.dto.TopicStoriesResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 서비스 모델을 브라우저 계약으로 옮긴다.
 *
 * 표시 전용 값이 여기서 붙는다 — 분류 코드의 한글 이름과 url 에서 파생한 도메인이다.
 * 안쪽 계층은 이 값들의 존재를 모른다.
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topics;

    public TopicController(TopicService topics) {
        this.topics = topics;
    }

    @GetMapping
    public TopicStats stats() {
        TopicDistribution d = topics.distribution();
        return new TopicStats(
                new TopicStats.Stats(d.counts().stories(), d.counts().techFields(),
                        d.counts().categories(), d.counts().chunks(), d.counts().keywords()),
                toFacets(d.techFields()),
                toFacets(d.categories()));
    }

    @GetMapping("/{techField}/stories")
    public TopicStoriesResponse stories(@PathVariable String techField,
                                        @RequestParam(defaultValue = "score") String sort,
                                        @RequestParam(defaultValue = "20") int limit) {
        StorySort order = "recent".equalsIgnoreCase(sort) ? StorySort.RECENT : StorySort.SCORE;
        TopicStories result = topics.stories(techField, order, limit);

        return new TopicStoriesResponse(result.techField(), result.total(),
                result.stories().stream().map(TopicController::toBrief).toList());
    }

    private static List<Facet> toFacets(List<TopicCount> counts) {
        return counts.stream()
                .map(c -> new Facet(c.value(), TopicLabels.of(c.value()), c.count()))
                .toList();
    }

    private static StoryBrief toBrief(StorySummary s) {
        return new StoryBrief(s.storyId(), s.title(), s.url(), WebFormats.domainOf(s.url()),
                s.score(), s.commentCount(), s.category(), s.keywords(), s.communityReaction());
    }
}
