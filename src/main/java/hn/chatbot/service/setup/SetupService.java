package hn.chatbot.service.setup;

import hn.chatbot.service.setup.model.SetupProgress;
import hn.chatbot.service.setup.model.SetupRun;

/**
 * 셋업 실행과 진행 상황 산출.
 *
 * run 은 비동기로 시작하고 즉시 반환한다. 이미 실행 중이면 컨트롤러가 409 를 낸다.
 *
 * 구현은 완성본 PipelineSetupService 다. 스토리 하나를 2~8단계로 처리하는 일은
 * StoryProcessor 에 위임하며, 단계와 규칙은 그 Javadoc 에 있다.
 */
public interface SetupService {

    SetupRun run(int limit);

    SetupProgress progress();

    void stop();
}
