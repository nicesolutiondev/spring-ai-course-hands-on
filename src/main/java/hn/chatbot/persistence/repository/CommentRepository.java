package hn.chatbot.persistence.repository;

import hn.chatbot.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByStoryIdOrderByPostedAtAsc(long storyId);

    /** 최상위 댓글만. depth 를 저장해두어 재귀가 필요 없다. */
    List<Comment> findByStoryIdAndDepthOrderByPostedAtAsc(long storyId, short depth);
}
