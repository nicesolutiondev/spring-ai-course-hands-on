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
 * 원문 처리 결과. 성공과 실패를 모두 기록하며,
 * 이 행의 존재 여부가 중복 확인의 기준이다.
 *
 * body_tsv 는 생성 컬럼이라 매핑하지 않는다. body 가 바뀌면 DB 가 알아서 갱신한다.
 */
@Entity
@Table(name = "article")
public class Article {

    @Id
    @Column(name = "story_id")
    private Long storyId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ArticleStatus status;

    /** 기계적 정제 결과. 프롬프트를 고쳤을 때 원문을 다시 받지 않고 재실행하기 위해 남긴다. */
    @Column(name = "cleaned_text")
    private String cleanedText;

    @Column(name = "cleaned_len")
    private Integer cleanedLen;

    /** LLM 추출 본문 */
    private String body;

    @Generated(event = EventType.INSERT)
    @Column(name = "processed_at", insertable = false, updatable = false)
    private Instant processedAt;

    protected Article() {
    }

    public Article(Long storyId, ArticleStatus status, String cleanedText,
                   Integer cleanedLen, String body) {
        this.storyId = storyId;
        this.status = status;
        this.cleanedText = cleanedText;
        this.cleanedLen = cleanedLen;
        this.body = body;
    }

    public Long getStoryId() { return storyId; }
    public ArticleStatus getStatus() { return status; }
    public String getCleanedText() { return cleanedText; }
    public Integer getCleanedLen() { return cleanedLen; }
    public String getBody() { return body; }
    public Instant getProcessedAt() { return processedAt; }
}
