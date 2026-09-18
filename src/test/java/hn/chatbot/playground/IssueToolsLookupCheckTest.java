package hn.chatbot.playground;

import hn.chatbot.service.chat.CommentView;
import hn.chatbot.service.chat.IssueTools;
import hn.chatbot.service.chat.StoryDetail;
import hn.chatbot.service.topic.model.TopicDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IssueTools 의 조회 도구 확인. countStories · getStoryDetail · getComments · listTopics 가
 * 도구로 등록됐는지와 값을 돌려주는지 본다. 적재된 데이터가 있어야 한다.
 *
 *   ./gradlew playground --tests '*IssueToolsLookupCheckTest'
 */
@Tag("playground")
@SpringBootTest
class IssueToolsLookupCheckTest {

    @Autowired IssueTools tools;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void requireData() {
        Playground.requireLoadedData(jdbc);
    }

    @Test
    @DisplayName("조회 도구 네 개가 설명과 함께 등록돼 있다")
    void registered() {
        List<ToolCallback> callbacks = List.of(ToolCallbacks.from(tools));

        Playground.title("등록된 도구");
        callbacks.forEach(c -> System.out.printf("  %-16s %s%n",
                c.getToolDefinition().name(), c.getToolDefinition().description()));

        assertThat(callbacks).extracting(c -> c.getToolDefinition().name())
                .contains("countStories", "getStoryDetail", "getComments", "listTopics");
        // description 을 비워 두면 Spring AI 가 메서드 이름으로 채운다. 그 값으로는 모델이 도구를 고를 수 없다.
        assertThat(callbacks).as("모델은 설명을 보고 도구를 고른다. 설명 문구를 직접 작성해야 한다")
                .allSatisfy(c -> assertThat(c.getToolDefinition().description())
                        .hasSizeGreaterThan(20)
                        .isNotEqualToIgnoringCase(c.getToolDefinition().name()));
    }

    @Test
    @DisplayName("조회 도구가 값을 돌려준다")
    void returnsValues() {
        long storyId = jdbc.queryForObject(
                "SELECT story_id FROM analysis WHERE suitable ORDER BY story_id LIMIT 1", Long.class);

        int count = tools.countStories(null, null);
        TopicDistribution topics = tools.listTopics();
        StoryDetail detail = tools.getStoryDetail(storyId);
        List<CommentView> comments = tools.getComments(storyId, 3);

        System.out.println("countStories   : " + count);
        System.out.println("listTopics     : " + topics.techFields());
        System.out.println("getStoryDetail : " + detail.title());
        System.out.println("getComments    : " + comments.size() + "건");

        assertThat(count).isPositive();
        assertThat(topics.techFields()).isNotEmpty();
        assertThat(detail.storyId()).isEqualTo(storyId);
        assertThat(comments).hasSizeLessThanOrEqualTo(3);
    }
}
