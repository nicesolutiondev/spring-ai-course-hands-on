package hn.chatbot.persistence.repository;

import hn.chatbot.domain.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    /**
     * 데이터 준비 판정. 탭 잠금(SetupStatus.searchableStories)과
     * /api/topics 의 stats.stories 가 같이 쓴다.
     */
    @Query("SELECT count(a) FROM Analysis a WHERE a.suitable = true")
    int countSuitable();

    /**
     * 검색 계획 1 — 키워드 완전 일치.
     *
     * 배열 겹침(&&)이라 형태소 분석이 필요 없다. 질의가 자연어 문장이면
     * 대부분 0건이고, pgvector 나 VMware 같은 고유명사 질의에서 가장 정확하다.
     * techField · category 가 null 이면 그 축으로 거르지 않는다.
     */
    @Query(value = """
            SELECT story_id
            FROM analysis
            WHERE suitable AND keywords && CAST(:keywords AS text[])
              AND (CAST(:techField AS text) IS NULL OR tech_field = CAST(:techField AS text))
              AND (CAST(:category AS text) IS NULL OR category = CAST(:category AS text))
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> searchByKeywords(@Param("keywords") String[] keywords,
                                @Param("techField") String techField,
                                @Param("category") String category,
                                @Param("limit") int limit);

    /** 주제 분포 — 주제 탐색 탭과 listTopics 도구가 같이 쓴다. */
    @Query(value = """
            SELECT tech_field AS value, count(*) AS count
            FROM analysis
            WHERE suitable AND tech_field IS NOT NULL
            GROUP BY tech_field
            ORDER BY count(*) DESC
            """, nativeQuery = true)
    List<FacetRow> countByTechField();

    @Query(value = """
            SELECT category AS value, count(*) AS count
            FROM analysis
            WHERE suitable AND category IS NOT NULL
            GROUP BY category
            ORDER BY count(*) DESC
            """, nativeQuery = true)
    List<FacetRow> countByCategory();

    @Query("SELECT count(a) FROM Analysis a WHERE a.suitable = true AND a.techField = :techField")
    int countSuitableByTechField(@Param("techField") String techField);

    /** countStories 도구용 건수. techField · category 가 null 이면 그 축으로 거르지 않는다. */
    @Query(value = """
            SELECT count(*)
            FROM analysis
            WHERE suitable
              AND (CAST(:techField AS text) IS NULL OR tech_field = CAST(:techField AS text))
              AND (CAST(:category AS text) IS NULL OR category = CAST(:category AS text))
            """, nativeQuery = true)
    int countSuitable(@Param("techField") String techField, @Param("category") String category);

    /** 제외 사유별 집계. LLM 판정으로 걸러진 것만 대상이다. */
    @Query(value = """
            SELECT unsuitable_reason AS value, count(*) AS count
            FROM analysis
            WHERE NOT suitable AND unsuitable_reason IS NOT NULL
            GROUP BY unsuitable_reason
            ORDER BY count(*) DESC
            """, nativeQuery = true)
    List<FacetRow> countUnsuitableReasons();

    /** 저장소 전체의 고유 키워드 수. */
    @Query(value = """
            SELECT count(DISTINCT k)
            FROM analysis a, unnest(a.keywords) AS k
            WHERE a.suitable
            """, nativeQuery = true)
    int countDistinctKeywords();

    /**
     * 클러스터별 스토리 목록. recent 가 true 면 최신순, 아니면 점수순이다.
     * techField 가 null 이면 전체가 대상이다.
     */
    @Query(value = """
            SELECT s.id            AS storyId,
                   s.title         AS title,
                   s.url           AS url,
                   a.summary       AS summary,
                   s.score         AS score,
                   s.descendants   AS commentCount,
                   a.category      AS category,
                   a.keywords      AS keywords,
                   a.community_reaction AS communityReaction
            FROM analysis a
            JOIN story s ON s.id = a.story_id
            WHERE a.suitable
              AND (:techField IS NULL OR a.tech_field = :techField)
            ORDER BY CASE WHEN :recent THEN s.posted_at END DESC NULLS LAST,
                     CASE WHEN NOT :recent THEN s.score END DESC NULLS LAST
            LIMIT :limit
            """, nativeQuery = true)
    List<StoryRow> findStories(@Param("techField") String techField,
                               @Param("recent") boolean recent,
                               @Param("limit") int limit);

    List<Analysis> findBySuitableTrue();

    /** 분포 조회의 투영. */
    interface FacetRow {
        String getValue();
        long getCount();
    }

    /** 스토리 목록 조회의 투영. 서비스 모델로 옮기는 것은 어댑터의 몫이다. */
    interface StoryRow {
        long getStoryId();
        String getTitle();
        String getUrl();
        String getSummary();
        int getScore();
        int getCommentCount();
        String getCategory();
        String[] getKeywords();
        String getCommunityReaction();
    }
}
