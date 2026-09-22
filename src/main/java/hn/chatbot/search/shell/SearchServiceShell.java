package hn.chatbot.search.shell;

import hn.chatbot.search.SearchPlan;
import hn.chatbot.search.SearchResult;
import hn.chatbot.search.SearchService;
import hn.chatbot.search.port.StoryIndexQuery;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 4개 계획을 전부 수행하고 storyId 기준으로 중복을 제거한다(최대 20건).
 *
 * 점수는 합치지 않는다. 불리언 · ts_rank · 코사인 유사도는 서로 비교할 수 없는 척도다.
 * 최종 순위는 RelevanceJudge 가 정한다.
 *
 * 계획마다 PlanSummary 를 남긴다. 건수와 함께 계획이 돌려준 실행 조건을 그대로 담는다.
 *
 * 계획이 돌려주는 것은 storyId 와 점수뿐이다. 중복을 제거한 뒤
 * StoryIndexQuery.hydrate 로 제목과 요약을 채워 후보를 만들고, 후보마다 찾아낸 계획 번호를 기록한다.
 *
 * 수강생이 채운다. 계획 4개와 색인 조회가 주입돼 있다.
 */
@Service
public class SearchServiceShell implements SearchService {

    private final List<SearchPlan> plans;
    private final StoryIndexQuery index;

    public SearchServiceShell(List<SearchPlan> plans, StoryIndexQuery index) {
        this.plans = plans;
        this.index = index;
    }

    @Override
    public SearchResult search(String query, String techField, String category) {
        List<SearchPlan> ordered = new java.util.ArrayList<>(plans);
        ordered.sort(java.util.Comparator.comparingInt(SearchPlan::number));
        List<hn.chatbot.search.PlanSummary> summaries = new java.util.ArrayList<>();
        java.util.Map<Long, java.util.LinkedHashSet<Integer>> matched = new java.util.LinkedHashMap<>();
        for (SearchPlan plan : ordered) {
            hn.chatbot.search.PlanRun run = plan.execute(query, techField, category, 5);
            summaries.add(new hn.chatbot.search.PlanSummary(plan.number(), plan.name(), run.hits().size(), run.condition()));
            for (hn.chatbot.search.PlanHit hit : run.hits()) matched.computeIfAbsent(hit.storyId(), k -> new java.util.LinkedHashSet<>()).add(plan.number());
        }
        List<Long> ids = new java.util.ArrayList<>(matched.keySet());
        if (ids.size() > 20) ids = new java.util.ArrayList<>(ids.subList(0, 20));
        java.util.Map<Long, hn.chatbot.search.Candidate> byId = new java.util.HashMap<>();
        for (hn.chatbot.search.Candidate c : index.hydrate(ids)) byId.put(c.storyId(), c);
        List<hn.chatbot.search.Candidate> candidates = new java.util.ArrayList<>();
        for (Long id : ids) { hn.chatbot.search.Candidate c = byId.get(id); if (c != null) candidates.add(new hn.chatbot.search.Candidate(c.storyId(), c.title(), c.summary(), new java.util.ArrayList<>(matched.get(id)))); }
        return new SearchResult(summaries, ids.size(), candidates);
    }
}
