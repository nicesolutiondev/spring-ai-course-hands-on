package hn.chatbot.service.setup.shell;

import hn.chatbot.ai.*;
import hn.chatbot.ai.ArticleChunker;
import hn.chatbot.ai.CommentModerator;
import hn.chatbot.ai.EmbeddingIndexer;
import hn.chatbot.ai.HarmfulPhraseDetector;
import hn.chatbot.ai.IssueAnalyzer;
import hn.chatbot.service.setup.StageTracker;
import hn.chatbot.service.setup.PipelinePolicy;
import hn.chatbot.service.setup.AnalysisMapper;
import hn.chatbot.service.setup.model.*;
import hn.chatbot.domain.*;
import java.util.*;
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
        if (setupQuery.isProcessed(storyId)) return StoryOutcome.SKIPPED;
        Optional<CollectedStory> collected = storySource.collect(storyId);
        if (collected.isEmpty()) return StoryOutcome.EXCLUDED;
        CollectedStory story = collected.get();
        store.saveStory(new Story(story.id(), story.title(), story.url(), story.author(), story.score(), story.descendants(), story.text(), story.postedAt()));
        List<Comment> comments = new ArrayList<>();
        List<String> topComments = new ArrayList<>();
        for (CollectedComment comment : story.comments()) {
            String text = comment.text() == null ? "" : comment.text();
            ModerationVerdict verdict = commentModerator.inspect(text);
            String clean = text;
            if (verdict.flagged()) {
                MaskingResult masking = harmfulPhraseDetector.detect(text);
                if (masking != null && masking.harmfulPhrases() != null) for (String phrase : masking.harmfulPhrases()) if (phrase != null && !phrase.isEmpty()) clean = clean.replace(phrase, PipelinePolicy.MASK);
            }
            comments.add(new Comment(comment.id(), storyId, comment.parentId(), comment.author(), clean, comment.depth(), comment.postedAt(), verdict.flagged()));
            if (comment.depth() == 1 && topComments.size() < PipelinePolicy.TOP_COMMENTS) topComments.add(clean);
        }
        store.saveComments(comments); tracker.passed(3);
        FetchedArticle fetched = articleSource.fetch(story.url()); ArticleCheck check = PipelinePolicy.check(fetched);
        if (!check.passed()) { store.saveArticle(new Article(storyId, check.status(), check.text(), check.length(), null)); return StoryOutcome.EXCLUDED; }
        Optional<String> extracted = articleBodyExtractor.extract(story.title(), check.text());
        if (extracted.isEmpty()) { store.saveArticle(new Article(storyId, ArticleStatus.NO_BODY, check.text(), check.length(), null)); return StoryOutcome.EXCLUDED; }
        String body = extracted.get(); store.saveArticle(new Article(storyId, ArticleStatus.PASS, check.text(), check.length(), body)); tracker.passed(4); tracker.passed(5);
        IssueAnalysis analysis = issueAnalyzer.analyze(story.title(), body, topComments); store.saveAnalysis(AnalysisMapper.toEntity(storyId, analysis)); tracker.passed(6);
        if (!analysis.suitable()) return StoryOutcome.EXCLUDED;
        List<String> chunks = articleChunker.chunk(body); List<BodyChunk> entities = new ArrayList<>(); for (int i = 0; i < chunks.size(); i++) entities.add(new BodyChunk(storyId, i, chunks.get(i)));
        store.saveChunks(entities); tracker.passed(7); embeddingIndexer.indexSummary(storyId, analysis); embeddingIndexer.indexBody(storyId, analysis, chunks); tracker.passed(8); return StoryOutcome.COMPLETED;
    }
}
