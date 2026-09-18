package hn.chatbot.playground;

import hn.chatbot.ai.HarmfulPhraseDetector;
import hn.chatbot.ai.MaskingResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HarmfulPhraseDetector 확인. 돌려준 구간이 원문에 그대로 있어야 애플리케이션이 치환할 수 있다.
 *
 *   ./gradlew playground --tests '*HarmfulPhraseDetectorCheckTest'
 */
@Tag("playground")
@SpringBootTest
class HarmfulPhraseDetectorCheckTest {

    private static final String HARMFUL =
            "This release is fine, but whoever wrote the installer is a worthless idiot and should be beaten up.";

    private static final String BENIGN =
            "The new release fixes the memory leak and the installer is much faster now.";

    @Autowired HarmfulPhraseDetector detector;

    @Test
    @DisplayName("유해 구간만 원문 그대로 돌려준다")
    void returnsPhrasesFromOriginal() {
        MaskingResult result = detector.detect(HARMFUL);

        System.out.println("원문: " + HARMFUL);
        System.out.println("구간: " + result.harmfulPhrases());

        assertThat(result.harmfulPhrases()).as("유해 구간이 하나 이상 있어야 한다").isNotEmpty();
        assertThat(result.harmfulPhrases())
                .as("모든 구간은 원문에 그대로 있어야 치환된다")
                .allSatisfy(phrase -> assertThat(HARMFUL).contains(phrase));
        assertThat(String.join("", result.harmfulPhrases()).length())
                .as("원문 전체가 아니라 구간만 돌려줘야 한다")
                .isLessThan(HARMFUL.length());
    }

    @Test
    @DisplayName("유해 표현이 없으면 빈 목록을 돌려준다")
    void returnsNothingForBenignText() {
        MaskingResult result = detector.detect(BENIGN);

        System.out.println("원문: " + BENIGN);
        System.out.println("구간: " + result.harmfulPhrases());
        assertThat(result.harmfulPhrases()).isEmpty();
    }
}
