package hn.chatbot.web;

import java.util.Map;

/**
 * 분류 코드의 표시 이름. 표현 계층에만 있다.
 *
 * 서비스도 도구도 모델도 이 표를 모른다. 모델은 AI_LLM 이라는 코드로 도구를 부르지
 * "AI/LLM" 이라는 표시 이름으로 부르지 않는다.
 *
 * 두 분류 모두 폴백 규칙이 있어 런타임에 새 값이 생긴다
 * (실제로 QUANTUM_COMPUTING 이 그렇게 만들어졌다). 표에 없는 코드는
 * 언더스코어를 공백으로 바꿔 그대로 보여준다 — 화면이 비거나 깨지지 않는다.
 */
final class TopicLabels {

    private static final Map<String, String> LABELS = Map.ofEntries(
            // techField
            Map.entry("AI_LLM", "AI/LLM"),
            Map.entry("SECURITY_PRIVACY", "보안·프라이버시"),
            Map.entry("OPEN_SOURCE", "오픈소스 프로젝트"),
            Map.entry("INFRASTRUCTURE_ENTERPRISE", "인프라·엔터프라이즈"),
            Map.entry("PLATFORM_POLICY", "플랫폼 정책·규제"),
            Map.entry("DEV_CULTURE_PRACTICE", "개발 문화·실무"),
            Map.entry("HARDWARE", "하드웨어"),
            Map.entry("MOBILITY", "모빌리티"),
            Map.entry("NON_TECHNICAL", "비기술 주제"),
            // category
            Map.entry("OFFICIAL_ANNOUNCEMENT", "공식 발표"),
            Map.entry("RELEASE_NOTES", "릴리스 노트"),
            Map.entry("NEWS_REPORT", "언론 보도"),
            Map.entry("OPINION_ESSAY", "의견·에세이"),
            Map.entry("TECHNICAL_DEEP_DIVE", "기술 분석"),
            Map.entry("RESEARCH_PAPER", "논문"),
            Map.entry("SHOW_HN_PROJECT", "프로젝트 공개"),
            Map.entry("ASK_TELL_HN", "커뮤니티 질문·제보"),
            Map.entry("PRODUCT_MARKETING", "제품 홍보"));

    private TopicLabels() {
    }

    static String of(String code) {
        if (code == null) {
            return "";
        }
        return LABELS.getOrDefault(code, code.replace('_', ' '));
    }
}
