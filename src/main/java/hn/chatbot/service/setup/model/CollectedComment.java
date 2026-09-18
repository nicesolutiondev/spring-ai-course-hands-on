package hn.chatbot.service.setup.model;

import java.time.Instant;

/**
 * 수집된 댓글 하나. depth 는 최상위가 1, 대댓글이 2 다.
 *
 * HN 응답의 deleted · dead 항목은 어댑터가 이미 걸러냈다. 여기 오는 것은 저장할 것뿐이다.
 */
public record CollectedComment(long id, long parentId, String author,
                               String text, short depth, Instant postedAt) {
}
