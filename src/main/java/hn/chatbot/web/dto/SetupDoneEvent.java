package hn.chatbot.web.dto;

import hn.chatbot.service.setup.model.SetupState;

public record SetupDoneEvent(SetupState state, int completed, int excluded, int searchableStories) {
}
