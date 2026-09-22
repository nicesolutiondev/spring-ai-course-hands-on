package hn.chatbot.ai.shell;

import hn.chatbot.ai.CommentModerator;
import hn.chatbot.ai.ModerationProperties;
import hn.chatbot.ai.ModerationVerdict;
import org.springframework.ai.moderation.CategoryScores;
import org.springframework.ai.moderation.ModerationModel;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 3단계 — ModerationModel 로 댓글의 카테고리별 점수를 받는다.
 *
 * isFlagged() 를 그대로 쓰지 않는다. 우리가 고른 카테고리의 점수만
 * 임계값과 비교한다. harassment 계열은 뺀다 — 기술 커뮤니티의 제품·기업 비판이 대량으로 걸린다.
 * 카테고리 목록과 임계값은 application.yml 에서 주입된다.
 *
 * 수강생이 채운다. 이 클래스가 비어 있으면 안전 필터링 테스트가 실패한다.
 */
@Component
public class CommentModeratorShell implements CommentModerator {

    private final ModerationModel moderationModel;
    private final ModerationProperties properties;

    public CommentModeratorShell(ModerationModel moderationModel, ModerationProperties properties) {
        this.moderationModel = moderationModel;
        this.properties = properties;
    }

    @Override
    public ModerationVerdict inspect(String text) {
        CategoryScores categoryScores = moderationModel.call(new ModerationPrompt(text))
                .getResult()
                .getOutput()
                .getResults()
                .get(0)
                .getCategoryScores();

        Map<String, Double> scores = scoresOf(categoryScores);
        Map.Entry<String, Double> top = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
        boolean flagged = properties.categories().stream()
                .map(scores::get)
                .filter(score -> score != null)
                .anyMatch(score -> score >= properties.threshold());

        return new ModerationVerdict(flagged, top.getKey(), top.getValue(), scores);
    }

    private Map<String, Double> scoresOf(CategoryScores scores) {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("sexual", scores.getSexual());
        values.put("sexual/minors", scores.getSexualMinors());
        values.put("hate", scores.getHate());
        values.put("hate/threatening", scores.getHateThreatening());
        values.put("harassment", scores.getHarassment());
        values.put("harassment/threatening", scores.getHarassmentThreatening());
        values.put("violence", scores.getViolence());
        values.put("violence/graphic", scores.getViolenceGraphic());
        values.put("self-harm", scores.getSelfHarm());
        values.put("self-harm/intent", scores.getSelfHarmIntent());
        values.put("self-harm/instructions", scores.getSelfHarmInstructions());
        values.put("health", scores.getHealth());
        values.put("financial", scores.getFinancial());
        values.put("law", scores.getLaw());
        values.put("pii", scores.getPii());
        return values;
    }
}
