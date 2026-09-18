package hn.chatbot.search;

import java.util.List;

/**
 * 4개 계획의 실행 결과와 중복 제거를 마친 후보들.
 *
 * web/dto 를 참조하지 않는다. 화면과 모델에 넘길 형태로 옮기는 것은
 * IssueTools.searchIssues 의 몫이다.
 */
public record SearchResult(List<PlanSummary> plans, int merged, List<Candidate> candidates) {
}
