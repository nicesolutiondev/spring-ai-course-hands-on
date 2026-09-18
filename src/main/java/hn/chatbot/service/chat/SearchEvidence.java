package hn.chatbot.service.chat;

import hn.chatbot.ai.RelevantStory;
import hn.chatbot.search.Candidate;
import hn.chatbot.search.PlanSummary;
import hn.chatbot.search.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * searchIssues 도구의 반환값. 모델이 답을 쓸 근거이고, 화면 사건과 기억 기록의 원본이다. 완성본이다.
 *
 * - plans: 계획별 건수와 실행 조건. 모델이 답변에서 검색 조건을 설명할 때 쓴다
 * - evidence: 적합성 판단이 고른 순서 그대로의 근거. rank 가 근거 카드 번호다
 *
 * assemble 이 검색 결과 · 판단 결과 · 조회한 상세를 합친다. 여기서 대목을 다시 검사한다.
 * 모델이 원문을 바꿔 쓴 대목이나 목록에 없는 storyId 는 버린다.
 */
public record SearchEvidence(List<PlanSummary> plans, int merged, List<Item> evidence) {

    static final int PASSAGE_MAX = 300;

    public record Item(int rank, long storyId, String title, String url, List<Integer> matchedPlans,
                       String category, String summary, String communityReaction,
                       String practicalImplication, String passage) {
    }

    /**
     * 인자는 순서대로 SearchService.search 결과, RelevanceJudge 결과(순서가 곧 근거 순서),
     * ChatQuery.storyDetails 결과, ChatQuery.bodyExcerpts 결과(대목 검사용)다.
     */
    public static SearchEvidence assemble(SearchResult result, List<RelevantStory> judged,
                                          List<StoryDetail> details, Map<Long, String> excerpts) {
        Map<Long, Candidate> candidates = result.candidates().stream()
                .collect(Collectors.toMap(Candidate::storyId, Function.identity(), (a, b) -> a));
        Map<Long, StoryDetail> detailById = details.stream()
                .collect(Collectors.toMap(StoryDetail::storyId, Function.identity(), (a, b) -> a));

        List<Item> items = new ArrayList<>();
        for (RelevantStory story : judged) {
            Candidate candidate = candidates.get(story.storyId());
            StoryDetail detail = detailById.get(story.storyId());
            if (candidate == null || detail == null || items.stream().anyMatch(i -> i.storyId() == story.storyId())) {
                continue;
            }
            items.add(new Item(items.size() + 1, detail.storyId(), detail.title(), detail.url(),
                    candidate.matchedPlans(), detail.category(), detail.summary(),
                    detail.communityReaction(), detail.practicalImplication(),
                    verifiedPassage(story.passage(), excerpts.get(story.storyId()))));
        }
        return new SearchEvidence(result.plans(), result.merged(), items);
    }

    /** 원문에 그대로 있는 대목만 남긴다. 공백 차이는 무시한다. */
    static String verifiedPassage(String passage, String excerpt) {
        if (passage == null || passage.isBlank() || excerpt == null) {
            return null;
        }
        String clean = passage.strip();
        if (!squash(excerpt).contains(squash(clean))) {
            return null;
        }
        return clean.length() <= PASSAGE_MAX ? clean : clean.substring(0, PASSAGE_MAX);
    }

    private static String squash(String text) {
        return text.replaceAll("\\s+", " ");
    }
}
