package hn.chatbot.service.chat;

import hn.chatbot.service.chat.model.ChatEvent;
import hn.chatbot.ai.AnswerGenerator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 빈 구현이다.
 *
 * Q&A 탭은 잠그지 않는다. 어떤 질문이 들어와도 안내 문구를 token 으로 나눠 흘린다.
 * 채팅 창에서 글자가 하나씩 채워지므로 스트리밍 경로 전체가 처음부터
 * 살아 있음을 확인할 수 있다.
 *
 * 수강생이 ChatService 를 구현한 뒤에는, 데이터가 없을 때 안내 문구를 같은 방식으로
 * 흘리는 것이 그 자리를 대신한다.
 */
@Service
public class ChatServiceShell implements ChatService {

    private final ChatClient chatClient;
    private final AnswerGenerator answerGenerator;
    private final IssueTools issueTools;
    private final ChatMemory chatMemory;

    public ChatServiceShell(ChatClient.Builder builder, AnswerGenerator answerGenerator, IssueTools issueTools, ChatMemory chatMemory) {
        this.chatClient = builder.build(); this.answerGenerator = answerGenerator; this.issueTools = issueTools; this.chatMemory = chatMemory;
    }

    @Override
    public Flux<ChatEvent> chat(String conversationId, String question) {
        if (conversationId == null || conversationId.isBlank() || question == null || question.isBlank()) return Flux.just(new ChatEvent.Completed("STOP"));
        ChatTurn turn = new ChatTurn();
        Flux<String> tokens = chatClient.prompt().system(answerGenerator.answerRules()).user(question).tools(issueTools)
                .toolContext(Map.of(ChatMemory.CONVERSATION_ID, conversationId, ChatTurn.KEY, turn))
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId)).stream().content();
        return turn.stream(tokens);
    }
}
