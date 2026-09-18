package hn.chatbot.playground;

import hn.chatbot.service.chat.IssueTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TopicSummarizer 와 summarizeTopic 도구 확인. 분야 하나의 논의 흐름을 산문으로 요약하는지 본다.
 * 적재된 데이터가 있어야 한다.
 *
 *   ./gradlew playground --tests '*TopicSummarizerCheckTest'
 */
@Tag("playground")
@SpringBootTest
class TopicSummarizerCheckTest {

    @Autowired IssueTools tools;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void requireData() {
        Playground.requireLoadedData(jdbc);
    }

    @Test
    @DisplayName("summarizeTopic 도구가 분야의 논의 흐름을 요약한다")
    void summarizes() {
        String techField = jdbc.queryForObject("""
                SELECT tech_field FROM analysis WHERE suitable AND tech_field IS NOT NULL
                GROUP BY tech_field ORDER BY count(*) DESC LIMIT 1
                """, String.class);

        String summary = tools.summarizeTopic(techField);

        Playground.title("summarizeTopic(" + techField + ")");
        System.out.println(summary);

        assertThat(List.of(ToolCallbacks.from(tools))).extracting(c -> c.getToolDefinition().name())
                .as("summarizeTopic 이 도구로 등록돼 있어야 한다").contains("summarizeTopic");
        assertThat(summary).isNotBlank();
    }
}
