package hn.chatbot.service.setup.model;

/** 파이프라인 8단계 중 한 단계. progress 는 0.0 ~ 1.0 이다. */
public record StageProgress(int step, String name, int count, StageState state, double progress) {
}
