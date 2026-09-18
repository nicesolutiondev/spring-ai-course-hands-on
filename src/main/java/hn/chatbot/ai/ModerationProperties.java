package hn.chatbot.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 댓글 검열 설정.
 *
 * isFlagged() 를 그대로 쓰지 않고 여기 적힌 카테고리의 점수만 임계값과 비교한다.
 * harassment 계열은 목록에 없다 — 욕설이 아니라 대상을 향한 공격성을
 * 잡는 분류라, 기술 커뮤니티의 제품·기업 비판이 대량으로 걸린다.
 *
 * 임계값 0.85 는 실측 근거가 있다. 실제 HN 댓글 30건의 최고점이 0.285 이고
 * 픽스처 30건 중 10건이 걸린다. 실제 데이터에서 0건이 걸리는 것이 정상이다.
 */
@ConfigurationProperties(prefix = "app.moderation")
public record ModerationProperties(List<String> categories, double threshold) {
}
