package hn.chatbot.adapter.hn;

import hn.chatbot.service.setup.model.CollectedComment;
import hn.chatbot.service.setup.model.CollectedStory;
import hn.chatbot.service.setup.port.StorySource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * StorySource 포트의 HN 공식 API(Firebase) 어댑터.
 *
 * 목록이 ID 배열만 돌려주므로 item 을 건건이 조회해야 한다(N+1). 공식 API 에는
 * 집계도 검색도 없어 이 구조를 피할 수 없다. 그 순회를 여기서 끝낸다 —
 * 서비스는 스토리 하나를 달라고 하면 댓글까지 붙어서 받는다.
 *
 * 수집 범위는 설정값이다(app.hn.top-level-comments · replies-per-comment).
 * 하위 전체를 순회하면 스토리당 평균 85건이라 검열 호출이 그만큼 늘어난다.
 * 트리는 depth 2 에서 끊는다 — DB 의 CHECK 제약이 이를 강제한다.
 *
 * 버전 v0 은 필드 추가를 호환 변경으로 보므로 모르는 필드는 무시한다.
 * deleted · dead 항목은 여기서 걸러내 서비스로 넘기지 않는다.
 */
@Component
public class HttpStorySource implements StorySource {

    private final RestClient client;
    private final int topLevelComments;
    private final int repliesPerComment;

    public HttpStorySource(RestClient.Builder builder,
                           @Value("${app.hn.base-url}") String baseUrl,
                           @Value("${app.hn.top-level-comments}") int topLevelComments,
                           @Value("${app.hn.replies-per-comment}") int repliesPerComment) {
        this.client = builder.baseUrl(baseUrl).build();
        this.topLevelComments = topLevelComments;
        this.repliesPerComment = repliesPerComment;
    }

    @Override
    public List<Long> bestStoryIds(int limit) {
        Long[] ids = client.get().uri("/beststories.json").retrieve().body(Long[].class);
        return ids == null ? List.of() : Arrays.stream(ids).limit(limit).toList();
    }

    @Override
    public Optional<CollectedStory> collect(long storyId) {
        return item(storyId)
                .filter(HttpStorySource::alive)
                .map(story -> new CollectedStory(
                        story.id(), story.title(), story.url(), story.by(),
                        orZero(story.score()), orZero(story.descendants()), story.text(),
                        Instant.ofEpochSecond(story.time()),
                        comments(story)));
    }

    /** 설정한 최상위 N개 + 각 대댓글 M개까지. depth 2 에서 끊는다. */
    private List<CollectedComment> comments(HnItem story) {
        List<CollectedComment> collected = new ArrayList<>();

        for (long topId : take(story.kids(), topLevelComments)) {
            Optional<HnItem> top = item(topId).filter(HttpStorySource::alive);
            if (top.isEmpty()) {
                continue;
            }
            collected.add(toComment(top.get(), story.id(), (short) 1));

            for (long replyId : take(top.get().kids(), repliesPerComment)) {
                item(replyId).filter(HttpStorySource::alive)
                        .ifPresent(reply -> collected.add(toComment(reply, story.id(), (short) 2)));
            }
        }
        return collected;
    }

    private Optional<HnItem> item(long id) {
        // 존재하지 않는 id 는 404 가 아니라 본문이 null 이다.
        return Optional.ofNullable(
                client.get().uri("/item/{id}.json", id).retrieve().body(HnItem.class));
    }

    private static CollectedComment toComment(HnItem item, long storyId, short depth) {
        return new CollectedComment(item.id(),
                depth == 1 ? storyId : orZero(item.parent() == null ? null : item.parent().intValue()),
                item.by(), item.text(), depth, Instant.ofEpochSecond(item.time()));
    }

    private static boolean alive(HnItem item) {
        return !item.deleted() && !item.dead();
    }

    private static List<Long> take(List<Long> ids, int n) {
        return ids == null ? List.of() : ids.stream().limit(n).toList();
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
