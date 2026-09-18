package hn.chatbot.service.setup;

import hn.chatbot.ai.IssueAnalysis;
import hn.chatbot.domain.Analysis;

import java.util.List;

/**
 * 6단계 구조화 출력을 저장 가능한 엔티티로 옮긴다.
 *
 * 구조화 출력은 값이 없는 String 필드에 null 이 아니라 빈 문자열을 준다.
 * 그대로 저장하면 analysis 테이블의 제약이 깨진다.
 *
 *   CHECK (suitable = (unsuitable_reason IS NULL))
 *
 * 제약은 양방향이라 정규화도 양방향이어야 한다.
 *   - suitable = true 인데 사유가 "" → NULL 이 아니므로 위반
 *   - suitable = false 인데 사유가 "" → 빈 문자열을 NULL 로 바꾸면 이번엔
 *       NULL 이라서 위반. 모델이 사유를 안 줬다는 뜻이므로 대체값을 넣는다
 *
 * Spring AI 를 부르지 않는 순수 변환이라 완성본으로 제공한다.
 */
public final class AnalysisMapper {

    /** 모델이 suitable=false 로 판정하고도 사유를 주지 않았을 때. 제외 사유 집계에 그대로 드러난다. */
    static final String UNSPECIFIED_REASON = "UNSPECIFIED";

    private AnalysisMapper() {
    }

    /** storyId 는 구조화 출력에 없으므로 부르는 쪽이 넘긴다. */
    public static Analysis toEntity(long storyId, IssueAnalysis a) {
        return new Analysis(
                storyId,
                blankToNull(a.summary()),
                blankToNull(a.category()),
                blankToNull(a.techField()),
                toArray(a.keywords()),
                blankToNull(a.communityReaction()),
                blankToNull(a.practicalImplication()),
                a.suitable(),
                reasonFor(a));
    }

    private static String reasonFor(IssueAnalysis a) {
        if (a.suitable()) {
            return null;
        }
        String reason = blankToNull(a.unsuitableReason());
        return reason == null ? UNSPECIFIED_REASON : reason;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String[] toArray(List<String> keywords) {
        return keywords == null ? new String[0] : keywords.toArray(String[]::new);
    }
}
