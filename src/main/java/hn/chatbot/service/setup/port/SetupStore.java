package hn.chatbot.service.setup.port;

import hn.chatbot.domain.Analysis;
import hn.chatbot.domain.Article;
import hn.chatbot.domain.BodyChunk;
import hn.chatbot.domain.Comment;
import hn.chatbot.domain.Story;

import java.util.List;

/**
 * 파이프라인이 수집·처리한 것을 저장하는 포트. 서비스가 소유하고 persistence/ 가 구현한다.
 *
 * 서비스는 JPA 도 Spring Data 도 모른다. 도메인 엔티티만 주고받는다 —
 * domain/ 은 최내층이라 어느 계층이 참조해도 방향이 어긋나지 않는다.
 */
public interface SetupStore {

    void saveStory(Story story);

    void saveComments(List<Comment> comments);

    void saveArticle(Article article);

    void saveAnalysis(Analysis analysis);

    void saveChunks(List<BodyChunk> chunks);
}
