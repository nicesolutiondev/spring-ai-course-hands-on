package hn.chatbot.playground;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 확인 테스트가 함께 쓰는 입력과 전제 조건. */
final class Playground {

    record SampleIssue(long storyId, String title, String techField,
                       String body, List<String> topComments) {
    }

    private Playground() {
    }

    static List<SampleIssue> samples() throws Exception {
        try (InputStream in = Playground.class.getResourceAsStream("/playground/sample-issues.json")) {
            return new ObjectMapper().readerForListOf(SampleIssue.class).readValue(in);
        }
    }

    /**
     * 검색과 도구 확인은 적재된 데이터가 있어야 의미가 있다.
     * 데이터 셋업을 한 번도 돌리지 않았으면 실패 대신 건너뛰고 이유를 알린다.
     */
    static void requireLoadedData(JdbcTemplate jdbc) {
        Integer analyses = jdbc.queryForObject("SELECT count(*) FROM analysis WHERE suitable", Integer.class);
        Integer summaries = jdbc.queryForObject("SELECT count(*) FROM summary_vector_store", Integer.class);
        assumeTrue(analyses != null && analyses > 0 && summaries != null && summaries > 0,
                "적재된 데이터가 없어 건너뜁니다. 데이터 셋업 탭에서 셋업을 먼저 실행하세요.");
    }

    static void title(String text) {
        System.out.printf("%n=== %s%n", text);
    }
}
