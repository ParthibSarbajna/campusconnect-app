-- V7: private student resume file storage.
ALTER TABLE student_profiles
    ADD COLUMN resume_file_name VARCHAR(255),
    ADD COLUMN resume_content_type VARCHAR(100),
    ADD COLUMN resume_data BYTEA,
    ADD COLUMN resume_uploaded_at TIMESTAMPTZ;
