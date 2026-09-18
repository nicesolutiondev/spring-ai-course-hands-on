package hn.chatbot.service.setup.model;

import java.time.Instant;

public record SetupRun(String runId, SetupState state, Instant startedAt) {
}
