-- 수집 원본과 처리 결과. 파이프라인 판정과 무관하게 모두 저장한다.

CREATE TABLE story (
    id           BIGINT      PRIMARY KEY,          -- HN storyId
    title        TEXT        NOT NULL,
    url          TEXT,                             -- Ask/Tell HN 은 NULL
    author       TEXT,                             -- HN by
    score        INT         NOT NULL,
    descendants  INT         NOT NULL,             -- 전체 댓글 수
    text         TEXT,                             -- 스토리 본문 (Ask/Tell HN)
    posted_at    TIMESTAMPTZ NOT NULL,             -- HN time
    collected_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 수집 범위는 application.yml 의 설정값. 트리는 depth 2 에서 끊는다.
-- parent_id 는 스토리 ID 일 수도 댓글 ID 일 수도 있어 외래키를 걸지 않는다.
-- text 에는 마스킹된 텍스트가 들어간다.
CREATE TABLE comment (
    id         BIGINT      PRIMARY KEY,
    story_id   BIGINT      NOT NULL REFERENCES story(id),
    parent_id  BIGINT      NOT NULL,               -- 스토리 또는 상위 댓글
    author     TEXT,
    text       TEXT,
    depth      SMALLINT    NOT NULL,               -- 최상위 = 1, 대댓글 = 2
    posted_at  TIMESTAMPTZ NOT NULL,
    moderation_flagged BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT depth_within_policy CHECK (depth IN (1, 2))
);

CREATE TYPE article_status AS ENUM (
    'PASS',          -- 본문 확보
    'PDF',           -- %PDF 시그니처
    'TOO_SHORT',     -- 정제 후 2000자 이하 (JS 렌더링 등)
    'FETCH_FAILED',  -- 조회 실패
    'NO_BODY'        -- LLM 이 본문을 찾지 못함
);

-- 이 테이블의 존재 여부가 중복 확인의 기준이다.
CREATE TABLE article (
    story_id     BIGINT         PRIMARY KEY REFERENCES story(id),
    status       article_status NOT NULL,
    cleaned_text TEXT,                              -- 기계적 정제 결과
    cleaned_len  INT,
    body         TEXT,                              -- LLM 추출 본문
    body_tsv     TSVECTOR GENERATED ALWAYS AS (
                     to_tsvector('english', coalesce(body, ''))
                 ) STORED,
    processed_at TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- category / tech_field 는 폴백 규칙으로 값이 늘 수 있어 ENUM 이 아니라 TEXT 다.
CREATE TABLE analysis (
    story_id              BIGINT      PRIMARY KEY REFERENCES story(id),
    summary               TEXT,
    category              TEXT,
    tech_field            TEXT,
    keywords              TEXT[]      NOT NULL DEFAULT '{}',
    community_reaction    TEXT,
    practical_implication TEXT,
    suitable              BOOLEAN     NOT NULL,
    unsuitable_reason     TEXT,
    analyzed_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reason_matches_suitable
        CHECK (suitable = (unsuitable_reason IS NULL))
);

-- seq 는 본문 내 순서. 검색 결과에서 앞뒤 청크를 이어 붙일 때 쓴다.
CREATE TABLE body_chunk (
    id       BIGSERIAL PRIMARY KEY,
    story_id BIGINT    NOT NULL REFERENCES story(id),
    seq      INT       NOT NULL,
    content  TEXT      NOT NULL,
    UNIQUE (story_id, seq)
);
