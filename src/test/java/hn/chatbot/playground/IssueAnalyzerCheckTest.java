package hn.chatbot.playground;

import hn.chatbot.ai.IssueAnalysis;
import hn.chatbot.ai.IssueAnalyzer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IssueAnalyzer 확인. 샘플 이슈 세 건의 구조화 출력을 보고, 적합 여부와 사유가 서로 맞는지 확인한다.
 *
 *   ./gradlew playground --tests '*IssueAnalyzerCheckTest'
 */
@Tag("playground")
@SpringBootTest
class IssueAnalyzerCheckTest {

    @Autowired IssueAnalyzer analyzer;

    @Test
    @DisplayName("샘플 이슈를 구조화 출력으로 분석한다")
    void analyzesSamples() throws Exception {
        for (Playground.SampleIssue issue : Playground.samples()) {
            IssueAnalysis a = analyzer.analyze(issue.title(), issue.body(), issue.topComments());

            Playground.title(issue.title());
            System.out.println("techField  : " + a.techField() + "  (이전 분석: " + issue.techField() + ")");
            System.out.println("category   : " + a.category());
            System.out.println("keywords   : " + a.keywords());
            System.out.println("summary    : " + a.summary());
            System.out.println("suitable   : " + a.suitable() + "  " + a.unsuitableReason());

            if (a.suitable()) {
                assertThat(a.summary()).as("적합이면 요약이 있어야 한다").isNotBlank();
                assertThat(a.techField()).as("적합이면 기술 분야가 있어야 한다").isNotBlank();
                assertThat(a.keywords()).as("적합이면 키워드가 있어야 한다").isNotEmpty();
            } else {
                assertThat(a.unsuitableReason()).as("부적합이면 사유가 있어야 한다").isNotBlank();
            }
        }
    }
}
