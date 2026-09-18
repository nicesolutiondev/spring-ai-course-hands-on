package hn.chatbot.service.chat;

import hn.chatbot.service.chat.model.ChatEvent;
import org.springframework.ai.chat.model.ToolContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Q&A 한 턴 동안 도구가 화면으로 보낼 사건을 모으는 통로. 완성본이다.
 *
 * plans · evidence 사건은 searchIssues 안에서 만들어지는데, 도구의 반환값은 모델에게 간다.
 * ChatService 가 그 사건을 받을 다른 길이 없어서 이 객체를 toolContext 에 넣어 건넨다.
 *
 *   ChatService : ChatTurn turn = new ChatTurn();
 *                 .toolContext(Map.of(ChatMemory.CONVERSATION_ID, id, ChatTurn.KEY, turn))
 *                 return turn.stream(모델의 token 흐름);
 *   searchIssues: ChatTurn.from(ctx).publish(evidence);
 *
 * 턴마다 새로 만든다. ThreadLocal 이 아니므로 비울 책임이 없다.
 * 순서는 자연히 보장된다. Spring AI 는 도구 실행을 끝낸 뒤에 모델이 답을 생성하므로
 * publish 한 사건이 첫 token 보다 앞선다.
 */
public final class ChatTurn {

    public static final String KEY = "chatTurn";

    private final Sinks.Many<ChatEvent> events = Sinks.many().unicast().onBackpressureBuffer();

    public static ChatTurn from(ToolContext ctx) {
        return (ChatTurn) ctx.getContext().get(KEY);
    }

    /** 검색 결과를 plans · evidence 사건으로 방출한다. */
    public void publish(SearchEvidence evidence) {
        events.tryEmitNext(new ChatEvent.Plans(evidence.plans().stream()
                .map(p -> new ChatEvent.PlanOutcome(p.plan(), p.name(), p.hits(), p.condition()))
                .toList(), evidence.merged()));
        events.tryEmitNext(new ChatEvent.Evidence(evidence.evidence().size(), evidence.evidence().stream()
                .map(i -> new ChatEvent.EvidenceItem(i.storyId(), i.title(), i.url(), i.matchedPlans(),
                        i.category(), i.summary(), i.communityReaction(), i.practicalImplication(),
                        i.passage()))
                .toList()));
    }

    /** 도구가 방출한 사건과 모델의 token 을 합쳐 화면으로 보낼 흐름을 만든다. 끝에 done 을 붙인다. */
    public Flux<ChatEvent> stream(Flux<String> tokens) {
        Flux<ChatEvent> tokenEvents = tokens
                .<ChatEvent>map(ChatEvent.Token::new)
                .doFinally(signal -> events.tryEmitComplete());
        return Flux.merge(events.asFlux(), tokenEvents)
                .concatWith(Flux.just(new ChatEvent.Completed("STOP")));
    }
}
