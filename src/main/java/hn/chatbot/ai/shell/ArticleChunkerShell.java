package hn.chatbot.ai.shell;

import hn.chatbot.ai.ArticleChunker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 7단계 — 문단 단위로 청킹한다. 경계가 식별되지 않거나 한 문단이 지나치게 길면 하드 리밋으로 나눈다.
 *
 * 수강생이 채운다. LLM을 사용하지 않고, 기계적으로 청킹하도록 한다.
 */
@Component
public class ArticleChunkerShell implements ArticleChunker {

    private static final int HARD_LIMIT = 2_000;

    @Override
    public List<String> chunk(String body) {
        List<String> chunks = new ArrayList<>();
        for (String paragraph : body.split("\\R\\s*\\R+")) {
            String text = paragraph.strip();
            for (int start = 0; start < text.length(); start += HARD_LIMIT) {
                chunks.add(text.substring(start, Math.min(start + HARD_LIMIT, text.length())));
            }
        }
        return chunks;
    }
}
