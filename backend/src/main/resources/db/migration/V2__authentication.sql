-- V2: user accounts, roles, lockout counters. V1 is untouched.
CREATE TABLE users (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email                  VARCHAR(254) NOT NULL,
    full_name              VARCHAR(120) NOT NULL,
    password_hash          VARCHAR(100) NOT NULL,
    role                   VARCHAR(20)  NOT NULL,
    department_id          BIGINT       REFERENCES departments (id),
    enabled                BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts  INTEGER      NOT NULL DEFAULT 0,
    locked_until           TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email)) AND position('@' IN email) > 1),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'DEPARTMENT_SPOC', 'PLACEMENT_ADMIN')),
    CONSTRAINT ck_users_spoc_department CHECK (role <> 'DEPARTMENT_SPOC' OR department_id IS NOT NULL),
    CONSTRAINT ck_users_failed_attempts CHECK (failed_login_attempts >= 0)
);
CREATE INDEX ix_users_department ON users (department_id);
CREATE INDEX ix_users_role ON users (role);
