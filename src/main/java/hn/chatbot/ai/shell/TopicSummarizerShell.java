package hn.chatbot.ai.shell;

import hn.chatbot.ai.AnalysisTarget;
import hn.chatbot.ai.TopicSummarizer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 기술 분야 하나의 논의 흐름을 산문으로 요약한다. 스토리마다 제목 · 요약 · 커뮤니티 반응이 들어온다.
 *
 * 수강생이 채운다. ChatClient 는 주입돼 있다.
 */
@Component
public class TopicSummarizerShell implements TopicSummarizer {

    private final ChatClient chatClient;

    public TopicSummarizerShell(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String summarize(String techField, List<AnalysisTarget> targets) {
        if (targets == null || targets.isEmpty()) return "No stories are available for this topic.";
        StringBuilder user = new StringBuilder("Technical field: ").append(techField).append("\n\nStories:\n");
        for (AnalysisTarget target : targets.stream().limit(20).toList()) {
            user.append("Title: ").append(target.title()).append("\nSummary: ").append(target.summary())
                    .append("\nCommunity reaction: ").append(target.communityReaction()).append("\n---\n");
        }
        return chatClient.prompt()
                .system("""
                        Summarize the common trends across these Hacker News stories in the requested technical field.
                        Explain recurring themes, changes over time, and disagreements or community reactions.
                        Do not invent facts or discuss stories that are not provided. Write a concise, coherent
                        answer in the same language as the user-facing request.
                        """)
                .user(user.toString())
                .call()
                .content();
    }
}
