package hn.chatbot.service.chat.port;

import hn.chatbot.service.chat.CommentView;
import hn.chatbot.service.chat.StoryDetail;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Q&A 가 필요로 하는 조회 포트. 근거 카드를 채우고 도구가 상세를 가져온다. */
public interface ChatQuery {

    Optional<StoryDetail> storyDetail(long storyId);

    List<CommentView> comments(long storyId, int limit);

    /** 최종 선택된 storyId 들의 상세. 근거 카드를 채울 때 한 번에 가져온다. */
    List<StoryDetail> storyDetails(List<Long> storyIds);

    /** 원문을 이만큼만 읽어 적합성 판단에 넘긴다. */
    int EXCERPT_CHARS = 2_000;

    /** 후보 storyId 들의 원문 앞 EXCERPT_CHARS 자. 원문이 없는 스토리는 빠진다. */
    Map<Long, String> bodyExcerpts(List<Long> storyIds);
}
