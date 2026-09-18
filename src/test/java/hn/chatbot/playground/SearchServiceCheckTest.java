package hn.chatbot.playground;

import hn.chatbot.search.Candidate;
import hn.chatbot.search.SearchResult;
import hn.chatbot.search.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchService 확인. 네 계획을 모두 수행하고 중복을 제거한 후보를 만드는지 본다. 적재된 데이터가 있어야 한다.
 *
 *   ./gradlew playground --tests '*SearchServiceCheckTest'
 */
@Tag("playground")
@SpringBootTest
class SearchServiceCheckTest {

    @Autowired SearchService searchService;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void requireData() {
        Playground.requireLoadedData(jdbc);
    }

    @Test
    @DisplayName("네 계획을 수행하고 중복을 제거한 후보를 돌려준다")
    void searches() {
        SearchResult result = searchService.search("What are the limits of AI coding agents?", null, null);

        Playground.title("계획별 건수");
        result.plans().forEach(p -> System.out.printf("  계획 %d %-14s %d건  %s%n",
                p.plan(), p.name(), p.hits(), p.condition()));
        Playground.title("중복 제거 후 " + result.merged() + "건");
        for (Candidate c : result.candidates()) {
            System.out.printf("  %d  %s  계획 %s%n", c.storyId(), c.title(), c.matchedPlans());
        }

        assertThat(result.plans()).as("네 계획이 모두 수행돼야 한다").hasSize(4);
        assertThat(result.plans()).as("계획마다 실행한 조건이 있어야 한다")
                .allSatisfy(p -> assertThat(p.condition()).isNotBlank());
        assertThat(result.candidates()).isNotEmpty().hasSizeLessThanOrEqualTo(20);
        assertThat(result.candidates()).extracting(Candidate::storyId).doesNotHaveDuplicates();
        assertThat(result.candidates()).as("후보에는 제목이 채워져 있어야 한다")
                .allSatisfy(c -> assertThat(c.title()).isNotBlank());
        assertThat(result.candidates()).as("후보마다 찾아낸 계획 번호가 있어야 한다")
                .allSatisfy(c -> assertThat(c.matchedPlans()).isNotEmpty());
    }
}
