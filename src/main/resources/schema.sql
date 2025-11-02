-- Создание схемы и таблицы для miniCRM
CREATE SCHEMA IF NOT EXISTS minicrm;

CREATE TABLE IF NOT EXISTS minicrm.crm_user (
                                                id BIGSERIAL PRIMARY KEY,
                                                fio VARCHAR(150) NOT NULL,
                                                phone VARCHAR(50) NOT NULL,
                                                district VARCHAR(100) NOT NULL,
                                                source VARCHAR(100) NOT NULL,
                                                quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
                                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Индексы для ускорения поиска
CREATE INDEX IF NOT EXISTS ix_crm_user_district ON minicrm.crm_user(district);
CREATE INDEX IF NOT EXISTS ix_crm_user_source   ON minicrm.crm_user(source);

-- Уникальный индекс по нормализованному телефону (только цифры)
CREATE UNIQUE INDEX IF NOT EXISTS ux_crm_user_phone_norm
    ON minicrm.crm_user ((regexp_replace(phone, '\D', '', 'g')));
