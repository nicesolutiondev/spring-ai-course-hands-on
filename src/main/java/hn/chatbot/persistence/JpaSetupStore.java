package hn.chatbot.persistence;

import hn.chatbot.domain.Analysis;
import hn.chatbot.domain.Article;
import hn.chatbot.domain.BodyChunk;
import hn.chatbot.domain.Comment;
import hn.chatbot.domain.Story;
import hn.chatbot.persistence.repository.AnalysisRepository;
import hn.chatbot.persistence.repository.ArticleRepository;
import hn.chatbot.persistence.repository.BodyChunkRepository;
import hn.chatbot.persistence.repository.CommentRepository;
import hn.chatbot.persistence.repository.StoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** SetupStore 포트의 영속 어댑터. */
@Component
public class JpaSetupStore implements hn.chatbot.service.setup.port.SetupStore {

    private final StoryRepository stories;
    private final CommentRepository comments;
    private final ArticleRepository articles;
    private final AnalysisRepository analyses;
    private final BodyChunkRepository chunks;

    public JpaSetupStore(StoryRepository stories, CommentRepository comments,
                         ArticleRepository articles, AnalysisRepository analyses,
                         BodyChunkRepository chunks) {
        this.stories = stories;
        this.comments = comments;
        this.articles = articles;
        this.analyses = analyses;
        this.chunks = chunks;
    }

    @Override
    public void saveStory(Story story) {
        stories.save(story);
    }

    @Override
    public void saveComments(List<Comment> list) {
        comments.saveAll(list);
    }

    @Override
    public void saveArticle(Article article) {
        articles.save(article);
    }

    @Override
    public void saveAnalysis(Analysis analysis) {
        analyses.save(analysis);
    }

    @Override
    public void saveChunks(List<BodyChunk> list) {
        chunks.saveAll(list);
    }
}
