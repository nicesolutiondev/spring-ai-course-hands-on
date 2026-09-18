package hn.chatbot.ai;

import java.util.List;

/**
 * 파이프라인 8단계: 임베딩해 두 벡터 스토어에 적재한다.
 *
 * 요약은 스토리당 1행이고 청크는 N행이라 스토어를 나눈다.
 * 주입할 때 @Qualifier 가 필요하다. PgVectorStore 빈이 둘이라
 * 타입만으로는 주입되지 않는다.
 *
 * 메타데이터
 *
 * - 두 스토어 공통: storyId, suitable, techField, category. storyId 로 검색 결과를
 *   스토리 단위로 묶고, 나머지는 검색 필터로 쓴다
 * - 본문 스토어만: chunkSeq. 재색인할 때 같은 청크를 식별한다
 *
 * 본문 스토어에도 필터 값이 있어야 하므로 indexBody 도 분석 결과를 받는다.
 *
 * Document 의 id 는 UUID 문자열이어야 한다. PgVectorStore 가
 * UUID.fromString(id) 로 파싱하므로 storyId 나 "storyId-seq" 같은 값을 넣으면
 * 적재 시점에 IllegalArgumentException: UUID string too large 가 난다.
 * 스토리와의 연결은 id 가 아니라 메타데이터의 storyId 로 한다.
 */
public interface EmbeddingIndexer {

    void indexSummary(long storyId, IssueAnalysis analysis);

    void indexBody(long storyId, IssueAnalysis analysis, List<String> chunks);
}
