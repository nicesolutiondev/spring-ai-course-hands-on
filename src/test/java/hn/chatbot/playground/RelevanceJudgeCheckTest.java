package hn.chatbot.playground;

import hn.chatbot.ai.AnalysisTarget;
import hn.chatbot.ai.RelevanceJudge;
import hn.chatbot.ai.RelevantStory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RelevanceJudge 확인. 준비된 후보 다섯 건 중 질문에 맞는 것을 고르고,
 * 원문에 그대로 있는 대목을 뽑는지 본다. 적재된 데이터는 필요 없다.
 *
 *   ./gradlew playground --tests '*RelevanceJudgeCheckTest'
 */
@Tag("playground")
@SpringBootTest
class RelevanceJudgeCheckTest {

    static final List<AnalysisTarget> TARGETS = List.of(
            AnalysisTarget.forJudge(101, "Claude Code struggles with large refactors",
                    "Developers report that AI coding agents lose context on multi-file refactors.",
                    "We tried an AI coding agent on a refactor that touched forty files. It handled the first "
                            + "few well, but after that it lost track of the renamed interfaces and reintroduced "
                            + "old names. The agent kept no memory of decisions made earlier in the session."),
            AnalysisTarget.forJudge(102, "Swiss government replaces 3,000 Microsoft seats",
                    "A Swiss agency moves office software to open-source alternatives.",
                    "The federal agency announced that 3,000 workstations will move from Microsoft Office "
                            + "to LibreOffice and Nextcloud over the next two years."),
            AnalysisTarget.forJudge(103, "Why AI agents still need human code review",
                    "An essay on hallucinated APIs and silent bugs introduced by coding assistants.",
                    "Coding assistants confidently call APIs that do not exist. The code compiles in the "
                            + "happy path and fails silently at runtime, which is why every agent-written change "
                            + "still needs a human reviewer."),
            AnalysisTarget.forJudge(104, "LG smart TVs listen through the microphone",
                    "Researchers find LG TVs sending audio fingerprints to ad servers.",
                    "Packet captures show the TV uploading audio fingerprints every fifteen seconds."),
            AnalysisTarget.forJudge(105, "Broadcom ends VDDK distribution",
                    "VMware customers lose a key library needed for backup and migration tools.",
                    "Broadcom removed the public download of VDDK, which backup vendors rely on."));

    @Autowired RelevanceJudge judge;

    @Test
    @DisplayName("질문에 맞는 후보를 고르고 원문 그대로의 대목을 뽑는다")
    void selectsRelevant() {
        List<RelevantStory> selected = judge.selectRelevant("What are the limits of AI coding agents?", TARGETS, 5);

        Map<Long, AnalysisTarget> byId = TARGETS.stream()
                .collect(Collectors.toMap(AnalysisTarget::storyId, Function.identity()));
        selected.forEach(s -> System.out.printf("  %d  대목: %s%n", s.storyId(), s.passage()));

        assertThat(selected).as("최소 1건은 남아야 한다").isNotEmpty().hasSizeLessThanOrEqualTo(5);
        assertThat(selected).extracting(RelevantStory::storyId).as("받은 후보의 id 만 돌려줘야 한다")
                .isSubsetOf(byId.keySet());
        assertThat(selected).extracting(RelevantStory::storyId).as("코딩 에이전트 이슈가 포함돼야 한다")
                .containsAnyOf(101L, 103L);
        assertThat(selected).extracting(RelevantStory::storyId).as("무관한 TV 이슈는 빠져야 한다")
                .doesNotContain(104L);
        assertThat(selected).as("대목이 있으면 원문에 그대로 있어야 한다")
                .allSatisfy(s -> {
                    if (s.passage() != null && !s.passage().isBlank()) {
                        assertThat(squash(byId.get(s.storyId()).excerpt())).contains(squash(s.passage().strip()));
                    }
                });
        assertThat(selected).as("관련 후보 중 하나 이상에서는 대목을 뽑아야 한다")
                .anySatisfy(s -> assertThat(s.passage()).isNotBlank());
    }

    private static String squash(String text) {
        return text.replaceAll("\\s+", " ");
    }
}
