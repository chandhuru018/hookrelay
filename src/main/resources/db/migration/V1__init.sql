CREATE TABLE tenants (
                         id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         name         VARCHAR(100) NOT NULL,
                         api_key_hash CHAR(64)     NOT NULL UNIQUE,
                         created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE endpoints (
                           id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           tenant_id            BIGINT       NOT NULL REFERENCES tenants(id),
                           url                  VARCHAR(500) NOT NULL,
                           secret               VARCHAR(100) NOT NULL,
                           status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                           consecutive_failures INT          NOT NULL DEFAULT 0,
                           created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE events (
                        id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        tenant_id       BIGINT       NOT NULL REFERENCES tenants(id),
                        type            VARCHAR(100) NOT NULL,
                        payload         JSONB        NOT NULL,
                        idempotency_key VARCHAR(100) NOT NULL,
                        created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                        UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE deliveries (
                            id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            event_id        BIGINT      NOT NULL REFERENCES events(id),
                            endpoint_id     BIGINT      NOT NULL REFERENCES endpoints(id),
                            status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                            attempt_count   INT         NOT NULL DEFAULT 0,
                            next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                            last_error      VARCHAR(500),
                            created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_deliveries_queue ON deliveries (status, next_attempt_at);

CREATE TABLE delivery_attempts (
                                   id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   delivery_id  BIGINT      NOT NULL REFERENCES deliveries(id),
                                   attempted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   http_status  INT,
                                   latency_ms   INT,
                                   error        VARCHAR(500)
);