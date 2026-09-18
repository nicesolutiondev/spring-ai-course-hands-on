package hn.chatbot.web.dto;

import hn.chatbot.service.setup.model.LogKind;
import hn.chatbot.service.setup.model.LogState;

public record LogEntry(String at, LogKind kind, long storyId, String title, String meta, LogState state) {
}
