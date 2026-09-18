package hn.chatbot.service.chat;

import hn.chatbot.search.PlanSummary;
import hn.chatbot.service.chat.model.ChatEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatTurnTest {

    @Test
    @DisplayName("도구가 방출한 사건이 token 보다 앞서고 끝에 done 이 붙는다")
    void ordersEvents() {
        ChatTurn turn = new ChatTurn();
        SearchEvidence evidence = new SearchEvidence(
                List.of(new PlanSummary(1, "키워드 완전 일치", 0, "keywords=[a], suitable")), 1,
                List.of(new SearchEvidence.Item(1, 7L, "t", "u", List.of(1), "NEWS_REPORT", "s", "r", null, "p")));

        // 모델이 도구를 호출한 뒤에야 token 이 나오는 상황을 흉내 낸다.
        Flux<String> tokens = Flux.defer(() -> {
            turn.publish(evidence);
            return Flux.just("안", "녕").delayElements(Duration.ofMillis(10));
        });

        List<ChatEvent> events = turn.stream(tokens).collectList().block();

        assertThat(events).extracting(e -> e.getClass().getSimpleName())
                .containsExactly("Plans", "Evidence", "Token", "Token", "Completed");
        ChatEvent.Plans plans = (ChatEvent.Plans) events.get(0);
        assertThat(plans.plans().get(0).condition()).isEqualTo("keywords=[a], suitable");
    }

    @Test
    @DisplayName("검색이 없는 턴은 token 과 done 만 흐른다")
    void withoutSearch() {
        ChatTurn turn = new ChatTurn();

        List<ChatEvent> events = turn.stream(Flux.just("3건입니다")).collectList().block();

        assertThat(events).extracting(e -> e.getClass().getSimpleName())
                .containsExactly("Token", "Completed");
    }
}
