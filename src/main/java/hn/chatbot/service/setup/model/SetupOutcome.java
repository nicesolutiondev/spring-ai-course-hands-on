package hn.chatbot.service.setup.model;

public record SetupOutcome(SetupState state, int completed, int excluded, int searchableStories) {
}
