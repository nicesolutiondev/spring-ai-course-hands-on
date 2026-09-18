package hn.chatbot.search.plan;

import hn.chatbot.search.PlanRun;
import hn.chatbot.search.SearchPlan;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 검색 계획 3 — 원문 청크 벡터 검색.
 *
 * 한 스토리가 여러 청크를 가지므로 스토리 단위로 접는다. 순위는 가장 가까운 청크 기준이다.
 *
 * 메타데이터 필터에서 suitable 은 항상 걸고, techField · category 는 값이 있을 때만 건다.
 * 둘 다 모델이 채우는 값이라 빈 문자열로 올 수 있다.
 *
 * 결과와 함께 실행한 조건을 돌려준다. 조건 문자열은 PlanConditions.vector 로 만든다.
 *
 * 수강생이 채운다.
 */
@Component
public class BodyVectorPlan implements SearchPlan {

    private final PgVectorStore vectorStore;

    public BodyVectorPlan(@Qualifier("bodyVectorStore") PgVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public int number() {
        return 3;
    }

    @Override
    public String name() {
        return "원문 청크 벡터 검색";
    }

    @Override
    public PlanRun execute(String query, String techField, String category, int limit) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다. 이 메서드를 채우세요.");
    }
}
