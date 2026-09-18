# HN Chatbot: Spring AI 실습

Hacker News 의 기술 이슈와 댓글을 수집해 AI 가 분석·구조화하고, 그 결과로
의미 기반 검색과 근거 기반 Q&A 를 하는 애플리케이션입니다.

**애플리케이션은 이미 동작합니다.** 화면의 세 탭이 동작하고 테스트도 대부분 통과합니다. 다만
**AI 호출 코드가 비어 있습니다.** 이 빈 구현을 채우는 것이 실습입니다.

## 문서

| 문서 | 참조 시점과 내용 |
| --- | --- |
| [01. 시작하기](docs/01-시작하기.md) | **처음 한 번.** 환경 구성, 빈 구현 상태의 동작 확인 |
| [02. 전체 구조](docs/02-전체-구조.md) | **구현 전에.** 데이터 흐름과 계층 구조. 어느 빈 구현을 맡든 먼저 필요한 내용 |
| [03. 구현](docs/03-구현.md) | **구현 중 수시로.** 빈 구현마다 입력 · 출력 · 확인 방법 |
| [04. 코드 리뷰](docs/04-코드-리뷰.md) | **구현 직후.** 코딩 도구에 줄 것과 생성된 코드의 리뷰 기준 |

각 클래스의 Javadoc 에 더 자세한 명세가 있습니다.

## 기동

```bash
docker compose up -d     # PostgreSQL + pgAdmin
./gradlew bootRun        # 애플리케이션
```

- 애플리케이션 → http://localhost:8080
- pgAdmin → http://localhost:5050 (`admin@local.dev` / `admin`)

준비물은 **JDK 17** 과 **Docker Desktop**, 그리고 **OpenAI API 키**입니다.
Node 는 필요 없습니다. 프론트엔드 빌드 산출물이 저장소에 들어 있습니다.

키는 저장소 루트의 `.env` 에 넣습니다. 방법과 확인 방법은 [01. 시작하기](docs/01-시작하기.md) 에 있습니다.
**`application.yml` 에 키를 넣지 마세요.** 이 저장소는 공개돼 있습니다. `.env` 는 커밋되지 않습니다.

## 스키마

`init/*.sql` 이 컨테이너를 처음 생성할 때 한 번만 실행됩니다. Flyway 를 쓰지 않습니다.

| 파일 | 내용 |
| --- | --- |
| `01-extension.sql` | `vector` 확장 |
| `02-tables.sql` | `story` · `comment` · `article` · `analysis` · `body_chunk` |
| `03-indexes.sql` | GIN · B-tree |
| `04-vector.sql` | `summary_vector_store` · `body_vector_store` + HNSW · GIN |
| `05-chat-memory.sql` | `SPRING_AI_CHAT_MEMORY` |

스키마를 바꾸려면 볼륨을 삭제하고 컨테이너를 다시 생성합니다. **데이터가 지워집니다.**

```bash
docker compose down -v && docker compose up -d
```

## 프론트엔드

소스는 `frontend/`, 빌드 산출물은 `src/main/resources/static/` 에 커밋돼 있습니다.
소스를 고쳤을 때만 다시 빌드하면 됩니다.

```bash
./gradlew buildFrontend
```

이 태스크는 `build` 에 연결돼 있지 않습니다. Node 없는 환경에서 `bootRun` 이 실패하지
않게 하기 위해서입니다.
