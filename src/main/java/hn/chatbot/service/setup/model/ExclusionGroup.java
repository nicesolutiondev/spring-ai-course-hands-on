package hn.chatbot.service.setup.model;

import java.util.Map;

public record ExclusionGroup(int total, Map<String, Integer> reasons) {
}
