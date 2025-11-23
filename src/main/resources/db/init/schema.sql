
CREATE SCHEMA IF NOT EXISTS minicrm;

CREATE TABLE IF NOT EXISTS minicrm.tg_users (
                                               id          BIGSERIAL     PRIMARY KEY,
                                               chat_id     BIGINT        NOT NULL UNIQUE,
                                               username    VARCHAR(64),
                                               phone       VARCHAR(50),
                                               first_name  VARCHAR(150),
                                               last_name   VARCHAR(150),
                                               notify      BOOLEAN       NOT NULL DEFAULT FALSE,
                                               created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                                               updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_tg_user_notify ON minicrm.tg_users(notify);

CREATE TABLE IF NOT EXISTS minicrm.leads (
                                            id         BIGSERIAL     PRIMARY KEY,
                                            fio        VARCHAR(150)  NOT NULL,
                                            phone      VARCHAR(50)   NOT NULL,
                                            district   VARCHAR(100)  NOT NULL,
                                            source     VARCHAR(100)  NOT NULL,
                                            quantity   INTEGER       NOT NULL CHECK (quantity >= 0),
                                            amount     NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
                                            created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_lead_created_at ON minicrm.leads(created_at);
CREATE INDEX IF NOT EXISTS ix_lead_source     ON minicrm.leads(source);
CREATE INDEX IF NOT EXISTS ix_lead_district   ON minicrm.leads(district);
