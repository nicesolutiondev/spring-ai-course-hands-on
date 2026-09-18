package hn.chatbot.web.dto;

import hn.chatbot.service.setup.model.StageState;

public record StageStatus(int step, String name, int count, StageState state, double progress) {
}
