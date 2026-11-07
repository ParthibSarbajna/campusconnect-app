-- V1: baseline reference tables. Core placement tables arrive in Phase 3 (see README).
-- No data is seeded here.

CREATE TABLE departments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(16)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_departments_code UNIQUE (code)
);

CREATE TABLE academic_years (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    start_year  SMALLINT    NOT NULL,
    label       VARCHAR(9)  NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_academic_years_start UNIQUE (start_year),
    CONSTRAINT uq_academic_years_label UNIQUE (label),
    CONSTRAINT ck_academic_years_range CHECK (start_year BETWEEN 2000 AND 2100)
);
