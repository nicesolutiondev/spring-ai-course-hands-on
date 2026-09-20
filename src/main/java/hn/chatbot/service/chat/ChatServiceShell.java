package hn.chatbot.service.chat;

import hn.chatbot.ai.AnswerGenerator;
import hn.chatbot.service.chat.model.ChatEvent;
import hn.chatbot.service.topic.TopicService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * Q&A 한 턴을 처리한다. 답변 규칙 · 도구 · 기억 Advisor · ChatTurn 을 한 체인에 붙여
 * 스트리밍으로 호출한다.
 *
 * 적재된 데이터가 없으면 모델을 부르지 않고 안내 문구를 token 으로 나눠 흘린다.
 * 빈 구현일 때 스트리밍 경로가 살아 있음을 보여 주던 방식을 그대로 쓴다.
 */
@Service
public class ChatServiceShell implements ChatService {

    private static final String NO_DATA_MESSAGE =
            "아직 적재된 이슈가 없습니다. 데이터 셋업 탭에서 먼저 셋업을 실행해 주세요.";

    private final ChatClient chatClient;
    private final AnswerGenerator answerGenerator;
    private final IssueTools issueTools;
    private final TopicService topicService;
    private final ChatMemory chatMemory;

    public ChatServiceShell(ChatClient.Builder builder, AnswerGenerator answerGenerator, IssueTools issueTools,
                            TopicService topicService, ChatMemory chatMemory) {
        this.chatClient = builder.build();
        this.answerGenerator = answerGenerator;
        this.issueTools = issueTools;
        this.topicService = topicService;
        this.chatMemory = chatMemory;
    }

    @Override
    public Flux<ChatEvent> chat(String conversationId, String question) {
        if (topicService.count(null, null) == 0) {
            return guidance(NO_DATA_MESSAGE);
        }

        ChatTurn turn = new ChatTurn();

        Flux<String> tokens = chatClient.prompt()
                .system(answerGenerator.answerRules())
                .user(question)
                .tools(issueTools)
                .toolContext(Map.of(ChatMemory.CONVERSATION_ID, conversationId, ChatTurn.KEY, turn))
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();

        return turn.stream(tokens);
    }

    private static Flux<ChatEvent> guidance(String message) {
        Flux<ChatEvent> tokens = Flux.fromArray(message.split(""))
                .delayElements(Duration.ofMillis(40))
                .map(ChatEvent.Token::new)
                .cast(ChatEvent.class);

        return tokens.concatWith(Flux.just(new ChatEvent.Completed("STOP")));
    }
}
