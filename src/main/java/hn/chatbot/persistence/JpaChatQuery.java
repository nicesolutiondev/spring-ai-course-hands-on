package hn.chatbot.persistence;

import hn.chatbot.domain.Analysis;
import hn.chatbot.domain.Story;
import hn.chatbot.persistence.repository.AnalysisRepository;
import hn.chatbot.persistence.repository.ArticleRepository;
import hn.chatbot.persistence.repository.CommentRepository;
import hn.chatbot.persistence.repository.StoryRepository;
import hn.chatbot.service.chat.CommentView;
import hn.chatbot.service.chat.StoryDetail;
import hn.chatbot.service.chat.port.ChatQuery;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** ChatQuery 포트의 영속 어댑터. */
@Component
public class JpaChatQuery implements ChatQuery {

    private final CommentRepository comments;
    private final StoryRepository stories;
    private final AnalysisRepository analyses;
    private final ArticleRepository articles;

    public JpaChatQuery(CommentRepository comments, StoryRepository stories, AnalysisRepository analyses,
                        ArticleRepository articles) {
        this.comments = comments;
        this.stories = stories;
        this.analyses = analyses;
        this.articles = articles;
    }

    @Override
    public Map<Long, String> bodyExcerpts(List<Long> storyIds) {
        if (storyIds.isEmpty()) {
            return Map.of();
        }
        return articles.findExcerpts(storyIds, EXCERPT_CHARS).stream()
                .collect(Collectors.toMap(ArticleRepository.Excerpt::getStoryId,
                        ArticleRepository.Excerpt::getExcerpt, (a, b) -> a, LinkedHashMap::new));
    }

    @Override
    public Optional<StoryDetail> storyDetail(long storyId) {
        return storyDetails(List.of(storyId)).stream().findFirst();
    }

    @Override
    public List<CommentView> comments(long storyId, int limit) {
        return comments.findByStoryIdOrderByPostedAtAsc(storyId).stream()
                .limit(limit)
                .map(c -> new CommentView(c.getId(), c.getAuthor(), c.getDepth(),
                        c.getText(), c.isModerationFlagged()))
                .toList();
    }

    /** 입력 순서를 따른다. 스토리나 분석 행이 없는 storyId 는 빠진다. */
    @Override
    public List<StoryDetail> storyDetails(List<Long> storyIds) {
        Map<Long, Story> storyById = stories.findAllById(storyIds).stream()
                .collect(Collectors.toMap(Story::getId, Function.identity()));
        Map<Long, Analysis> analysisById = analyses.findAllById(storyIds).stream()
                .collect(Collectors.toMap(Analysis::getStoryId, Function.identity()));

        return storyIds.stream()
                .filter(id -> storyById.containsKey(id) && analysisById.containsKey(id))
                .map(id -> toDetail(storyById.get(id), analysisById.get(id)))
                .toList();
    }

    private static StoryDetail toDetail(Story s, Analysis a) {
        return new StoryDetail(s.getId(), s.getTitle(), s.getUrl(),
                a.getSummary(), a.getCommunityReaction(), a.getPracticalImplication(),
                a.getTechField(), a.getCategory(), a.getKeywords() == null ? List.of() : List.of(a.getKeywords()));
    }
}
