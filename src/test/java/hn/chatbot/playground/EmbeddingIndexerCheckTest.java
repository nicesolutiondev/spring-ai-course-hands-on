package hn.chatbot.playground;

import hn.chatbot.ai.EmbeddingIndexer;
import hn.chatbot.ai.IssueAnalysis;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EmbeddingIndexer 확인. 요약 1행과 청크 N행이 각 스토어에 들어가고 메타데이터가 붙는지 본다.
 * 확인이 끝나면 넣은 행을 지운다.
 *
 *   ./gradlew playground --tests '*EmbeddingIndexerCheckTest'
 */
@Tag("playground")
@SpringBootTest
class EmbeddingIndexerCheckTest {

    private static final long STORY_ID = 999_000_001L;

    @Autowired EmbeddingIndexer indexer;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        for (String table : List.of("summary_vector_store", "body_vector_store")) {
            jdbc.update("DELETE FROM " + table + " WHERE metadata->>'storyId' = ?", String.valueOf(STORY_ID));
        }
    }

    @Test
    @DisplayName("요약과 청크를 두 스토어에 나눠 적재한다")
    void indexesIntoTwoStores() {
        IssueAnalysis analysis = new IssueAnalysis("pgvector adds faster HNSW index builds",
                "NEWS_REPORT", "AI_LLM", List.of("pgvector", "HNSW"),
                "Developers welcome the speedup", "Rebuild indexes after upgrading", true, null);
        List<String> chunks = List.of(
                "pgvector 0.8 speeds up HNSW index builds with parallel workers.",
                "Benchmarks show recall stays the same while build time drops.");

        indexer.indexSummary(STORY_ID, analysis);
        indexer.indexBody(STORY_ID, analysis, chunks);

        List<String> summaryMeta = metadata("summary_vector_store");
        List<String> bodyMeta = metadata("body_vector_store");
        System.out.println("요약 스토어: " + summaryMeta);
        System.out.println("본문 스토어: " + bodyMeta);

        assertThat(summaryMeta).as("요약은 스토리당 1행").hasSize(1);
        assertThat(bodyMeta).as("청크 수만큼 행이 있어야 한다").hasSize(chunks.size());
        assertThat(summaryMeta.get(0)).contains("techField", "category", "suitable");
        assertThat(bodyMeta).as("본문 스토어에도 필터 값과 chunkSeq 가 있어야 한다")
                .allSatisfy(m -> assertThat(m).contains("techField", "category", "suitable", "chunkSeq"));
    }

    private List<String> metadata(String table) {
        return jdbc.queryForList("SELECT metadata::text FROM " + table + " WHERE metadata->>'storyId' = ?",
                String.class, String.valueOf(STORY_ID));
    }
}
