-- V6: placement drives, their eligible branches, student applications and the application status history.
CREATE TABLE placement_drives (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id            BIGINT        NOT NULL REFERENCES companies (id),
    title                 VARCHAR(160)  NOT NULL,
    description           VARCHAR(4000),
    location              VARCHAR(160),
    ctc_lpa               NUMERIC(6, 2),
    graduation_year       SMALLINT      NOT NULL,   -- the batch (cohort) this drive is open to
    min_cgpa              NUMERIC(4, 2),
    max_active_backlogs   SMALLINT,
    application_deadline  TIMESTAMPTZ   NOT NULL,
    status                VARCHAR(12)   NOT NULL DEFAULT 'DRAFT',
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_drives_status CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_drives_year CHECK (graduation_year BETWEEN 2000 AND 2100),
    CONSTRAINT ck_drives_ctc CHECK (ctc_lpa IS NULL OR ctc_lpa >= 0),
    CONSTRAINT ck_drives_cgpa CHECK (min_cgpa IS NULL OR min_cgpa BETWEEN 0 AND 10),
    CONSTRAINT ck_drives_backlogs CHECK (max_active_backlogs IS NULL OR max_active_backlogs >= 0)
);
CREATE INDEX ix_drives_status_deadline ON placement_drives (status, application_deadline);
CREATE INDEX ix_drives_company ON placement_drives (company_id);
CREATE INDEX ix_drives_year ON placement_drives (graduation_year);

-- No rows for a drive means "all branches are eligible".
CREATE TABLE drive_branches (
    drive_id  BIGINT       NOT NULL REFERENCES placement_drives (id) ON DELETE CASCADE,
    branch    VARCHAR(120) NOT NULL,
    PRIMARY KEY (drive_id, branch)
);

CREATE TABLE applications (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    drive_id         BIGINT      NOT NULL REFERENCES placement_drives (id),
    student_user_id  BIGINT      NOT NULL REFERENCES users (id),
    status           VARCHAR(12) NOT NULL DEFAULT 'APPLIED',
    applied_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_applications_drive_student UNIQUE (drive_id, student_user_id),
    CONSTRAINT ck_applications_status CHECK (status IN ('APPLIED', 'SHORTLISTED', 'SELECTED', 'REJECTED', 'WITHDRAWN'))
);
CREATE INDEX ix_applications_student ON applications (student_user_id, applied_at DESC);
CREATE INDEX ix_applications_drive_status ON applications (drive_id, status);

-- Append-only audit trail of every status change.
CREATE TABLE application_status_history (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id  BIGINT      NOT NULL REFERENCES applications (id),
    from_status     VARCHAR(12),
    to_status       VARCHAR(12) NOT NULL,
    changed_by      BIGINT      NOT NULL REFERENCES users (id),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    note            VARCHAR(500)
);
CREATE INDEX ix_app_history_application ON application_status_history (application_id, changed_at);
