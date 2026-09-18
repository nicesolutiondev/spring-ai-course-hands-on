package hn.chatbot.persistence.repository;

import hn.chatbot.domain.BodyChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BodyChunkRepository extends JpaRepository<BodyChunk, Long> {

    List<BodyChunk> findByStoryIdOrderBySeqAsc(long storyId);

    long countByStoryId(long storyId);
}
