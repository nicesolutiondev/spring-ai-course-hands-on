package hn.chatbot.search;

import java.util.List;

/**
 * 계획 하나의 실행 결과와 실제로 실행한 조건.
 *
 * condition 은 두 곳에서 쓰인다. 화면 사이드바가 계획마다 그대로 표시하고,
 * 모델이 도구 결과로 받아 답변에서 해석한다("공식 발표로 한정해 찾았지만 1건뿐이었습니다").
 * 문자열은 PlanConditions 로 만든다.
 */
public record PlanRun(List<PlanHit> hits, String condition) {
}
