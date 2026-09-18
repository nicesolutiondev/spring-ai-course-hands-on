package hn.chatbot.adapter;

import hn.chatbot.service.setup.model.CollectedComment;
import hn.chatbot.service.setup.model.CollectedStory;
import hn.chatbot.service.setup.model.FetchOutcome;
import hn.chatbot.service.setup.model.FetchedArticle;
import hn.chatbot.service.setup.port.ArticleSource;
import hn.chatbot.service.setup.port.StorySource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 어댑터는 바깥 세계를 상대하므로 실제로 불러 봐야 검증된다.
 * 인터넷이 필요하고 HN 의 실시간 데이터에 의존한다.
 *
 * 테스트가 보는 것은 포트다. HN 의 응답 모양(HnItem)도, 목록이 ID 만 주기 때문에
 * 생기는 순회도 어댑터 안에 있다.
 */
@SpringBootTest
class AdapterLiveTest {

    @Autowired StorySource stories;
    @Autowired ArticleSource articles;

    @Test
    @DisplayName("인기 스토리 ID 를 limit 만큼 가져온다")
    void bestStoryIds() {
        List<Long> ids = stories.bestStoryIds(10);

        assertThat(ids).hasSize(10).doesNotHaveDuplicates().allMatch(id -> id > 0);
    }

    @Test
    @DisplayName("스토리 하나를 부르면 댓글까지 붙어서 온다 — 순회는 어댑터가 감춘다")
    void collectStoryWithComments() {
        long storyId = stories.bestStoryIds(1).get(0);

        CollectedStory story = stories.collect(storyId).orElseThrow();

        assertThat(story.id()).isEqualTo(storyId);
        assertThat(story.title()).isNotBlank();
        assertThat(story.postedAt()).isNotNull();

        // 수집 범위는 설정값이고, depth 2 에서 끊는다.
        assertThat(story.comments()).allSatisfy(c -> {
            assertThat(c.depth()).isBetween((short) 1, (short) 2);
            assertThat(c.id()).isPositive();
            assertThat(c.parentId()).isPositive();
        });

        long topLevel = story.comments().stream().filter(c -> c.depth() == 1).count();
        assertThat(topLevel).isLessThanOrEqualTo(10);

        for (CollectedComment top : story.comments()) {
            if (top.depth() != 1) {
                continue;
            }
            long replies = story.comments().stream()
                    .filter(c -> c.depth() == 2 && c.parentId() == top.id()).count();
            assertThat(replies).isLessThanOrEqualTo(5);
        }
        // 최상위 댓글의 부모는 스토리다.
        assertThat(story.comments().stream().filter(c -> c.depth() == 1))
                .allMatch(c -> c.parentId() == storyId);
    }

    @Test
    @DisplayName("없는 ID 는 404 가 아니라 빈 값이다")
    void missingStoryIsEmpty() {
        assertThat(stories.collect(999_999_999_999L)).isEmpty();
    }

    @Test
    @DisplayName("정상 문서는 태그가 걷힌 본문으로 온다")
    void staticPage() {
        FetchedArticle page = articles.fetch("https://en.wikipedia.org/wiki/Hacker_News");

        assertThat(page.outcome()).isEqualTo(FetchOutcome.OK);
        assertThat(page.text()).doesNotContain("<script").doesNotContain("<div");
        assertThat(page.text().length()).isGreaterThan(2000);
    }

    @Test
    @DisplayName("PDF 는 시그니처로 걸러낸다")
    void pdfIsDetected() {
        FetchedArticle page = articles.fetch(
                "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");

        assertThat(page.outcome()).isEqualTo(FetchOutcome.PDF);
        assertThat(page.text()).isEmpty();
    }

    @Test
    @DisplayName("조회 실패는 예외가 아니라 FAILED 로 기록된다")
    void fetchFailureIsRecorded() {
        FetchedArticle page = articles.fetch("https://no-such-host-" + System.nanoTime() + ".invalid/x");

        assertThat(page.outcome()).isEqualTo(FetchOutcome.FAILED);
        assertThat(page.text()).isEmpty();
    }
}
