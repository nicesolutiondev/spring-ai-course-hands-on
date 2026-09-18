package hn.chatbot.service.setup;

import hn.chatbot.service.setup.model.SetupOutcome;
import hn.chatbot.service.setup.model.SetupProgress;

/**
 * 진행 상황을 바깥으로 흘리는 포트. 서비스가 소유하고 web/ 이 구현한다.
 *
 * 구독(SseEmitter)은 여기 없다. 순전히 전송 방식의 문제이고 서비스가 알 일이 아니다.
 * 서비스는 "진행됐다"와 "끝났다"만 알린다.
 */
public interface SetupProgressPublisher {

    void publish(SetupProgress progress);

    void complete(SetupOutcome outcome);
}
