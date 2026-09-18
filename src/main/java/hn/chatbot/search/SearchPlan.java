package hn.chatbot.search;


/**
 * 검색 계획 하나.
 *
 * 네 계획을 전부 수행하고 각각 상위 5건을 받는다. 하나가 0건이어도
 * 나머지가 채운다. 계획 1 은 자연어 문장 질의에서 대부분 0건이고 고유명사 질의에서만
 * 맞는다.
 *
 * 필터: suitable 은 항상 건다. techField · category 는 값이 있을 때만 건다.
 * 둘 다 모델이 searchIssues 의 인자로 채운 값이다.
 *
 * 결과와 함께 실제로 실행한 조건을 돌려준다. 조건 문자열은 PlanConditions 로 만든다.
 */
public interface SearchPlan {

    int number();

    String name();

    PlanRun execute(String query, String techField, String category, int limit);
}
