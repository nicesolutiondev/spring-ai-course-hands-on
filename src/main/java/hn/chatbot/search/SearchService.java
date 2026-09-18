package hn.chatbot.search;

/**
 * 4개 계획을 수행하고 storyId 기준으로 중복을 제거한다(최대 20건).
 *
 * techField · category 는 모델이 searchIssues 의 인자로 채워 넘긴 값이고,
 * 비어 있으면 그 축으로 거르지 않는다.
 */
public interface SearchService {

    SearchResult search(String query, String techField, String category);
}
