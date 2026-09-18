package hn.chatbot.service.setup;

import hn.chatbot.service.setup.model.LogKind;
import hn.chatbot.service.setup.model.LogState;

/**
 * StoryProcessor 가 단계 통과와 로그를 뼈대에 알리는 통로. 완성본이 구현한다.
 *
 * 화면의 8단계 체크와 최근 로그가 이 호출로 채워진다. 호출하지 않아도 처리는 되지만
 * 화면의 단계 표시가 움직이지 않는다.
 */
public interface StageTracker {

    /** stage 단계(2~8)를 통과했다. 1 수집은 뼈대가 기록한다. */
    void passed(int stage);

    void log(LogKind kind, long storyId, String title, String meta, LogState state);
}
