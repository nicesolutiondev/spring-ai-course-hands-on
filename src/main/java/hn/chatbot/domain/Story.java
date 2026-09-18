package hn.chatbot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;

/** HN 스토리 원본. id 는 HN 이 부여한 storyId 를 그대로 쓴다. */
@Entity
@Table(name = "story")
public class Story {

    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    /** Ask/Tell HN 은 null */
    private String url;

    /** HN 의 by */
    private String author;

    @Column(nullable = false)
    private int score;

    /** 전체 댓글 수 */
    @Column(nullable = false)
    private int descendants;

    /** 스토리 본문 (Ask/Tell HN) */
    @Column(name = "text")
    private String text;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "collected_at", insertable = false, updatable = false)
    private Instant collectedAt;

    protected Story() {
    }

    public Story(Long id, String title, String url, String author,
                 int score, int descendants, String text, Instant postedAt) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.author = author;
        this.score = score;
        this.descendants = descendants;
        this.text = text;
        this.postedAt = postedAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getAuthor() { return author; }
    public int getScore() { return score; }
    public int getDescendants() { return descendants; }
    public String getText() { return text; }
    public Instant getPostedAt() { return postedAt; }
    public Instant getCollectedAt() { return collectedAt; }
}
