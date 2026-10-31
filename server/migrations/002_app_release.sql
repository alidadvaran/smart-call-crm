-- Holds the latest uploaded APK so devices can self-update.
CREATE TABLE IF NOT EXISTS app_release (
    id           INT PRIMARY KEY DEFAULT 1,
    version_code INT NOT NULL,
    version_name TEXT NOT NULL,
    file_path    TEXT NOT NULL,
    size_bytes   BIGINT NOT NULL DEFAULT 0,
    uploaded_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT app_release_single_row CHECK (id = 1)
);
