package hn.chatbot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * LLM 분석 결과. IssueAnalysis 레코드가 그대로 대응된다.
 *
 * category 와 techField 는 폴백 규칙으로 값이 늘 수 있어 ENUM 이 아니라 TEXT 다.
 *
 * unsuitableReason 은 빈 문자열이면 안 된다. DB 에
 * CHECK (suitable = (unsuitable_reason IS NULL)) 가 걸려 있는데
 * 구조화 출력은 값이 없는 String 필드에 null 이 아니라 "" 을 채워 돌려준다.
 * 저장 전에 null 로 정규화해야 한다.
 */
@Entity
@Table(name = "analysis")
public class Analysis {

    @Id
    @Column(name = "story_id")
    private Long storyId;

    private String summary;

    private String category;

    @Column(name = "tech_field")
    private String techField;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "keywords", nullable = false, columnDefinition = "text[]")
    private String[] keywords = new String[0];

    @Column(name = "community_reaction")
    private String communityReaction;

    @Column(name = "practical_implication")
    private String practicalImplication;

    @Column(nullable = false)
    private boolean suitable;

    @Column(name = "unsuitable_reason")
    private String unsuitableReason;

    @Generated(event = EventType.INSERT)
    @Column(name = "analyzed_at", insertable = false, updatable = false)
    private Instant analyzedAt;

    protected Analysis() {
    }

    public Analysis(Long storyId, String summary, String category, String techField,
                    String[] keywords, String communityReaction, String practicalImplication,
                    boolean suitable, String unsuitableReason) {
        this.storyId = storyId;
        this.summary = summary;
        this.category = category;
        this.techField = techField;
        this.keywords = keywords == null ? new String[0] : keywords;
        this.communityReaction = communityReaction;
        this.practicalImplication = practicalImplication;
        this.suitable = suitable;
        this.unsuitableReason = unsuitableReason;
    }

    public Long getStoryId() { return storyId; }
    public String getSummary() { return summary; }
    public String getCategory() { return category; }
    public String getTechField() { return techField; }
    public String[] getKeywords() { return keywords; }
    public String getCommunityReaction() { return communityReaction; }
    public String getPracticalImplication() { return practicalImplication; }
    public boolean isSuitable() { return suitable; }
    public String getUnsuitableReason() { return unsuitableReason; }
    public Instant getAnalyzedAt() { return analyzedAt; }
}
