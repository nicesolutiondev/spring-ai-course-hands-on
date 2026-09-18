package hn.chatbot.service.chat;

import hn.chatbot.ai.RelevantStory;
import hn.chatbot.search.Candidate;
import hn.chatbot.search.PlanSummary;
import hn.chatbot.search.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SearchEvidenceTest {

    private static final SearchResult RESULT = new SearchResult(
            List.of(new PlanSummary(3, "원문 청크 벡터 검색", 2, "query=\"q\", suitable, top 5")), 2,
            List.of(new Candidate(1L, "one", "s1", List.of(3)), new Candidate(2L, "two", "s2", List.of(3, 4))));

    private static final List<StoryDetail> DETAILS = List.of(
            new StoryDetail(1L, "one", "https://a", "s1", "r1", null, "AI_LLM", "NEWS_REPORT", List.of()),
            new StoryDetail(2L, "two", "https://b", "s2", "r2", "p2", "AI_LLM", "OPINION_ESSAY", List.of()));

    @Test
    @DisplayName("판단 순서대로 번호를 매기고, 목록에 없는 id 와 원문에 없는 대목은 버린다")
    void assemble() {
        List<RelevantStory> judged = List.of(
                new RelevantStory(2L, "agents   lose\ncontext"),
                new RelevantStory(99L, "made up"),
                new RelevantStory(1L, "not in the text"));
        Map<Long, String> excerpts = Map.of(1L, "something else", 2L, "Coding agents lose context on refactors.");

        SearchEvidence e = SearchEvidence.assemble(RESULT, judged, DETAILS, excerpts);

        assertThat(e.evidence()).extracting(SearchEvidence.Item::storyId).containsExactly(2L, 1L);
        assertThat(e.evidence()).extracting(SearchEvidence.Item::rank).containsExactly(1, 2);
        assertThat(e.evidence().get(0).passage()).isEqualTo("agents   lose\ncontext");
        assertThat(e.evidence().get(0).matchedPlans()).containsExactly(3, 4);
        assertThat(e.evidence().get(1).passage()).isNull();
    }

    @Test
    @DisplayName("대목은 300자까지만 남긴다")
    void passageLimit() {
        String longText = "a".repeat(400);
        assertThat(SearchEvidence.verifiedPassage(longText, longText)).hasSize(300);
        assertThat(SearchEvidence.verifiedPassage(" ", longText)).isNull();
        assertThat(SearchEvidence.verifiedPassage("x", null)).isNull();
    }
}
