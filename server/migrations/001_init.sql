-- Smart Call CRM — central schema.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS devices (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_uuid  TEXT UNIQUE NOT NULL,
    name         TEXT NOT NULL,
    role         TEXT NOT NULL DEFAULT 'PRIMARY',
    token_hash   TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen    TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS customers (
    id                BIGSERIAL PRIMARY KEY,
    number_normalized TEXT UNIQUE NOT NULL,
    display_name      TEXT NOT NULL,
    company           TEXT,
    status            TEXT NOT NULL DEFAULT 'NEW',
    notes             TEXT,
    last_call_at      BIGINT NOT NULL DEFAULT 0,
    call_count        INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS calls (
    id                BIGSERIAL PRIMARY KEY,
    client_uuid       TEXT NOT NULL,
    device_id         UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    device_uuid       TEXT NOT NULL,
    source_id         BIGINT NOT NULL,
    raw_number        TEXT,
    number_normalized TEXT,
    contact_name      TEXT,
    type              INT NOT NULL,
    started_at        BIGINT NOT NULL,
    duration_sec      BIGINT NOT NULL DEFAULT 0,
    phone_account_id  TEXT,
    category          TEXT NOT NULL DEFAULT 'UNSET',
    customer_id       BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Idempotency key: the same call uploaded twice by a device is rejected.
    UNIQUE (device_uuid, client_uuid)
);

CREATE INDEX IF NOT EXISTS idx_calls_started ON calls (started_at);
CREATE INDEX IF NOT EXISTS idx_calls_number  ON calls (number_normalized);
CREATE INDEX IF NOT EXISTS idx_calls_device  ON calls (device_uuid);
CREATE INDEX IF NOT EXISTS idx_calls_type    ON calls (type);

CREATE TABLE IF NOT EXISTS report_log (
    id        BIGSERIAL PRIMARY KEY,
    type      TEXT NOT NULL,
    recipient TEXT NOT NULL,
    ok        BOOLEAN NOT NULL,
    detail    TEXT,
    sent_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
