package hn.chatbot.ai.shell;

import hn.chatbot.ai.AnalysisTarget;
import hn.chatbot.ai.RelevanceJudge;
import hn.chatbot.ai.RelevantStory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

/**
 * 후보 중 질의에 실제로 답이 되는 것을 고르고, 근거 대목을 뽑는다.
 * 계약은 RelevanceJudge 의 Javadoc 에 있다.
 *
 * 수강생이 채운다. ChatClient 는 주입돼 있다. 결과는 구조화 출력으로 받는다.
 */
@Component
public class RelevanceJudgeShell implements RelevanceJudge {

    private static final int MAX_TARGETS = 20;
    private static final int MAX_QUESTION_CHARS = 2_000;
    private static final int MAX_TITLE_CHARS = 500;
    private static final int MAX_SUMMARY_CHARS = 2_000;
    private static final int MAX_EXCERPT_CHARS = 2_000;

    private final ChatClient chatClient;

    public RelevanceJudgeShell(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public List<RelevantStory> selectRelevant(String question, List<AnalysisTarget> targets, int max) {
        if (question == null || question.isBlank() || targets == null || targets.isEmpty() || max <= 0) return List.of();
        StringBuilder user = new StringBuilder("Question: ").append(limit(question, MAX_QUESTION_CHARS)).append("\n\nCandidates:\n");
        for (AnalysisTarget target : targets.stream().limit(MAX_TARGETS).toList()) {
            user.append("ID ").append(target.storyId()).append("\nTitle: ")
                    .append(limit(target.title(), MAX_TITLE_CHARS)).append("\nSummary: ")
                    .append(limit(target.summary(), MAX_SUMMARY_CHARS)).append("\nExcerpt: ")
                    .append(limit(target.excerpt(), MAX_EXCERPT_CHARS)).append("\n---\n");
        }
        List<RelevantStory> selected = chatClient.prompt().system("Select genuinely relevant candidates. Return a JSON array with storyId and passage. Use only candidate IDs, at most the requested number. passage must be an exact contiguous quote from the excerpt, at most 300 characters, or null.").user(user.toString()).call().entity(new ParameterizedTypeReference<List<RelevantStory>>() {});
        if (selected == null) return List.of();
        java.util.Set<Long> ids = targets.stream().map(AnalysisTarget::storyId).collect(java.util.stream.Collectors.toSet());
        return selected.stream().filter(item -> item != null && ids.contains(item.storyId())).limit(max).toList();
    }

    private static String limit(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }
}
