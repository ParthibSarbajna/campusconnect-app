-- V3: student profiles and academic information.
-- One profile per student account; account identity and role remain in users.

CREATE TABLE student_profiles (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    roll_number         VARCHAR(40),
    phone               VARCHAR(20),
    date_of_birth       DATE,
    degree              VARCHAR(100),
    branch              VARCHAR(120),
    graduation_year     SMALLINT,
    cgpa                NUMERIC(4, 2),
    active_backlogs     SMALLINT NOT NULL DEFAULT 0,
    resume_url          VARCHAR(2048),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_student_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_student_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_student_profiles_roll_number UNIQUE (roll_number),
    CONSTRAINT ck_student_profiles_graduation_year
        CHECK (graduation_year IS NULL OR graduation_year BETWEEN 2000 AND 2100),
    CONSTRAINT ck_student_profiles_cgpa
        CHECK (cgpa IS NULL OR cgpa BETWEEN 0 AND 10),
    CONSTRAINT ck_student_profiles_backlogs
        CHECK (active_backlogs >= 0)
);

CREATE INDEX ix_student_profiles_graduation_year
    ON student_profiles (graduation_year);