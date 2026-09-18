package hn.chatbot.web.dto;

/** condition 은 그 계획이 실제로 실행한 조건이다. 사이드바가 그대로 표시한다. */
public record PlanResult(int plan, String name, int hits, String condition) {
}
