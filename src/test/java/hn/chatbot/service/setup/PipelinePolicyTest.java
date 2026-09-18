package hn.chatbot.service.setup;

import hn.chatbot.domain.ArticleStatus;
import hn.chatbot.service.setup.model.ArticleCheck;
import hn.chatbot.service.setup.model.FetchOutcome;
import hn.chatbot.service.setup.model.FetchedArticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 4단계 판정이 조회 결과와 길이 기준을 article 상태로 옮기는지 본다. */
class PipelinePolicyTest {

    @Test
    @DisplayName("조회 실패와 PDF 는 텍스트 없이 제외한다")
    void failedAndPdfAreExcludedWithoutText() {
        ArticleCheck failed = PipelinePolicy.check(fetched(FetchOutcome.FAILED, ""));
        ArticleCheck pdf = PipelinePolicy.check(fetched(FetchOutcome.PDF, ""));

        assertThat(failed.status()).isEqualTo(ArticleStatus.FETCH_FAILED);
        assertThat(pdf.status()).isEqualTo(ArticleStatus.PDF);
        assertThat(failed.passed()).isFalse();
        assertThat(pdf.passed()).isFalse();
        assertThat(failed.length()).isNull();
    }

    @Test
    @DisplayName("MIN_LENGTH 이하는 TOO_SHORT 로 제외하되 텍스트를 남긴다")
    void shortTextIsExcludedWithText() {
        ArticleCheck atLimit = PipelinePolicy.check(fetched(FetchOutcome.OK, "a".repeat(PipelinePolicy.MIN_LENGTH)));
        ArticleCheck nullText = PipelinePolicy.check(fetched(FetchOutcome.OK, null));

        assertThat(atLimit.status()).isEqualTo(ArticleStatus.TOO_SHORT);
        assertThat(atLimit.length()).isEqualTo(PipelinePolicy.MIN_LENGTH);
        assertThat(nullText.status()).isEqualTo(ArticleStatus.TOO_SHORT);
        assertThat(nullText.length()).isZero();
    }

    @Test
    @DisplayName("MIN_LENGTH 를 넘으면 통과하고 그대로 돌려준다")
    void longEnoughTextPasses() {
        String text = "a".repeat(PipelinePolicy.MIN_LENGTH + 1);
        ArticleCheck check = PipelinePolicy.check(fetched(FetchOutcome.OK, text));

        assertThat(check.passed()).isTrue();
        assertThat(check.text()).isEqualTo(text);
    }

    @Test
    @DisplayName("MAX_LENGTH 초과분은 잘라서 통과시킨다")
    void overlongTextIsTruncated() {
        ArticleCheck check = PipelinePolicy.check(fetched(FetchOutcome.OK, "a".repeat(PipelinePolicy.MAX_LENGTH + 500)));

        assertThat(check.passed()).isTrue();
        assertThat(check.length()).isEqualTo(PipelinePolicy.MAX_LENGTH);
    }

    private static FetchedArticle fetched(FetchOutcome outcome, String text) {
        return new FetchedArticle("https://example.com", outcome, text);
    }
}
