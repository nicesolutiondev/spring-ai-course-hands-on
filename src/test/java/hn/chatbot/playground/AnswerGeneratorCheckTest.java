package hn.chatbot.playground;

import hn.chatbot.ai.AnswerGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AnswerGenerator 확인. 답변 규칙을 system 에 넣고, 준비된 근거로 답하게 해 결과를 본다.
 * Q&A 에서는 ChatService 가 같은 방식으로 규칙을 넣는다. 적재된 데이터는 필요 없다.
 *
 *   ./gradlew playground --tests '*AnswerGeneratorCheckTest'
 */
@Tag("playground")
@SpringBootTest
class AnswerGeneratorCheckTest {

    private static final String EVIDENCE = """
            [근거]
            1. storyId=101, category=OPINION_ESSAY, title=Claude Code struggles with large refactors
               summary: Developers report that AI coding agents lose context on multi-file refactors.
               communityReaction: Many agree; some say smaller steps help.
            2. storyId=103, category=TECHNICAL_DEEP_DIVE, title=Why AI agents still need human code review
               summary: Hallucinated APIs and silent bugs introduced by coding assistants.
               communityReaction: Reviewers report more subtle bugs than before.
            [검색 조건] query="coding agent limits", category=없음
            """;

    @Autowired AnswerGenerator generator;
    @Autowired ChatClient.Builder builder;

    @Test
    @DisplayName("답변 규칙을 따라 근거로만 답한다")
    void answersWithRules() {
        String rules = generator.answerRules();
        Playground.title("답변 규칙");
        System.out.println(rules);

        List<String> pieces = builder.build().prompt()
                .system(rules)
                .user(EVIDENCE + "\n질문: 코딩 에이전트의 한계가 뭐야?")
                .stream()
                .content()
                .doOnNext(System.out::print)
                .collectList()
                .block();
        System.out.println();

        assertThat(rules).as("규칙이 비어 있으면 안 된다").isNotBlank();
        assertThat(pieces).as("답이 조각으로 흘러와야 한다").hasSizeGreaterThan(1);
        assertThat(String.join("", pieces)).isNotBlank();
    }
}
