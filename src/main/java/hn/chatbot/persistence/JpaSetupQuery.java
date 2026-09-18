package hn.chatbot.persistence;

import hn.chatbot.domain.ArticleStatus;
import hn.chatbot.persistence.repository.AnalysisRepository;
import hn.chatbot.persistence.repository.ArticleRepository;
import hn.chatbot.service.setup.model.ExclusionGroup;
import hn.chatbot.service.setup.model.Exclusions;
import hn.chatbot.service.setup.port.SetupQuery;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** SetupQuery 포트의 영속 어댑터. */
@Component
public class JpaSetupQuery implements SetupQuery {

    private final ArticleRepository articles;
    private final AnalysisRepository analyses;

    public JpaSetupQuery(ArticleRepository articles, AnalysisRepository analyses) {
        this.articles = articles;
        this.analyses = analyses;
    }

    @Override
    public boolean isProcessed(long storyId) {
        return articles.existsById(storyId);
    }

    @Override
    public int countSearchableStories() {
        return analyses.countSuitable();
    }

    @Override
    public Exclusions exclusions() {
        Map<String, Integer> mechanical = new LinkedHashMap<>();
        for (ArticleStatus status : ArticleStatus.values()) {
            if (status == ArticleStatus.PASS) {
                continue;
            }
            long count = articles.countByStatus(status);
            if (count > 0) {
                mechanical.put(status.name(), (int) count);
            }
        }
        Map<String, Integer> judged = new LinkedHashMap<>();
        analyses.countUnsuitableReasons()
                .forEach(row -> judged.put(row.getValue(), (int) row.getCount()));

        return new Exclusions(group(mechanical), group(judged));
    }

    private static ExclusionGroup group(Map<String, Integer> reasons) {
        return new ExclusionGroup(reasons.values().stream().mapToInt(Integer::intValue).sum(), reasons);
    }
}
