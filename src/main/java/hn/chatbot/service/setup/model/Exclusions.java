package hn.chatbot.service.setup.model;

/** 제외 사유 2계층. mechanical 은 기계적 정제, judged 는 LLM 판정이다. */
public record Exclusions(ExclusionGroup mechanical, ExclusionGroup judged) {
}
