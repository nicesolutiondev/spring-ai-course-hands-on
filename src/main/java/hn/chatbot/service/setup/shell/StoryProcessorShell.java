package hn.chatbot.service.setup.shell;

import hn.chatbot.ai.ArticleBodyExtractor;
import hn.chatbot.ai.ArticleChunker;
import hn.chatbot.ai.CommentModerator;
import hn.chatbot.ai.EmbeddingIndexer;
import hn.chatbot.ai.HarmfulPhraseDetector;
import hn.chatbot.ai.IssueAnalysis;
import hn.chatbot.ai.IssueAnalyzer;
import hn.chatbot.ai.MaskingResult;
import hn.chatbot.ai.ModerationVerdict;
import hn.chatbot.domain.Article;
import hn.chatbot.domain.ArticleStatus;
import hn.chatbot.domain.BodyChunk;
import hn.chatbot.domain.Comment;
import hn.chatbot.domain.Story;
import hn.chatbot.service.setup.AnalysisMapper;
import hn.chatbot.service.setup.PipelinePolicy;
import hn.chatbot.service.setup.StageTracker;
import hn.chatbot.service.setup.StoryOutcome;
import hn.chatbot.service.setup.StoryProcessor;
import hn.chatbot.service.setup.model.ArticleCheck;
import hn.chatbot.service.setup.model.CollectedComment;
import hn.chatbot.service.setup.model.CollectedStory;
import hn.chatbot.service.setup.model.LogKind;
import hn.chatbot.service.setup.model.LogState;
import hn.chatbot.service.setup.port.ArticleSource;
import hn.chatbot.service.setup.port.SetupQuery;
import hn.chatbot.service.setup.port.SetupStore;
import hn.chatbot.service.setup.port.StorySource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 스토리 하나를 2~8단계로 처리한다. 단계와 규칙은 StoryProcessor 의 Javadoc 에 있다.
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
        if (setupQuery.isProcessed(storyId)) {
            return StoryOutcome.SKIPPED;
        }
        tracker.passed(2);

        CollectedStory collected = collectAndSaveStory(storyId);

        List<Comment> comments = moderateAndSaveComments(collected);
        tracker.passed(3);

        ArticleCheck check = PipelinePolicy.check(articleSource.fetch(collected.url()));
        if (!check.passed()) {
            saveArticle(collected.id(), check.status(), check.text(), check.length(), null);
            tracker.log(LogKind.EXCLUDE, collected.id(), collected.title(), check.status().name(), LogState.WARN);
            return StoryOutcome.EXCLUDED;
        }
        tracker.passed(4);

        Optional<String> body = articleBodyExtractor.extract(collected.title(), check.text());
        if (body.isEmpty()) {
            saveArticle(collected.id(), ArticleStatus.NO_BODY, check.text(), check.length(), null);
            tracker.log(LogKind.EXCLUDE, collected.id(), collected.title(), "본문 없음", LogState.WARN);
            return StoryOutcome.EXCLUDED;
        }
        saveArticle(collected.id(), ArticleStatus.PASS, check.text(), check.length(), body.get());
        tracker.passed(5);
        tracker.log(LogKind.EXTRACT, collected.id(), collected.title(), body.get().length() + "자", LogState.OK);

        IssueAnalysis analysis = analyzeAndSave(collected, body.get(), comments);
        tracker.passed(6);
        tracker.log(LogKind.ANALYZE, collected.id(), collected.title(), analysis.category(), LogState.OK);
        if (!analysis.suitable()) {
            return StoryOutcome.EXCLUDED;
        }

        List<String> chunks = chunkAndSave(collected, body.get());
        tracker.passed(7);
        tracker.log(LogKind.CHUNK, collected.id(), collected.title(), chunks.size() + "개", LogState.OK);

        embeddingIndexer.indexSummary(collected.id(), analysis);
        embeddingIndexer.indexBody(collected.id(), analysis, chunks);
        tracker.passed(8);

        return StoryOutcome.COMPLETED;
    }

    /** 2단계 뒤, 3단계 앞 — 스토리를 가져와 저장한다. */
    private CollectedStory collectAndSaveStory(long storyId) {
        CollectedStory collected = storySource.collect(storyId)
                .orElseThrow(() -> new NoSuchElementException("스토리를 찾을 수 없습니다: " + storyId));

        store.saveStory(new Story(collected.id(), collected.title(), collected.url(), collected.author(),
                collected.score(), collected.descendants(), collected.text(), collected.postedAt()));
        return collected;
    }

    /** 3단계 — 댓글마다 검열하고 걸린 것만 마스킹해 저장한다. */
    private List<Comment> moderateAndSaveComments(CollectedStory collected) {
        List<Comment> comments = collected.comments().stream()
                .map(comment -> moderateComment(collected.id(), comment))
                .toList();
        store.saveComments(comments);
        return comments;
    }

    private Comment moderateComment(long storyId, CollectedComment comment) {
        ModerationVerdict verdict = commentModerator.inspect(comment.text());
        String text = comment.text();
        if (verdict.flagged()) {
            MaskingResult masking = harmfulPhraseDetector.detect(text);
            for (String phrase : masking.harmfulPhrases()) {
                text = text.replace(phrase, PipelinePolicy.MASK);
            }
        }
        return new Comment(comment.id(), storyId, comment.parentId(), comment.author(),
                text, comment.depth(), comment.postedAt(), verdict.flagged());
    }

    /** 6단계 — 최상위 댓글과 함께 분석하고 결과를 저장한다. */
    private IssueAnalysis analyzeAndSave(CollectedStory collected, String body, List<Comment> comments) {
        List<String> topComments = comments.stream()
                .filter(comment -> comment.getDepth() == 1)
                .limit(PipelinePolicy.TOP_COMMENTS)
                .map(Comment::getText)
                .toList();
        IssueAnalysis analysis = issueAnalyzer.analyze(collected.title(), body, topComments);
        store.saveAnalysis(AnalysisMapper.toEntity(collected.id(), analysis));
        return analysis;
    }

    /** 7단계 — 본문을 청킹해 순서대로 저장한다. */
    private List<String> chunkAndSave(CollectedStory collected, String body) {
        List<String> chunks = articleChunker.chunk(body);
        List<BodyChunk> bodyChunks = new ArrayList<>();
        for (int seq = 0; seq < chunks.size(); seq++) {
            bodyChunks.add(new BodyChunk(collected.id(), seq, chunks.get(seq)));
        }
        store.saveChunks(bodyChunks);
        return chunks;
    }

    private void saveArticle(long storyId, ArticleStatus status, String cleanedText, Integer cleanedLen, String body) {
        store.saveArticle(new Article(storyId, status, cleanedText, cleanedLen, body));
    }
}
