package hn.chatbot.search;

import java.util.ArrayList;
import java.util.List;

/**
 * 계획별 실행 조건 문자열을 만든다. 완성본이다.
 *
 * 조건은 사람과 모델이 함께 읽는다. 형식을 계획마다 제각각 만들지 않도록 여기 모았다.
 *
 *   keywords=[VMware, Broadcom], suitable, category=OFFICIAL_ANNOUNCEMENT
 *   query="coding agent limits", suitable, top 5
 */
public final class PlanConditions {

    private PlanConditions() {
    }

    /** 계획 1. */
    public static String keywords(List<String> keywords, String techField, String category) {
        return join("keywords=" + keywords, techField, category, null);
    }

    /** 계획 2. */
    public static String fullText(String query, String techField, String category) {
        return join(quoted(query), techField, category, null);
    }

    /** 계획 3 · 4. */
    public static String vector(String query, String techField, String category, int topK) {
        return join(quoted(query), techField, category, "top " + topK);
    }

    /** 비어 있는 필터 인자를 null 로 맞춘다. 모델은 "없음"을 빈 문자열로 채우기도 한다. */
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String quoted(String query) {
        return "query=\"" + (query == null ? "" : query) + "\"";
    }

    private static String join(String head, String techField, String category, String tail) {
        List<String> parts = new ArrayList<>();
        parts.add(head);
        parts.add("suitable");
        if (blankToNull(techField) != null) {
            parts.add("techField=" + techField.strip());
        }
        if (blankToNull(category) != null) {
            parts.add("category=" + category.strip());
        }
        if (tail != null) {
            parts.add(tail);
        }
        return String.join(", ", parts);
    }
}
