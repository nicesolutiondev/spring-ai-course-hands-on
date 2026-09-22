package hn.chatbot.service.chat;

import hn.chatbot.ai.RelevanceJudge;
import hn.chatbot.search.SearchService;
import hn.chatbot.service.chat.port.ChatQuery;
import hn.chatbot.service.topic.TopicService;
import hn.chatbot.service.topic.model.StorySummary;
import hn.chatbot.service.topic.model.TopicDistribution;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import hn.chatbot.search.SearchResult;

/**
 * 모델이 호출하는 도구 7종. 수강생이 채운다.
 *
 * 도구 등록(@Tool · @ToolParam)과 인자 검증이 구현 대상이다. 설명 문구가 곧 모델의
 * 선택 기준이므로 언제 쓰고 언제 쓰지 않는지를 적는다.
 *
 * 모델이 채우는 인자는 비어 있거나 허용 범위를 벗어날 수 있다. 빈 문자열은 없는 값으로
 * 다루고, limit 은 비면 기본값, 크면 상한으로 자른다.
 *
 * techField 와 category 는 ENUM 이 아니라 TEXT 다. 대소문자까지 정확히 일치해야 걸리는데
 * 모델은 허용값을 모른다. 목록을 @ToolParam 설명에 적지 않으면 「하드웨어」나 hardware 를
 * 넣고, 예외 없이 0건이 돌아와 그 결과를 근거로 그럴듯한 오답이 만들어진다.
 * 두 분류 모두 폴백으로 런타임에 새 값이 생기므로 「목록에 없으면 listTopics 로 확인」도 적는다.
 *
 * techField 값: AI_LLM · SECURITY_PRIVACY · OPEN_SOURCE · INFRASTRUCTURE_ENTERPRISE ·
 * PLATFORM_POLICY · DEV_CULTURE_PRACTICE · HARDWARE · MOBILITY · NON_TECHNICAL
 *
 * category 값: OFFICIAL_ANNOUNCEMENT · RELEASE_NOTES · NEWS_REPORT · OPINION_ESSAY ·
 * TECHNICAL_DEEP_DIVE · RESEARCH_PAPER · SHOW_HN_PROJECT · ASK_TELL_HN · PRODUCT_MARKETING
 *
 * 반환 타입에 web/dto 를 쓰지 않는다. 도구 반환값은 JSON 으로 직렬화돼
 * 모델에게 들어간다. 화면용 한글 표시 이름이 섞이면 토큰만 쓴다.
 *
 * ToolContext 를 받는 도구는 모든 호출이 .toolContext(...) 를 채워야 한다.
 * 빠뜨리면 모델을 호출하기도 전에 IllegalArgumentException 이 난다.
 * 도구 호출의 중간 메시지는 대화 기억에 남지 않으므로, 목록을 돌려주는 도구는
 * 결과를 직접 기록한다. 문구는 SearchRecord 가 만든다.
 */
@Component
public class IssueTools {

    private final SearchService searchService;
    private final RelevanceJudge relevanceJudge;
    private final TopicService topicService;
    private final ChatQuery chatQuery;
    private final ChatMemory chatMemory;

    public IssueTools(SearchService searchService, RelevanceJudge relevanceJudge, TopicService topicService,
                      ChatQuery chatQuery, ChatMemory chatMemory) {
        this.searchService = searchService;
        this.relevanceJudge = relevanceJudge;
        this.topicService = topicService;
        this.chatQuery = chatQuery;
        this.chatMemory = chatMemory;
    }

    /**
     * 질의와 의미가 관련된 기술 이슈를 찾아 근거를 돌려준다. 개수를 세거나 목록을 나열할 때는 쓰지 않는다.
     *
     * techField · category 는 선택이다. 모델이 질의를 보고 채운다("공식 발표만" → category).
     * 비우면 그 축으로 거르지 않는다. 허용값은 클래스 Javadoc 에 있고 설명 문구에 적는다.
     *
     * 흐름
     *
     * 1 SearchService.search 로 후보를 찾는다
     * 2 ChatQuery.bodyExcerpts 로 후보의 원문 앞부분을 가져와 RelevanceJudge 로 근거를 고른다.
 *   판단에 넘기는 질문은 query 인자다. 후보가 0건이면 판단을 건너뛰고 빈 목록으로 3 을 진행한다
     * 3 ChatQuery.storyDetails 로 근거 내용을 채우고 SearchEvidence.assemble 로 합친다
     * 4 ChatTurn.from(ctx).publish 로 화면에 사건을 보낸다
     * 5 ctx 의 conversationId 로 SearchRecord.of 문구를 AssistantMessage 로 기억에 기록한다
     * 6 SearchEvidence 를 돌려준다. 모델이 이것으로 답을 쓴다
     */
    @Tool(description = "Search technical stories relevant to a question and return grounded evidence.")
    public SearchEvidence searchIssues(@ToolParam(description = "User question") String query,
                                       @ToolParam(description = "Exact tech field or blank") String techField,
                                       @ToolParam(description = "Exact category or blank") String category,
                                       ToolContext ctx) {
        SearchResult result = searchService.search(query, blank(techField), blank(category));
        List<Long> ids = result.candidates().stream().map(c -> c.storyId()).toList();
        Map<Long, String> excerpts = chatQuery.bodyExcerpts(ids);
        List<hn.chatbot.ai.AnalysisTarget> targets = result.candidates().stream()
                .map(c -> hn.chatbot.ai.AnalysisTarget.forJudge(c.storyId(), c.title(), c.summary(), excerpts.get(c.storyId())))
                .toList();
        List<hn.chatbot.ai.RelevantStory> judged = relevanceJudge.selectRelevant(query, targets, 5);
        SearchEvidence evidence = SearchEvidence.assemble(result, judged,
                chatQuery.storyDetails(judged.stream().map(x -> x.storyId()).toList()), excerpts);
        ChatTurn.from(ctx).publish(evidence);
        String conversationId = String.valueOf(ctx.getContext().get(ChatMemory.CONVERSATION_ID));
        chatMemory.add(conversationId, new org.springframework.ai.chat.messages.AssistantMessage(
                SearchRecord.of(query, techField, category, evidence)));
        return evidence;
    }

    /**
     * 기술 분야나 원문 타입별 이슈 건수를 센다. 벡터 검색으로는 할 수 없는 일이다.
     *
     * techField 와 category 의 허용값은 클래스 Javadoc 에 있고 설명 문구에 적는다.
     *
     * 두 인자 모두 선택이다. TopicService.count 에 그대로 넘기면 비운 축은 거르지 않는다.
     */
    @Tool(description = "Count suitable stories, optionally filtered by exact techField and category.")
    public int countStories(@ToolParam(description = "Exact tech field or blank") String techField,
                            @ToolParam(description = "Exact category or blank") String category) {
        validateFilter(techField, "techField");
        validateFilter(category, "category");
        return topicService.count(blank(techField), blank(category));
    }

    /**
     * 특정 기술 분야의 이슈를 나열한다.
     *
     * techField 는 선택이다. 허용값은 클래스 Javadoc 에 있고 설명 문구에 적는다.
     * 비면 모든 분야를 나열한다. 빈 문자열을 그대로 넘기면 「이름이 빈 분야」를 찾게 되므로
     * 없는 값으로 바꿔 넘긴다.
     *
     * sortBy 는 SCORE(점수순) 또는 RECENT(최신순)다. 그 밖의 값이 오면 기본값으로 다룬다.
     *
     * 결과를 SearchRecord.ofStories 문구로 대화 기억에 기록한다.
     */
    @Tool(description = "List suitable stories ordered by score or recency.")
    public List<StorySummary> listStories(@ToolParam(description = "Exact tech field or blank") String techField,
                                          @ToolParam(description = "SCORE or RECENT") String sortBy,
                                          @ToolParam(description = "Number of stories, 1 to 50") Integer limit,
                                          ToolContext ctx) {
        validateFilter(techField, "techField");
        if (limit != null && (limit < 1 || limit > 50)) throw new IllegalArgumentException("limit must be between 1 and 50");
        int size = limit == null ? 10 : Math.max(1, Math.min(50, limit));
        var sort = "RECENT".equalsIgnoreCase(sortBy)
                ? hn.chatbot.service.topic.port.TopicQuery.StorySort.RECENT
                : hn.chatbot.service.topic.port.TopicQuery.StorySort.SCORE;
        hn.chatbot.service.topic.model.TopicStories page = topicService.stories(blank(techField), sort, size);
        List<StorySummary> stories = page.stories();
        String id = String.valueOf(ctx.getContext().get(ChatMemory.CONVERSATION_ID));
        chatMemory.add(id, new org.springframework.ai.chat.messages.AssistantMessage(
                SearchRecord.ofStories(techField, sort.name(), stories)));
        return stories;
    }

    /**
     * 스토리 하나의 요약 · 커뮤니티 반응 · 실무 시사점을 가져온다.
     *
     * storyId 를 이미 받아 호출되므로 대화 기억에 따로 남기지 않아도 된다.
     */
    @Tool(description = "Get the analysis details of one story by story ID.")
    public StoryDetail getStoryDetail(@ToolParam(description = "Positive story ID") long storyId) {
        if (storyId <= 0) throw new IllegalArgumentException("storyId must be positive");
        return chatQuery.storyDetail(storyId).orElse(null);
    }

    /**
     * 스토리의 실제 댓글을 가져온다. "방금 그 이슈 댓글 보여줘" 같은 후속 질문이 여기로 온다.
     *
     * 돌려주는 text 는 검열을 거쳐 마스킹된 값이다.
     */
    @Tool(description = "Get comments for one story, with moderation masking applied.")
    public List<CommentView> getComments(@ToolParam(description = "Positive story ID") long storyId,
                                         @ToolParam(description = "Number of comments, 1 to 100") Integer limit) {
        if (storyId <= 0) throw new IllegalArgumentException("storyId must be positive");
        if (limit != null && (limit < 1 || limit > 100)) throw new IllegalArgumentException("limit must be between 1 and 100");
        return chatQuery.comments(storyId, limit == null ? 20 : Math.max(1, Math.min(100, limit)));
    }

    /**
     * 수집된 이슈가 어떤 기술 분야와 원문 타입으로 나뉘는지 분포를 가져온다.
     *
     * TopicService 를 그대로 호출한다. 주제 탐색 탭과 모델이 같은 데이터를 본다.
     */
    @Tool(description = "List available technology fields and categories with story counts.")
    public TopicDistribution listTopics() {
        return topicService.distribution();
    }

    /**
     * 특정 기술 분야 전반의 논의 흐름을 요약한다. 개별 이슈를 찾을 때는 쓰지 않는다.
     *
     * techField 의 허용값은 클래스 Javadoc 에 있고 설명 문구에 적는다. 빈 문자열은
     * 없는 값으로 바꿔 넘긴다.
     *
     * TopicService.summarize 에 위임한다. 그 안에서 TopicSummarizer 가 LLM 을 호출한다.
     */
    @Tool(description = "Summarize common trends across stories in one technical field.")
    public String summarizeTopic(@ToolParam(description = "Exact tech field") String techField) {
        return topicService.summarize(blank(techField));
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static void validateFilter(String value, String name) {
        if (value != null && value.length() > 80) throw new IllegalArgumentException(name + " is too long");
    }
}
