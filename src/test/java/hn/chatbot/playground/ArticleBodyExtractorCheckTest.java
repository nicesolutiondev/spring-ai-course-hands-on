package hn.chatbot.playground;

import hn.chatbot.ai.ArticleBodyExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ArticleBodyExtractor 확인. 샘플 원문에서 본문만 남는지 눈으로 보고,
 * 본문이 없는 텍스트에는 빈 값을 돌려주는지 확인한다.
 *
 *   ./gradlew playground --tests '*ArticleBodyExtractorCheckTest'
 */
@Tag("playground")
@SpringBootTest
class ArticleBodyExtractorCheckTest {

    private static final String NAVIGATION_ONLY = """
            Skip to content Sign in Sign up Home Pricing Docs Blog Careers
            Accept cookies Manage preferences Subscribe to our newsletter
            Privacy Terms Contact (c) 2026 Example Inc. All rights reserved.
            """;

    @Autowired ArticleBodyExtractor extractor;

    @Test
    @DisplayName("샘플 원문에서 본문을 추출한다")
    void extractsBody() throws Exception {
        int extracted = 0;
        int shortened = 0;
        for (Playground.SampleIssue issue : Playground.samples()) {
            Optional<String> body = extractor.extract(issue.title(), issue.body());

            Playground.title(issue.title());
            System.out.printf("입력 %d자 → 추출 %s%n", issue.body().length(),
                    body.map(b -> b.length() + "자").orElse("없음"));
            body.ifPresent(b -> System.out.println(b.substring(0, Math.min(400, b.length()))));

            if (body.isPresent()) {
                extracted++;
                assertThat(body.get()).as("본문이 있으면 비어 있지 않아야 한다").isNotBlank();
                // 본문만 있는 샘플은 줄바꿈이 더해져 조금 길어질 수 있다. 크게 늘면 내용을 지어낸 것이다.
                assertThat(body.get().length()).as("입력에 없는 내용을 덧붙이면 안 된다")
                        .isLessThanOrEqualTo((int) (issue.body().length() * 1.05));
                if (body.get().length() < issue.body().length()) {
                    shortened++;
                }
            }
        }
        assertThat(extracted).as("샘플 세 건 중 하나 이상에서는 본문을 찾아야 한다").isPositive();
        assertThat(shortened).as("메뉴 · 푸터가 섞인 샘플에서는 걷어내 짧아져야 한다").isPositive();
    }

    @Test
    @DisplayName("본문이 없는 텍스트에는 빈 값을 돌려준다")
    void returnsEmptyWithoutBody() {
        Optional<String> body = extractor.extract("Example Inc.", NAVIGATION_ONLY);

        System.out.println("메뉴와 푸터뿐인 텍스트 → " + body.orElse("빈 값"));
        assertThat(body).as("메뉴와 푸터만 있으면 빈 값이어야 한다").isEmpty();
    }
}
