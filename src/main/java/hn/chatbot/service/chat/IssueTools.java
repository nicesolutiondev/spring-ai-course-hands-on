package hn.chatbot.service.chat;

import hn.chatbot.ai.AnalysisTarget;
import hn.chatbot.ai.RelevanceJudge;
import hn.chatbot.ai.RelevantStory;
import hn.chatbot.search.Candidate;
import hn.chatbot.search.SearchResult;
import hn.chatbot.search.SearchService;
import hn.chatbot.service.chat.port.ChatQuery;
import hn.chatbot.service.topic.TopicService;
import hn.chatbot.service.topic.model.StorySummary;
import hn.chatbot.service.topic.model.TopicDistribution;
import hn.chatbot.service.topic.model.TopicStories;
import hn.chatbot.service.topic.port.TopicQuery.StorySort;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 모델이 호출하는 도구 7종.
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

    /** 모델이 techField 에 넣을 수 있는 값. 폴백으로 새 값이 생기므로 listTopics 안내를 함께 둔다. */
    static final String TECH_FIELDS = "허용값: AI_LLM, SECURITY_PRIVACY, OPEN_SOURCE, "
            + "INFRASTRUCTURE_ENTERPRISE, PLATFORM_POLICY, DEV_CULTURE_PRACTICE, HARDWARE, "
            + "MOBILITY, NON_TECHNICAL. 이 목록에 없는 분야를 찾을 때는 listTopics 로 실제 값을 먼저 확인한다";

    /** 모델이 category 에 넣을 수 있는 값. */
    static final String CATEGORIES = "허용값: OFFICIAL_ANNOUNCEMENT, RELEASE_NOTES, NEWS_REPORT, "
            + "OPINION_ESSAY, TECHNICAL_DEEP_DIVE, RESEARCH_PAPER, SHOW_HN_PROJECT, ASK_TELL_HN, "
            + "PRODUCT_MARKETING. 이 목록에 없는 타입을 찾을 때는 listTopics 로 실제 값을 먼저 확인한다";

    private static final int EVIDENCE_MAX = 5;
    private static final int LIST_DEFAULT_LIMIT = 20;
    private static final int LIST_MAX_LIMIT = 50;
    private static final int COMMENTS_DEFAULT_LIMIT = 5;
    private static final int COMMENTS_MAX_LIMIT = 10;

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
    @Tool(description = "질의와 의미가 관련된 기술 이슈를 찾아 근거를 돌려준다. "
            + "'코딩 에이전트 한계가 뭐야' 처럼 특정 내용을 묻는 질문에 쓴다. "
            + "개수를 세거나 목록을 나열할 때는 쓰지 않는다(그때는 countStories · listStories 를 쓴다).")
    public SearchEvidence searchIssues(
            @ToolParam(description = "검색할 질문. 사용자의 질문을 그대로 또는 검색에 알맞게 다듬어 넣는다") String query,
            @ToolParam(required = false, description = "찾을 기술 분야. 비우면 모든 분야에서 찾는다. " + TECH_FIELDS) String techField,
            @ToolParam(required = false, description = "찾을 원문 타입/카테고리. 예: 공식 발표만 찾을 때. 비우면 모든 카테고리에서 찾는다. " + CATEGORIES) String category,
            ToolContext ctx) {
        SearchResult result = searchService.search(query, techField, category);

        List<Long> storyIds = result.candidates().stream().map(Candidate::storyId).toList();
        Map<Long, String> excerpts = storyIds.isEmpty() ? Map.of() : chatQuery.bodyExcerpts(storyIds);

        List<RelevantStory> judged;
        // Javadoc 의 분기는 후보 기준이다. 후보가 있으면 원문이 비어도 판단을 거친다.
        if (result.candidates().isEmpty()) {
            judged = List.of();
        } else {
            Map<Long, Candidate> candidateById = result.candidates().stream()
                    .collect(Collectors.toMap(Candidate::storyId, Function.identity()));
            List<AnalysisTarget> targets = excerpts.entrySet().stream()
                    .map(e -> {
                        Candidate candidate = candidateById.get(e.getKey());
                        return AnalysisTarget.forJudge(candidate.storyId(), candidate.title(),
                                candidate.summary(), e.getValue());
                    })
                    .toList();
            judged = relevanceJudge.selectRelevant(query, targets, EVIDENCE_MAX);
        }

        List<StoryDetail> details = chatQuery.storyDetails(judged.stream().map(RelevantStory::storyId).toList());
        SearchEvidence evidence = SearchEvidence.assemble(result, judged, details, excerpts);

        ChatTurn.from(ctx).publish(evidence);

        String conversationId = (String) ctx.getContext().get(ChatMemory.CONVERSATION_ID);
        chatMemory.add(conversationId, new AssistantMessage(SearchRecord.of(query, techField, category, evidence)));

        return evidence;
    }

    /**
     * 기술 분야나 원문 타입별 이슈 건수를 센다. 벡터 검색으로는 할 수 없는 일이다.
     *
     * techField 와 category 의 허용값은 클래스 Javadoc 에 있고 설명 문구에 적는다.
     *
     * 두 인자 모두 선택이다. TopicService.count 에 그대로 넘기면 비운 축은 거르지 않는다.
     */
    @Tool(description = "적재된 기술 이슈의 건수를 센다. '이슈가 몇 건이야' 처럼 집계를 물을 때 쓴다. "
            + "개별 이슈를 찾을 때는 쓰지 않는다.")
    public int countStories(
            @ToolParam(required = false, description = "건수를 셀 기술 분야. 비우면 모든 분야를 센다. " + TECH_FIELDS) String techField,
            @ToolParam(required = false, description = "건수를 셀 원문 타입/카테고리. 비우면 모든 카테고리를 센다. " + CATEGORIES) String category) {
        return topicService.count(techField, category);
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
    @Tool(description = "특정 기술 분야의 이슈 목록을 점수순 또는 최신순으로 나열한다. "
            + "'AI 쪽 이슈 점수순으로 보여줘' 처럼 목록을 물을 때 쓴다.")
    public List<StorySummary> listStories(
            @ToolParam(required = false, description = "나열할 기술 분야. 비우면 모든 분야를 나열한다. " + TECH_FIELDS) String techField,
            @ToolParam(required = false, description = "정렬 기준. SCORE(점수순) 또는 RECENT(최신순). "
                    + "비우거나 알 수 없는 값이면 SCORE 를 쓴다") String sortBy,
            @ToolParam(required = false, description = "최대 개수. 비우면 기본값을 쓰고, 상한을 넘으면 상한으로 자른다") Integer limit,
            ToolContext ctx) {
        StorySort sort = parseSort(sortBy);
        int effectiveLimit = resolveLimit(limit, LIST_DEFAULT_LIMIT, LIST_MAX_LIMIT);

        TopicStories result = topicService.stories(blankToNull(techField), sort, effectiveLimit);
        List<StorySummary> stories = result.stories();

        String conversationId = (String) ctx.getContext().get(ChatMemory.CONVERSATION_ID);
        chatMemory.add(conversationId, new AssistantMessage(SearchRecord.ofStories(techField, sort.name(), stories)));

        return stories;
    }

    /**
     * 스토리 하나의 요약 · 커뮤니티 반응 · 실무 시사점을 가져온다.
     *
     * storyId 를 이미 받아 호출되므로 대화 기억에 따로 남기지 않아도 된다.
     */
    @Tool(description = "특정 스토리 하나의 요약 · 커뮤니티 반응 · 실무 시사점을 가져온다. storyId 를 이미 알고 있을 때 쓴다.")
    public StoryDetail getStoryDetail(@ToolParam(description = "조회할 스토리 id") long storyId) {
        return chatQuery.storyDetail(storyId)
                .orElseThrow(() -> new IllegalArgumentException("story not found: " + storyId));
    }

    /**
     * 스토리의 실제 댓글을 가져온다. "방금 그 이슈 댓글 보여줘" 같은 후속 질문이 여기로 온다.
     *
     * 돌려주는 text 는 검열을 거쳐 마스킹된 값이다.
     */
    @Tool(description = "특정 스토리의 실제 댓글을 가져온다. '방금 그 이슈 댓글 보여줘' 처럼 댓글 원문을 물을 때 쓴다.")
    public List<CommentView> getComments(
            @ToolParam(description = "댓글을 가져올 스토리 id") long storyId,
            @ToolParam(required = false, description = "최대 개수. 비우면 기본값을 쓰고, 상한을 넘으면 상한으로 자른다") Integer limit) {
        int effectiveLimit = resolveLimit(limit, COMMENTS_DEFAULT_LIMIT, COMMENTS_MAX_LIMIT);
        return chatQuery.comments(storyId, effectiveLimit);
    }

    /**
     * 수집된 이슈가 어떤 기술 분야와 원문 타입으로 나뉘는지 분포를 가져온다.
     *
     * TopicService 를 그대로 호출한다. 주제 탐색 탭과 모델이 같은 데이터를 본다.
     */
    @Tool(description = "수집된 이슈가 어떤 기술 분야와 원문 타입으로 나뉘는지 분포를 가져온다. "
            + "'어떤 기술 분야들이 있어' 처럼 전체 구성을 물을 때 쓴다.")
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
    @Tool(description = "특정 기술 분야 전반의 논의 흐름을 요약한다. 'AI 쪽 논의 흐름 정리해줘' 처럼 분야 전체를 물을 때 쓴다. "
            + "개별 이슈를 찾을 때는 쓰지 않는다(그때는 searchIssues 를 쓴다).")
    public String summarizeTopic(@ToolParam(description = "요약할 기술 분야. " + TECH_FIELDS) String techField) {
        return topicService.summarize(blankToNull(techField));
    }

    private static StorySort parseSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return StorySort.SCORE;
        }
        try {
            return StorySort.valueOf(sortBy.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return StorySort.SCORE;
        }
    }

    private static int resolveLimit(Integer limit, int defaultValue, int max) {
        // 비거나 말이 안 되는 값은 기본값으로 되돌린다. 큰 값만 상한으로 자른다.
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, max);
    }

    /** 모델은 없는 값을 빈 문자열로 채우기도 한다. 그대로 넘기면 「이름이 빈 분야」를 찾게 된다. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
