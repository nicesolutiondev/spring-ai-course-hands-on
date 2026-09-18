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
        throw new UnsupportedOperationException("아직 구현되지 않았습니다. 이 메서드를 채우세요.");
    }
}
