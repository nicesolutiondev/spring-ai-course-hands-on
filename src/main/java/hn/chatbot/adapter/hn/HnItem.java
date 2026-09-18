package hn.chatbot.adapter.hn;

import java.util.List;

/**
 * HN API 의 item 응답. 스토리와 댓글이 같은 형태를 공유한다.
 *
 * deleted / dead 항목은 by 와 text 가 비어 있을 수 있다. 저장하지 않는다.
 */
public record HnItem(
        long id, String type, String by, long time,
        String title, String url, String text,
        Integer score, Integer descendants, List<Long> kids, Long parent,
        boolean deleted, boolean dead) {
}
