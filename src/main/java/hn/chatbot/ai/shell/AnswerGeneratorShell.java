package hn.chatbot.ai.shell;


import hn.chatbot.ai.AnswerGenerator;
import org.springframework.stereotype.Component;

/**
 * Q&A 답변 규칙을 돌려준다. 담아야 할 규칙은 AnswerGenerator 의 Javadoc 에 있다.
 *
 * 수강생이 채운다. 모델을 호출하지 않고 규칙 문자열만 돌려준다.
 */
@Component
public class AnswerGeneratorShell implements AnswerGenerator {

    @Override
    public String answerRules() {
        return """
                Answer the user using only the evidence supplied in the conversation.
                If the evidence does not support a claim, say that it is unknown; do not invent facts,
                citations, story details, or numbers. Respect the search conditions and explain when they
                yielded no evidence. Distinguish reported facts from opinions and community reactions.
                Cite evidence inline using the evidence item rank, such as [1] or [2]. Every factual claim
                must have a supporting citation. Answer in the same language as the user and be concise.
                """;
    }
}
