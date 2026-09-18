package hn.chatbot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 계층 규칙을 강제한다. 규칙을 문장으로만 남기면 다음 사람이 import 한 줄로 조용히 깬다.
 *
 * 규칙은 하나다 — 안쪽 계층은 바깥쪽 계층의 모델을 쓰지 않는다.
 * 필드 구성이 같아도 타입을 나누고, 경계에서 바깥이 안쪽 모델을 받아 옮긴다.
 *
 * domain/ 은 예외다. 최내층이라 어느 계층이 참조해도 방향이 어긋나지 않는다.
 */
class LayeringTest {

    private static final String BASE = "src/main/java/hn/chatbot/";

    @Test
    @DisplayName("ai/ 는 어떤 계층도 참조하지 않는 잎이다")
    void aiIsLeaf() {
        assertThat(referencesFrom("ai"))
                .as("ai/ 가 참조하는 hn.chatbot 패키지")
                .isEmpty();
    }

    @Test
    @DisplayName("search/ 는 자기 자신 외에 아무것도 참조하지 않는다")
    void searchIsSelfContained() {
        assertThat(referencesFrom("search"))
                .as("search/ 가 참조하는 다른 패키지")
                .isEmpty();
    }

    @Test
    @DisplayName("service/ 는 web 도 persistence 도 모른다")
    void serviceKnowsNeitherWebNorPersistence() {
        // service 가 domain · ai · search · adapter 를 부르는 것은 의도된 방향이다.
        // 오케스트레이션이 그 역할이다. 막아야 하는 것은 바깥쪽 두 계층뿐이다.
        assertThat(referencesFrom("service"))
                .as("service/ 가 참조하는 다른 패키지")
                .noneMatch(p -> p.startsWith("web") || p.startsWith("persistence"));
    }

    @Test
    @DisplayName("persistence/repository 는 persistence 밖으로 새지 않는다")
    void springDataStaysInsidePersistence() throws IOException {
        List<String> offenders = javaFiles(BASE)
                .filter(p -> !p.toString().contains("/persistence/"))
                .filter(p -> imports(p).stream()
                        .anyMatch(i -> i.contains("hn.chatbot.persistence.repository")))
                .map(Path::toString)
                .toList();

        assertThat(offenders)
                .as("persistence 밖에서 Spring Data 리포지토리를 import 한 파일")
                .isEmpty();
    }

    @Test
    @DisplayName("service/ 는 adapter 도 모른다 — 포트로만 바깥과 만난다")
    void serviceKnowsNoAdapter() {
        assertThat(referencesFrom("service"))
                .as("service/ 가 참조하는 다른 패키지")
                .noneMatch(p -> p.startsWith("adapter"));
    }

    @Test
    @DisplayName("adapter/ 는 포트를 구현한다 — 화살표가 안쪽을 향한다")
    void adapterImplementsPorts() {
        assertThat(referencesFrom("adapter"))
                .as("adapter/ 가 참조하는 다른 패키지")
                .isNotEmpty()
                .allMatch(p -> p.startsWith("service.") || p.startsWith("domain"),
                        "service 의 포트와 모델, domain 외에는 참조하지 않아야 한다");
    }

    @Test
    @DisplayName("domain/ 은 아무것도 참조하지 않는다")
    void domainIsInnermost() {
        assertThat(referencesFrom("domain")).isEmpty();
    }

    /** 주어진 패키지가 참조하는 다른 hn.chatbot 패키지들. 자기 하위 패키지는 뺀다. */
    private static List<String> referencesFrom(String pkg) {
        try (Stream<Path> files = javaFiles(BASE + pkg)) {
            return files.flatMap(p -> imports(p).stream())
                    .filter(i -> i.startsWith("hn.chatbot."))
                    .map(i -> i.substring("hn.chatbot.".length()))
                    .map(i -> i.replaceAll("\\.[A-Z][A-Za-z0-9]*$", ""))
                    .filter(i -> !i.equals(pkg) && !i.startsWith(pkg + "."))
                    .distinct()
                    .sorted()
                    .toList();
        }
        catch (IOException e) {
            throw new IllegalStateException(pkg, e);
        }
    }

    private static Stream<Path> javaFiles(String dir) throws IOException {
        return Files.walk(Path.of(dir)).filter(p -> p.toString().endsWith(".java"));
    }

    private static List<String> imports(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .map(String::strip)
                    .filter(l -> l.startsWith("import "))
                    .map(l -> l.substring("import ".length()).replace(";", "").replace("static ", ""))
                    .toList();
        }
        catch (IOException e) {
            throw new IllegalStateException(file.toString(), e);
        }
    }
}
