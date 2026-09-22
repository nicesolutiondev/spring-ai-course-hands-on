package hn.chatbot.playground;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 세션 2 — 프롬프트 실험실. 여기는 마음껏 고친다.
 *
 * 준비된 이슈 세 건으로 프롬프트를 바꿔 가며 결과를 눈으로 본다. 앱을 띄우거나
 * 수집을 돌리지 않아도 되므로 반복이 빠르다. 여기서 다듬은 프롬프트가 세션 4 의
 * IssueAnalyzer 로 들어간다.
 *
 * 단정은 값이 돌아오는지까지만 한다. 이 테스트의 목적은 통과가 아니라
 * 프롬프트를 바꿨을 때 결과가 어떻게 달라지는지 보는 것이다.
 *
 * 해 볼 것
 *   - 허용값 목록을 빼고 돌려 본다. 목록에 없는 값이 나오는지
 *   - 출력 형식을 지시했다가 빼 본다. 형식이 유지되는지
 *   - 댓글을 넣었을 때와 뺐을 때 커뮤니티 반응이 어떻게 달라지는지
 *
 *   ./gradlew playground --tests '*PromptLabTest'
 */
@Tag("playground")
@SpringBootTest
class PromptLabTest {

    record SampleIssue(long storyId, String title, String techField,
                       String body, List<String> topComments) {
    }

    private static final List<String> TECH_FIELDS = List.of(
            "AI_LLM", "SECURITY_PRIVACY", "OPEN_SOURCE", "INFRASTRUCTURE_ENTERPRISE",
            "PLATFORM_POLICY", "DEV_CULTURE_PRACTICE", "HARDWARE", "MOBILITY", "NON_TECHNICAL");

    @Autowired ChatClient.Builder builder;

    @Test
    @DisplayName("샘플 이슈로 분석 프롬프트를 실험한다")
    void 프롬프트를_실험한다() throws Exception {
        for (SampleIssue issue : loadSamples()) {
            String result = builder.build().prompt()
                    // ── 여기부터 고친다 ──────────────────────────────────
                    .system("""
                            You analyse Hacker News technology stories.
                            Answer with three lines, in this exact shape:

                            summary: <one sentence>
                            techField: <one of the allowed values>
                            keywords: <comma separated, 3 to 5 items>

                            Allowed techField values: %s
                            If none of them fits, invent a new SCREAMING_SNAKE_CASE value.
                            """.formatted(String.join(", ", TECH_FIELDS)))
                    .user("""
                            Title: %s

                            Body:
                            %s

                            Top comments:
                            %s
                            """.formatted(issue.title(), issue.body(),
                                    String.join("\n---\n", issue.topComments())))
                    // ── 여기까지 ────────────────────────────────────────
                    .call()
                    .content();

            System.out.printf("%n=== %d  %s%n", issue.storyId(), issue.title());
            System.out.printf("   (실제 분석에서 나왔던 techField: %s)%n%n", issue.techField());
            System.out.println(result);

            assertThat(result).isNotBlank();
        }
    }

    @Test
    @DisplayName("샘플 이슈로 분석 프롬프트를 실험한다2")
    void 프롬프트를_실험한다2() throws Exception {
        String result = builder.build().prompt()
                // ── 여기부터 고친다 ──────────────────────────────────
                .system("""
                        너는 내가 인사를 하면 인사를 친절하게 받아줘야해.
                        """)
                .user("""
                        안녕?
                        """)
                // ── 여기까지 ────────────────────────────────────────
                .messages(new AssistantMessage("안녕하세요! 만나서 반가워요 \uD83D\uDE0A 무엇을 도와드릴까요?"))
                .user("오늘 서울 날씨를 알려줘.")
                .call()
                .content();

        System.out.println(result);

        assertThat(result).isNotBlank();
    }


    private List<SampleIssue> loadSamples() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/playground/sample-issues.json")) {
            return new ObjectMapper().readerForListOf(SampleIssue.class).readValue(in);
        }
    }
}
