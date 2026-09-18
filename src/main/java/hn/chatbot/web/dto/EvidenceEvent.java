package hn.chatbot.web.dto;

import java.util.List;

public record EvidenceEvent(int selected, List<EvidenceCard> evidence) {
}
