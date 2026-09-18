package hn.chatbot.service.topic.model;

/**
 * 분포 한 칸. 표시용 한글 이름(label)은 여기 없다.
 * 그건 화면 사정이라 web/dto 의 Facet 에만 있고, 컨트롤러가 붙인다.
 *
 * 이 구분이 중요한 이유는 이 값이 listTopics 도구를 통해 모델에게도 가기 때문이다.
 * 모델은 AI_LLM 이라는 코드로 도구를 부르지 "AI/LLM" 이라는 한글로 부르지 않는다.
 */
public record TopicCount(String value, int count) {
}
