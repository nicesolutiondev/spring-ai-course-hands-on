CREATE INDEX ON comment (story_id);
CREATE INDEX ON comment (parent_id);

CREATE INDEX ON article USING GIN (body_tsv);
CREATE INDEX ON article (status);

-- keywords 는 배열 겹침(&&)으로 매칭하므로 형태소 분석이 필요 없다.
CREATE INDEX ON analysis USING GIN (keywords);
CREATE INDEX ON analysis (tech_field);
CREATE INDEX ON analysis (category);
CREATE INDEX ON analysis (suitable);

CREATE INDEX ON body_chunk (story_id);
