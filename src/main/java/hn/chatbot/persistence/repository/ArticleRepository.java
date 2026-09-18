package hn.chatbot.persistence.repository;

import hn.chatbot.domain.Article;
import hn.chatbot.domain.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    /**
     * 검색 계획 2 — 전문 검색.
     *
     * websearch_to_tsquery 는 따옴표와 OR 같은 검색 문법을 이해하고,
     * 파싱 실패 시 예외 대신 빈 tsquery 를 돌려준다.
     * ts_rank 는 IDF 가 없어 질의마다 범위가 달라지므로 다른 계획의 점수와 합치지 않는다.
     * techField · category 가 null 이면 그 축으로 거르지 않는다.
     */
    @Query(value = """
            SELECT a.story_id AS storyId,
                   CAST(ts_rank(ar.body_tsv, q) AS double precision) AS score
            FROM analysis a
            JOIN article ar ON ar.story_id = a.story_id,
                 websearch_to_tsquery('english', :query) q
            WHERE a.suitable AND ar.body_tsv @@ q
              AND (CAST(:techField AS text) IS NULL OR a.tech_field = CAST(:techField AS text))
              AND (CAST(:category AS text) IS NULL OR a.category = CAST(:category AS text))
            ORDER BY score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<RankedStory> searchByFullText(@Param("query") String query,
                                       @Param("techField") String techField,
                                       @Param("category") String category,
                                       @Param("limit") int limit);

    interface RankedStory {
        Long getStoryId();
        Double getScore();
    }

    long countByStatus(ArticleStatus status);

    /** 적합성 판단에 넘길 원문 앞부분. */
    @Query(value = """
            SELECT story_id AS storyId, left(body, :maxChars) AS excerpt
            FROM article
            WHERE story_id IN (:storyIds) AND body IS NOT NULL
            """, nativeQuery = true)
    List<Excerpt> findExcerpts(@Param("storyIds") List<Long> storyIds, @Param("maxChars") int maxChars);

    interface Excerpt {
        Long getStoryId();
        String getExcerpt();
    }
}
