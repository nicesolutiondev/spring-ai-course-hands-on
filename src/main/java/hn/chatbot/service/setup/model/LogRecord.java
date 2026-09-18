package hn.chatbot.service.setup.model;

public record LogRecord(String at, LogKind kind, long storyId, String title, String meta, LogState state) {
}
