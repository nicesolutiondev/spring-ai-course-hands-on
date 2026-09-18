package hn.chatbot.service.setup;

import hn.chatbot.ai.IssueAnalysis;
import hn.chatbot.domain.Analysis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** analysis 의 CHECK (suitable = (unsuitable_reason IS NULL)) 를 양방향으로 지키는지 본다. */
class AnalysisMapperTest {

    @Test
    @DisplayName("suitable=true 이고 사유가 빈 문자열이면 null 로 바꾼다")
    void blankReasonBecomesNullWhenSuitable() {
        Analysis a = AnalysisMapper.toEntity(1L, analysis(true, ""));

        assertThat(a.getUnsuitableReason()).isNull();
        assertThat(a.isSuitable()).isTrue();
    }

    @Test
    @DisplayName("suitable=false 인데 사유가 비어 있으면 대체값을 넣는다")
    void blankReasonGetsFallbackWhenUnsuitable() {
        assertThat(AnalysisMapper.toEntity(1L, analysis(false, "")).getUnsuitableReason())
                .isEqualTo(AnalysisMapper.UNSPECIFIED_REASON);
        assertThat(AnalysisMapper.toEntity(1L, analysis(false, null)).getUnsuitableReason())
                .isEqualTo(AnalysisMapper.UNSPECIFIED_REASON);
    }

    @Test
    @DisplayName("suitable=false 이고 사유가 있으면 그대로 둔다")
    void realReasonSurvives() {
        assertThat(AnalysisMapper.toEntity(1L, analysis(false, "기술과 무관한 주제")).getUnsuitableReason())
                .isEqualTo("기술과 무관한 주제");
    }

    @Test
    @DisplayName("값이 없는 String 필드는 전부 null 이 된다")
    void blankFieldsBecomeNull() {
        IssueAnalysis src = new IssueAnalysis("요약", "", "  ",
                List.of("a"), "", "", true, "");

        Analysis a = AnalysisMapper.toEntity(1L, src);

        assertThat(a.getCategory()).isNull();
        assertThat(a.getTechField()).isNull();
        assertThat(a.getCommunityReaction()).isNull();
        assertThat(a.getPracticalImplication()).isNull();
        assertThat(a.getSummary()).isEqualTo("요약");
    }

    @Test
    @DisplayName("keywords 는 null 이 와도 빈 배열이다 — 컬럼이 NOT NULL 이다")
    void keywordsNeverNull() {
        IssueAnalysis src = new IssueAnalysis("s", "c", "t",
                null, null, null, true, null);

        assertThat(AnalysisMapper.toEntity(1L, src).getKeywords()).isEmpty();
    }

    private static IssueAnalysis analysis(boolean suitable, String reason) {
        return new IssueAnalysis("요약", "NEWS_REPORT", "AI_LLM",
                List.of("k"), "반응", "시사점", suitable, reason);
    }
}
