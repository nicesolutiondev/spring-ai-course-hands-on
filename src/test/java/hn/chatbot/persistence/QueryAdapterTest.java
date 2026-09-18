package hn.chatbot.persistence;

import hn.chatbot.domain.Analysis;
import hn.chatbot.domain.Article;
import hn.chatbot.domain.ArticleStatus;
import hn.chatbot.domain.Story;
import hn.chatbot.search.Candidate;
import hn.chatbot.service.chat.StoryDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 검색 후보와 근거 카드에 살을 붙이는 조회가 입력 순서를 지키고, 분석 행이 없는 스토리를 빼는지 본다.
 * docker compose 로 띄운 DB 를 그대로 쓴다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaStoryIndexQuery.class, JpaChatQuery.class})
class QueryAdapterTest {

    @Autowired TestEntityManager em;
    @Autowired JpaStoryIndexQuery index;
    @Autowired JpaChatQuery chat;

    @BeforeEach
    void seed() {
        em.persist(new Story(1L, "VMware licensing changes upset customers",
                "https://example.com/vmware", "alice", 300, 120, null, Instant.now()));
        em.persist(new Story(2L, "A deep dive into pgvector indexing",
                "https://example.com/pgvector", "bob", 210, 80, null, Instant.now()));
        em.persist(new Story(3L, "Not analyzed yet", null, "carol", 10, 2, null, Instant.now()));

        em.persist(new Article(1L, ArticleStatus.PASS, "raw", 5000, "A".repeat(2_500)));

        em.persist(new Analysis(1L, "VMware licensing shift", "NEWS_REPORT",
                "INFRASTRUCTURE_ENTERPRISE", new String[] {"VMware", "Broadcom"},
                "Frustration", "Evaluate alternatives", true, null));
        em.persist(new Analysis(2L, "pgvector indexing internals", "TECHNICAL_DEEP_DIVE",
                "AI_LLM", new String[] {"pgvector", "HNSW"},
                "Appreciation", null, true, null));
        em.flush();
    }

    @Test
    @DisplayName("hydrate 는 입력 순서대로 제목과 요약을 채우고 분석 행이 없는 스토리는 뺀다")
    void hydrate() {
        List<Candidate> candidates = index.hydrate(List.of(2L, 3L, 1L));

        assertThat(candidates).extracting(Candidate::storyId).containsExactly(2L, 1L);
        assertThat(candidates.get(0).title()).isEqualTo("A deep dive into pgvector indexing");
        assertThat(candidates.get(0).summary()).isEqualTo("pgvector indexing internals");
    }

    @Test
    @DisplayName("bodyExcerpts 는 원문 앞 2,000자만 돌려주고 원문이 없는 스토리는 뺀다")
    void bodyExcerpts() {
        Map<Long, String> excerpts = chat.bodyExcerpts(List.of(1L, 2L));

        assertThat(excerpts).containsOnlyKeys(1L);
        assertThat(excerpts.get(1L)).hasSize(2_000);
    }

    @Test
    @DisplayName("storyDetails 는 입력 순서대로 상세를 채운다")
    void storyDetails() {
        List<StoryDetail> details = chat.storyDetails(List.of(1L, 3L, 2L));

        assertThat(details).extracting(StoryDetail::storyId).containsExactly(1L, 2L);
        assertThat(details.get(0).keywords()).containsExactly("VMware", "Broadcom");
        assertThat(details.get(1).practicalImplication()).isNull();
        assertThat(chat.storyDetail(3L)).isEmpty();
    }
}
