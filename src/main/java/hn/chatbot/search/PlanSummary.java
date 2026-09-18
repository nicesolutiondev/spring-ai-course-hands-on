package hn.chatbot.search;

/**
 * 계획 하나의 실행 요약: 몇 번 계획이 어떤 조건으로 몇 건을 찾았나.
 *
 * web/dto 의 PlanResult 와 모양이 같지만 타입이 다르다. 저쪽은 화면이 읽는
 * 계약이고 이쪽은 검색 계층의 값이다.
 * 섞어두면 화면 사정으로 PlanResult 를 고칠 때 search/ 가 함께 흔들린다.
 */
public record PlanSummary(int plan, String name, int hits, String condition) {
}
