package hn.chatbot.playground;

import hn.chatbot.ai.ArticleChunker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ArticleChunker 확인. 문단 경계에서 나뉘는지, 긴 문단이 하드 리밋으로 잘리는지, 순서가 유지되는지 본다.
 *
 *   ./gradlew playground --tests '*ArticleChunkerCheckTest'
 */
@Tag("playground")
@SpringBootTest
class ArticleChunkerCheckTest {

    private static final int HARD_LIMIT = 2_000;

    @Autowired ArticleChunker chunker;

    @Test
    @DisplayName("문단과 하드 리밋으로 나누고 순서를 지킨다")
    void chunksInOrder() {
        String first = "First paragraph about pgvector indexes. ".repeat(20).trim();
        String second = "Second paragraph about HNSW recall. ".repeat(20).trim();
        String longOne = "A very long paragraph without breaks. ".repeat(150).trim();
        String body = first + "\n\n" + second + "\n\n" + longOne;

        List<String> chunks = chunker.chunk(body);

        System.out.printf("입력 %d자 → 청크 %d개%n", body.length(), chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String c = chunks.get(i);
            System.out.printf("  [%d] %4d자  %s…%n", i, c.length(), c.substring(0, Math.min(50, c.length())));
        }

        assertThat(chunks).as("청크가 여러 개여야 한다").hasSizeGreaterThan(2);
        assertThat(chunks).as("모든 청크가 하드 리밋 이하여야 한다")
                .allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(HARD_LIMIT));
        assertThat(chunks.get(0)).as("첫 청크는 첫 문단에서 시작해야 한다").startsWith("First paragraph");
        assertThat(String.join("", chunks).replaceAll("\\s", ""))
                .as("청크를 순서대로 이으면 원문과 같아야 한다")
                .isEqualTo(body.replaceAll("\\s", ""));
    }
}
