package hn.chatbot.ai.shell;

import hn.chatbot.ai.ArticleBodyExtractor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 5단계 — 제목을 앵커로 삼아 정제 텍스트에서 본문만 남긴다. 메뉴·관련기사·푸터를 제외한다.
 *
 * 수강생이 채운다. ChatClient 는 주입돼 있다.
 */
@Component
public class ArticleBodyExtractorShell implements ArticleBodyExtractor {

    private final ChatClient chatClient;

    public ArticleBodyExtractorShell(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public Optional<String> extract(String title, String cleanedText) {
        String body = chatClient.prompt()
                .system("""
                        Extract only the article body from the supplied cleaned web-page text.
                        Use the title as an anchor to distinguish the article from surrounding page text.
                        Exclude navigation, advertisements, related articles, subscription prompts,
                        comments, legal notices, and footer text.
                        Return the body verbatim without commentary, labels, or Markdown fences.
                        If there is no article body, return an empty response.
                        """)
                .user("""
                        Title:
                        %s

                        Cleaned web-page text:
                        %s
                        """.formatted(title, cleanedText))
                .call()
                .content();

        return Optional.ofNullable(body)
                .map(String::trim)
                .filter(text -> !text.isEmpty());
    }
}
