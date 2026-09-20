package hn.chatbot.search.plan;

import hn.chatbot.search.PlanConditions;
import hn.chatbot.search.PlanHit;
import hn.chatbot.search.PlanRun;
import hn.chatbot.search.SearchPlan;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 검색 계획 4 — 요약 벡터 검색.
 *
 * 요약은 스토리당 1행이라 접을 필요가 없다.
 *
 * 메타데이터 필터에서 suitable 은 항상 걸고, techField · category 는 값이 있을 때만 건다.
 * 둘 다 모델이 채우는 값이라 빈 문자열로 올 수 있다.
 *
 * 결과와 함께 실행한 조건을 돌려준다. 조건 문자열은 PlanConditions.vector 로 만든다.
 */
@Component
public class SummaryVectorPlan implements SearchPlan {

    private final PgVectorStore vectorStore;

    public SummaryVectorPlan(@Qualifier("summaryVectorStore") PgVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public int number() {
        return 4;
    }

    @Override
    public String name() {
        return "요약 벡터 검색";
    }

    @Override
    public PlanRun execute(String query, String techField, String category, int limit) {
        String field = PlanConditions.blankToNull(techField);
        String type = PlanConditions.blankToNull(category);
        String condition = PlanConditions.vector(query, field, type, limit);
        if (query == null || query.isBlank()) {
            return new PlanRun(List.of(), condition);
        }

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(limit)
                .similarityThreshold(0.0)                    // 하한을 두지 않고 재순위에 맡긴다
                .filterExpression(filter(field, type))
                .build());

        List<PlanHit> hits = documents.stream()
                .map(document -> new PlanHit(
                        ((Number) document.getMetadata().get("storyId")).longValue(),
                        1.0 - document.getScore()))
                .toList();

        return new PlanRun(hits, condition);
    }

    private static Filter.Expression filter(String techField, String category) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op expression = b.eq("suitable", true);
        if (techField != null) {
            expression = b.and(expression, b.eq("techField", techField));
        }
        if (category != null) {
            expression = b.and(expression, b.eq("category", category));
        }
        return expression.build();
    }
}
