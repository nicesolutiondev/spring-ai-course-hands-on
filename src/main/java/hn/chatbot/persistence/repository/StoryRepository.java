package hn.chatbot.persistence.repository;

import hn.chatbot.domain.Story;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<Story, Long> {
}
