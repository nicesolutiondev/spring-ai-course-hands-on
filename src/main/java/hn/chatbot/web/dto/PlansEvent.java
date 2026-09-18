package hn.chatbot.web.dto;

import java.util.List;

/** 검색 계획 4개의 히트 수와 중복 제거 후 후보 수. */
public record PlansEvent(List<PlanResult> plans, int merged) {
}
