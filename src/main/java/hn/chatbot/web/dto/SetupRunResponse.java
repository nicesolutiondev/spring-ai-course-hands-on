package hn.chatbot.web.dto;

import hn.chatbot.service.setup.model.SetupState;

import java.time.Instant;

public record SetupRunResponse(String runId, SetupState state, Instant startedAt) {
}
