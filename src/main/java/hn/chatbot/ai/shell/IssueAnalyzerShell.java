package hn.chatbot.ai.shell;

import hn.chatbot.ai.IssueAnalysis;
import hn.chatbot.ai.IssueAnalyzer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 6단계 — 구조화 출력으로 분석 결과를 받는다. techField 와 category 의 값 목록을 프롬프트에 반드시 넣는다.
 *
 * 수강생이 채운다. ChatClient 는 주입돼 있다.
 */
@Component
public class IssueAnalyzerShell implements IssueAnalyzer {

    private static final List<String> TECH_FIELDS = List.of(
            "AI_LLM", "SECURITY_PRIVACY", "OPEN_SOURCE", "INFRASTRUCTURE_ENTERPRISE",
            "PLATFORM_POLICY", "DEV_CULTURE_PRACTICE", "HARDWARE", "MOBILITY", "NON_TECHNICAL");
    private static final List<String> CATEGORIES = List.of(
            "OFFICIAL_ANNOUNCEMENT", "RELEASE_NOTES", "NEWS_REPORT", "OPINION_ESSAY",
            "TECHNICAL_DEEP_DIVE", "RESEARCH_PAPER", "SHOW_HN_PROJECT", "ASK_TELL_HN",
            "PRODUCT_MARKETING");

    private final ChatClient chatClient;

    public IssueAnalyzerShell(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public IssueAnalysis analyze(String title, String body, List<String> topComments) {
        return chatClient.prompt()
                .system("""
                        Analyze a Hacker News story for a technical-issue search service.
                        Summarize the article, classify it, identify its technical field, extract search
                        keywords, describe the top-comment reaction, and state a practical implication.

                        Set suitable to true only when the story is useful for a technology-issue search
                        service. If suitable is false, give a concise unsuitableReason. If suitable is true,
                        leave unsuitableReason empty.

                        techField must be exactly one of: %s
                        category must be exactly one of: %s
                        Return 6 to 10 specific, useful keywords.
                        """.formatted(String.join(", ", TECH_FIELDS), String.join(", ", CATEGORIES)))
                .user("""
                        Title: %s

                        Article body:
                        %s

                        Top comments:
                        %s
                        """.formatted(title, body, String.join("\n---\n", topComments)))
                .call()
                .entity(IssueAnalysis.class);
    }
}
