package hn.chatbot.ai.shell;

import hn.chatbot.ai.HarmfulPhraseDetector;
import hn.chatbot.ai.MaskingResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 3단계 — 검열에 걸린 댓글에서 유해 구간만 추출한다. 원문을 다시 쓰게 하면 내용이 변조되므로 치환할 구간만 받는다.
 *
 * 수강생이 채운다. ChatClient 는 주입돼 있다.
 */
@Component
public class HarmfulPhraseDetectorShell implements HarmfulPhraseDetector {

    private final ChatClient chatClient;

    public HarmfulPhraseDetectorShell(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public MaskingResult detect(String text) {
        MaskingResult result = chatClient.prompt()
                .system("""
                        Identify only the harmful phrases in the user's text that should be masked.
                        Each phrase must be an exact, contiguous substring from the original text,
                        including its original spelling and punctuation. Do not paraphrase, correct,
                        explain, or return the whole text. If there are no harmful phrases, return
                        an empty list.
                        """)
                .user(text)
                .call()
                .entity(MaskingResult.class);

        List<String> phrases = result.harmfulPhrases();
        if (phrases == null) {
            phrases = List.of();
        } else {
            phrases = phrases.stream()
                    .filter(text::contains)
                    .toList();
        }
        return new MaskingResult(phrases);
    }
}
