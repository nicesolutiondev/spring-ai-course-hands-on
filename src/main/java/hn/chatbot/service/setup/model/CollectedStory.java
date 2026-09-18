package hn.chatbot.service.setup.model;

import java.time.Instant;
import java.util.List;

/**
 * 수집된 스토리와 그 댓글. 서비스의 언어로 된 모델이다.
 *
 * HN API 의 응답 모양(kids · by · deleted · dead)은 여기 없다. 목록이 ID 만 주기 때문에
 * 생기는 N+1 순회도, 설정값으로 정하는 수집 범위도 어댑터가 감춘다.
 * 그래야 수집처를 바꿔도 파이프라인이 흔들리지 않는다.
 */
public record CollectedStory(long id, String title, String url, String author,
                             int score, int descendants, String text, Instant postedAt,
                             List<CollectedComment> comments) {
}
