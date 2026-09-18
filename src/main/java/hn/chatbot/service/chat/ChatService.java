package hn.chatbot.service.chat;

import hn.chatbot.service.chat.model.ChatEvent;
import reactor.core.publisher.Flux;

/**
 * 질문 하나를 받아 답변을 스트리밍한다. 답은 도구를 호출한 모델이 한 번에 생성한다.
 *
 * 흐름
 *
 * 질문
 *   ├─ 적재된 스토리가 0건이면(TopicService.count(null, null)) → 안내 문구를 Token 으로 흘리고 Completed. 여기서 끝.
 *   ↓
 * ChatTurn 을 새로 만든다
 *   ↓
 * ChatClient 에 붙여 stream() 으로 호출
 *   system     : AnswerGenerator.answerRules()
 *   tools      : issueTools
 *   toolContext: conversationId 와 ChatTurn (키: ChatMemory.CONVERSATION_ID, ChatTurn.KEY)
 *   advisors   : 주입된 ChatMemory 로 만든 MessageChatMemoryAdvisor 와 conversationId 파라미터
 *   ↓
 * 모델이 필요하면 searchIssues 를 호출한다. 검색 · 적합성 판단 · 사건 방출 · 기억 기록은 도구가 한다
 *   ↓
 * turn.stream(모델의 token 흐름) 을 돌려준다. plans · evidence · token · done 이 순서대로 합쳐진다
 *
 * 사건의 순서
 *
 * plans → evidence → token 반복 → done. 화면이 이 순서대로 점진적으로 그린다.
 * 검색이 없는 질문(건수 · 댓글 · 분포 · 분야 요약)은 token 과 done 만 흐른다.
 *
 * 대화 맥락
 *
 * conversationId 는 필수다. ChatMemory.CONVERSATION_ID 에 기본값이 없고,
 * DB 컬럼이 VARCHAR(36) 이라 UUID 여야 한다. 프론트가 세션 시작 시 만들어 보낸다.
 *
 * 같은 conversationId 를 advisors 파라미터와 toolContext 에 각각 넣는다. 하나는 기억 Advisor 가,
 * 다른 하나는 도구 본문이 읽는다. 서로 다른 통로다.
 *
 * SseEmitter 를 반환하지 않는다
 *
 * 전송 방식은 웹 계층의 관심사다. 여기서는 사건만 흘리고 컨트롤러가 구독해 SSE 로 옮긴다.
 */
public interface ChatService {

    Flux<ChatEvent> chat(String conversationId, String question);
}
