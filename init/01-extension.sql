-- 설치하는 확장은 vector 하나뿐이다.
-- Spring AI 가 스키마를 만들면 hstore · uuid-ossp 까지 설치하려 들기 때문에
-- initialize-schema 를 끄고 이 파일로 직접 만든다.
CREATE EXTENSION IF NOT EXISTS vector;
