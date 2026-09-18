package hn.chatbot.web.dto;

/**
 * 분포 한 칸.
 *
 * label 은 여기에만 있다. 화면에 띄울 한글 표시 이름이고,
 * 컨트롤러가 TopicCount 를 옮기면서 붙인다. 서비스도 도구도 모델도 이 값을 모른다.
 * 매핑 표에 없는 값이 오면 컨트롤러가 폴백을 정한다 — 분류 체계는 폴백 규칙 때문에
 * 런타임에 새 값이 생길 수 있다.
 */
public record Facet(String value, String label, int count) {
}
