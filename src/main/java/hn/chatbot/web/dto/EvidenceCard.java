package hn.chatbot.web.dto;

import java.util.List;

/**
 * 답변의 근거가 된 스토리 한 건.
 *
 * practicalImplication 과 passage 는 없을 수 있다. 화면은 그 줄을 생략한다.
 */
public record EvidenceCard(
        long storyId, String title, String url, String domain,
        List<Integer> matchedPlans, String category,
        String summary, String communityReaction, String practicalImplication,
        String passage) {
}
