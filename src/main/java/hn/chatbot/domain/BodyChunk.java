package hn.chatbot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 본문 청크. seq 는 본문 내 순서로, 검색 결과에서 앞뒤 청크를 이어 붙일 때 쓴다. */
@Entity
@Table(name = "body_chunk")
public class BodyChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Column(nullable = false)
    private int seq;

    @Column(nullable = false)
    private String content;

    protected BodyChunk() {
    }

    public BodyChunk(Long storyId, int seq, String content) {
        this.storyId = storyId;
        this.seq = seq;
        this.content = content;
    }

    public Long getId() { return id; }
    public Long getStoryId() { return storyId; }
    public int getSeq() { return seq; }
    public String getContent() { return content; }
}
