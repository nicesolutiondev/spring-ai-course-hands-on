package hn.chatbot.web;

import hn.chatbot.service.chat.ChatService;
import hn.chatbot.service.chat.model.ChatEvent;
import hn.chatbot.web.dto.ChatRequest;
import hn.chatbot.web.dto.DoneEvent;
import hn.chatbot.web.dto.EvidenceCard;
import hn.chatbot.web.dto.EvidenceEvent;
import hn.chatbot.web.dto.PlanResult;
import hn.chatbot.web.dto.PlansEvent;
import hn.chatbot.web.dto.TokenEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 서비스가 흘리는 사건을 구독해 SSE 로 옮긴다.
 *
 * 애플리케이션은 MVC 구조다. ChatClient.stream() 이 Flux 를 반환하므로 그 지점만
 * 리액티브를 쓰고, 받은 Flux 를 구독해 SseEmitter 로 흘린다.
 *
 * 응답이 SSE 인데 EventSource 는 GET 만 지원하므로 프론트는 fetch + ReadableStream 으로 받는다.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final long TIMEOUT = 5 * 60 * 1000L;

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @PostMapping
    public SseEmitter chat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        chat.chat(request.conversationId(), request.question())
                .subscribe(
                        event -> send(emitter, event),
                        emitter::completeWithError,
                        emitter::complete);

        return emitter;
    }

    private static void send(SseEmitter emitter, ChatEvent event) {
        try {
            // Java 17 이라 switch 패턴 매칭을 쓸 수 없다. instanceof 패턴은 표준이다.
            if (event instanceof ChatEvent.Plans e) {
                emitter.send(SseEmitter.event().name("plans").data(new PlansEvent(
                        e.plans().stream().map(p -> new PlanResult(p.plan(), p.name(), p.hits(), p.condition())).toList(),
                        e.merged())));
            }
            else if (event instanceof ChatEvent.Evidence e) {
                emitter.send(SseEmitter.event().name("evidence").data(new EvidenceEvent(
                        e.selected(), e.items().stream().map(ChatController::toCard).toList())));
            }
            else if (event instanceof ChatEvent.Token e) {
                emitter.send(SseEmitter.event().name("token").data(new TokenEvent(e.text())));
            }
            else if (event instanceof ChatEvent.Completed e) {
                emitter.send(SseEmitter.event().name("done").data(new DoneEvent(e.finishReason())));
            }
        }
        catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }

    private static EvidenceCard toCard(ChatEvent.EvidenceItem i) {
        return new EvidenceCard(i.storyId(), i.title(), i.url(), WebFormats.domainOf(i.url()),
                i.matchedPlans(), i.category(), i.summary(), i.communityReaction(),
                i.practicalImplication(), i.passage());
    }
}
