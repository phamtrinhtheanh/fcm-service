-- init_db.sql
CREATE SCHEMA IF NOT EXISTS device;
SET search_path TO device;

-- user_device table
CREATE TABLE IF NOT EXISTS user_device (
    userid       BIGINT      NOT NULL,
    app          VARCHAR(25) NOT NULL,
    device_token VARCHAR(500),
    status       SMALLINT    DEFAULT 1,
    create_date  TIMESTAMP   DEFAULT now(),
    last_update  TIMESTAMP   DEFAULT now(),
    PRIMARY KEY (userid, app)
);

-- notification table (partitioned by month)
DROP TABLE IF EXISTS notification CASCADE;
CREATE SEQUENCE IF NOT EXISTS notification_id_seq START 1;

CREATE TABLE notification (
    notification_id BIGINT      DEFAULT nextval('notification_id_seq'),
    receiver_id     VARCHAR(50) NOT NULL,
    action          VARCHAR(250),
    app             VARCHAR(25),
    icon            VARCHAR(250),
    title           VARCHAR(500),
    content         VARCHAR(3000),
    ext_data        JSONB,
    created_by      BIGINT      DEFAULT -1,
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id, created_at)
) PARTITION BY RANGE (created_at);

-- Create partitions for 2026
DO $$
DECLARE
    d DATE := '2026-01-01';
BEGIN
    FOR i IN 1..24 LOOP
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS notification_p%s PARTITION OF notification
             FOR VALUES FROM (%L) TO (%L)',
            to_char(d, 'YYYY_MM'),
            d,
            d + INTERVAL '1 month'
        );
        d := d + INTERVAL '1 month';
    END LOOP;
END $$;

CREATE TABLE notification_default PARTITION OF notification DEFAULT;
CREATE INDEX IF NOT EXISTS idx_notification_receiver_date ON notification (receiver_id, created_at DESC);

-- user_notification table (partitioned)
DROP TABLE IF EXISTS user_notification CASCADE;

CREATE TABLE user_notification (
    notification_id BIGINT      NOT NULL,
    user_id         VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP   NOT NULL,
    PRIMARY KEY (user_id, notification_id, created_at)
) PARTITION BY RANGE (created_at);

DO $$
DECLARE
    d DATE := '2026-01-01';
BEGIN
    FOR i IN 1..24 LOOP
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS user_notification_p%s PARTITION OF user_notification
             FOR VALUES FROM (%L) TO (%L)',
            to_char(d, 'YYYY_MM'),
            d,
            d + INTERVAL '1 month'
        );
        d := d + INTERVAL '1 month';
    END LOOP;
END $$;

CREATE TABLE user_notification_default PARTITION OF user_notification DEFAULT;
