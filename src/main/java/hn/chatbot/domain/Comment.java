package hn.chatbot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * HN 댓글. 수집 범위는 application.yml 의 설정값이고 트리는 depth 2 에서 끊는다.
 *
 * parentId 는 스토리 ID 일 수도 댓글 ID 일 수도 있어 외래키를 걸지 않는다.
 * text 에는 검열을 거친 뒤의 마스킹된 텍스트가 들어간다.
 */
@Entity
@Table(name = "comment")
public class Comment {

    @Id
    private Long id;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    /** 스토리 또는 상위 댓글 */
    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    private String author;

    @Column(name = "text")
    private String text;

    /** 최상위 = 1, 대댓글 = 2 */
    @Column(nullable = false)
    private short depth;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "moderation_flagged", nullable = false)
    private boolean moderationFlagged;

    protected Comment() {
    }

    public Comment(Long id, Long storyId, Long parentId, String author,
                   String text, short depth, Instant postedAt, boolean moderationFlagged) {
        this.id = id;
        this.storyId = storyId;
        this.parentId = parentId;
        this.author = author;
        this.text = text;
        this.depth = depth;
        this.postedAt = postedAt;
        this.moderationFlagged = moderationFlagged;
    }

    public Long getId() { return id; }
    public Long getStoryId() { return storyId; }
    public Long getParentId() { return parentId; }
    public String getAuthor() { return author; }
    public String getText() { return text; }
    public short getDepth() { return depth; }
    public Instant getPostedAt() { return postedAt; }
    public boolean isModerationFlagged() { return moderationFlagged; }
}
