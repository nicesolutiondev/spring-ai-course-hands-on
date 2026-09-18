package hn.chatbot.web;

import hn.chatbot.service.setup.SetupProgressPublisher;
import hn.chatbot.service.setup.model.SetupOutcome;
import hn.chatbot.service.setup.model.SetupProgress;
import hn.chatbot.web.dto.SetupDoneEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SetupProgressPublisher 포트의 웹 어댑터.
 *
 * 서비스 모델을 브라우저 계약으로 옮겨 SSE 로 흘린다. 서비스는 SseEmitter 도
 * web/dto 도 모른다.
 *
 * 실행 중이 아닐 때 연결해도 끊지 않는다. 화면이 부팅 직후에 붙어 두고 셋업이
 * 시작되기를 기다리기 때문이다.
 */
@Component
public class SetupSseBroadcaster implements SetupProgressPublisher {

    private static final long TIMEOUT = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** 구독은 웹 계층에만 있는 개념이라 포트에 없다. 컨트롤러가 직접 부른다. */
    public SseEmitter subscribe(SetupProgress current) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        sendTo(emitter, "progress", SetupDtoMapper.toStatus(current));
        return emitter;
    }

    @Override
    public void publish(SetupProgress progress) {
        broadcast("progress", SetupDtoMapper.toStatus(progress));
    }

    @Override
    public void complete(SetupOutcome outcome) {
        broadcast("done", new SetupDoneEvent(outcome.state(), outcome.completed(),
                outcome.excluded(), outcome.searchableStories()));
    }

    private void broadcast(String event, Object payload) {
        emitters.forEach(emitter -> sendTo(emitter, event, payload));
    }

    private void sendTo(SseEmitter emitter, String event, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(event).data(payload));
        }
        catch (IOException | IllegalStateException e) {
            // 브라우저가 이미 떠났다. 목록에서 빼고 계속 진행한다.
            emitters.remove(emitter);
        }
    }
}
