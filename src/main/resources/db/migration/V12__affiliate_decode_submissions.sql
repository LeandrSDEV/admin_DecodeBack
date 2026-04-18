-- V12: Proposta de novo estabelecimento submetida pelo afiliado.
-- O afiliado preenche os dados do cliente fechado e escolhe o plano;
-- o admin revisa e aprova -> cria o Decode, a Subscription e o Referral CONVERTED,
-- ativando o plano. Ao rejeitar, registra motivo.

CREATE TABLE IF NOT EXISTS affiliate_decode_submissions (
  id                      CHAR(36)     PRIMARY KEY,
  affiliate_id            CHAR(36)     NOT NULL,

  establishment_name      VARCHAR(180) NOT NULL,
  city                    VARCHAR(120) NOT NULL,
  state                   VARCHAR(4)   NULL,
  cnpj                    VARCHAR(20)  NULL,

  contact_name            VARCHAR(180) NOT NULL,
  contact_email           VARCHAR(180) NULL,
  contact_phone           VARCHAR(30)  NOT NULL,

  estimated_users_count   INT          NULL,
  estimated_monthly_revenue DECIMAL(12,2) NULL,

  plan_module             VARCHAR(20)  NOT NULL,
  plan_name               VARCHAR(120) NOT NULL,
  plan_price              DECIMAL(12,2) NOT NULL,
  plan_discount_pct       DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  plan_duration_days      INT          NOT NULL DEFAULT 30,
  plan_features           TEXT         NULL,

  notes                   TEXT         NULL,

  status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

  submitted_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at             TIMESTAMP    NULL,
  reviewed_by             CHAR(36)     NULL,
  rejection_reason        TEXT         NULL,

  decode_id               CHAR(36)     NULL,
  subscription_id         CHAR(36)     NULL,

  created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_decsub_affiliate    FOREIGN KEY (affiliate_id)    REFERENCES affiliates(id)    ON DELETE CASCADE,
  CONSTRAINT fk_decsub_reviewer     FOREIGN KEY (reviewed_by)     REFERENCES users(id)         ON DELETE SET NULL,
  CONSTRAINT fk_decsub_decode       FOREIGN KEY (decode_id)       REFERENCES decodes(id)       ON DELETE SET NULL,
  CONSTRAINT fk_decsub_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE SET NULL
);

CREATE INDEX idx_decsub_affiliate_status ON affiliate_decode_submissions(affiliate_id, status);
CREATE INDEX idx_decsub_status_date      ON affiliate_decode_submissions(status, submitted_at DESC);
