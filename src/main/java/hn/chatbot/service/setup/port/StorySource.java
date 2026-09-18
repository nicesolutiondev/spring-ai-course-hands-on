package hn.chatbot.service.setup.port;

import hn.chatbot.service.setup.model.CollectedStory;

import java.util.List;
import java.util.Optional;

/**
 * 기술 이슈를 가져오는 포트. 서비스가 소유하고 adapter/ 가 구현한다.
 *
 * 목록과 수집을 나눠 둔 이유는 중복 확인 때문이다. 파이프라인 2단계가 storyId 로
 * 기처리 여부를 먼저 보고, 이미 처리한 것은 댓글까지 받아오지 않고 건너뛴다.
 */
public interface StorySource {

    /** 인기 스토리 ID 목록. */
    List<Long> bestStoryIds(int limit);

    /**
     * 스토리 하나와 그 댓글을 가져온다. 수집 범위와 트리 순회는 어댑터가 감춘다.
     * 없는 ID 면 비어 있는 값을 돌려준다.
     */
    Optional<CollectedStory> collect(long storyId);
}
