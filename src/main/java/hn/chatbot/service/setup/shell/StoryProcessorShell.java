package hn.chatbot.service.setup.shell;

import hn.chatbot.ai.ArticleBodyExtractor;
import hn.chatbot.ai.ArticleChunker;
import hn.chatbot.ai.CommentModerator;
import hn.chatbot.ai.EmbeddingIndexer;
import hn.chatbot.ai.HarmfulPhraseDetector;
import hn.chatbot.ai.IssueAnalyzer;
import hn.chatbot.service.setup.StageTracker;
import hn.chatbot.service.setup.StoryOutcome;
import hn.chatbot.service.setup.StoryProcessor;
import hn.chatbot.service.setup.port.ArticleSource;
import hn.chatbot.service.setup.port.SetupQuery;
import hn.chatbot.service.setup.port.SetupStore;
import hn.chatbot.service.setup.port.StorySource;
import org.springframework.stereotype.Component;

/**
 * 스토리 하나를 2~8단계로 처리한다. 단계와 규칙은 StoryProcessor 의 Javadoc 에 있다.
 *
 * 수강생이 채운다. 필요한 부품은 모두 주입돼 있다. 생성자 인자 순서가 곧 파이프라인 순서다.
 * 실행 · 진행 상황 · 로그는 완성본 PipelineSetupService 가 맡는다.
 */
@Component
public class StoryProcessorShell implements StoryProcessor {

    private final SetupQuery setupQuery;
    private final StorySource storySource;
    private final CommentModerator commentModerator;
    private final HarmfulPhraseDetector harmfulPhraseDetector;
    private final ArticleSource articleSource;
    private final ArticleBodyExtractor articleBodyExtractor;
    private final IssueAnalyzer issueAnalyzer;
    private final ArticleChunker articleChunker;
    private final EmbeddingIndexer embeddingIndexer;
    private final SetupStore store;

    public StoryProcessorShell(SetupQuery setupQuery, StorySource storySource,
                               CommentModerator commentModerator, HarmfulPhraseDetector harmfulPhraseDetector,
                               ArticleSource articleSource, ArticleBodyExtractor articleBodyExtractor,
                               IssueAnalyzer issueAnalyzer, ArticleChunker articleChunker,
                               EmbeddingIndexer embeddingIndexer, SetupStore store) {
        this.setupQuery = setupQuery;
        this.storySource = storySource;
        this.commentModerator = commentModerator;
        this.harmfulPhraseDetector = harmfulPhraseDetector;
        this.articleSource = articleSource;
        this.articleBodyExtractor = articleBodyExtractor;
        this.issueAnalyzer = issueAnalyzer;
        this.articleChunker = articleChunker;
        this.embeddingIndexer = embeddingIndexer;
        this.store = store;
    }

    @Override
    public StoryOutcome process(long storyId, StageTracker tracker) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다. 이 메서드를 채우세요.");
    }
}
