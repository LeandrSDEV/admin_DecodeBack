-- V8: Sistema de Afiliados (PostgreSQL)

-- 1) Tabela principal de afiliados
CREATE TABLE IF NOT EXISTS affiliates (
  id              CHAR(36)     PRIMARY KEY,
  ref_code        VARCHAR(40)  NOT NULL UNIQUE,
  name            VARCHAR(180) NOT NULL,
  email           VARCHAR(180) NOT NULL UNIQUE,
  whatsapp        VARCHAR(30)  NOT NULL,
  cpf             VARCHAR(14)  NULL,
  city            VARCHAR(120) NULL,
  state           VARCHAR(4)   NULL,

  pix_key_type    VARCHAR(20)  NULL,
  pix_key         VARCHAR(180) NULL,
  bank_holder     VARCHAR(180) NULL,

  password_hash   VARCHAR(255) NULL,
  must_change_pw  BOOLEAN      NOT NULL DEFAULT TRUE,
  last_login_at   TIMESTAMP    NULL,

  status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

  approved_at     TIMESTAMP    NULL,
  approved_by     CHAR(36)     NULL,

  terms_accepted_at       TIMESTAMP NULL,
  terms_accepted_version  VARCHAR(20) NULL,

  custom_commission_rate  DECIMAL(5,2) NULL,
  notes                   TEXT NULL,

  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_affiliates_approver
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_affiliates_status ON affiliates(status);
CREATE INDEX idx_affiliates_refcode ON affiliates(ref_code);

-- 2) Indicacoes (referrals)
CREATE TABLE IF NOT EXISTS affiliate_referrals (
  id              CHAR(36)    PRIMARY KEY,
  affiliate_id    CHAR(36)    NOT NULL,

  first_touch_at  TIMESTAMP   NOT NULL,
  source_ip       VARCHAR(60) NULL,
  user_agent      VARCHAR(500) NULL,
  landing_url     VARCHAR(500) NULL,

  status          VARCHAR(20) NOT NULL DEFAULT 'CLICKED',

  lead_id         CHAR(36)   NULL,
  decode_id       CHAR(36)   NULL,
  converted_at    TIMESTAMP  NULL,
  churned_at      TIMESTAMP  NULL,

  created_at      TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_refs_affiliate FOREIGN KEY (affiliate_id) REFERENCES affiliates(id) ON DELETE CASCADE,
  CONSTRAINT fk_refs_lead      FOREIGN KEY (lead_id)      REFERENCES leads(id)      ON DELETE SET NULL,
  CONSTRAINT fk_refs_decode    FOREIGN KEY (decode_id)    REFERENCES decodes(id)    ON DELETE SET NULL
);

CREATE INDEX idx_refs_affiliate ON affiliate_referrals(affiliate_id, created_at DESC);
CREATE INDEX idx_refs_lead      ON affiliate_referrals(lead_id);
CREATE INDEX idx_refs_decode    ON affiliate_referrals(decode_id);
CREATE INDEX idx_refs_status    ON affiliate_referrals(status);

-- 3) Comissoes mensais
CREATE TABLE IF NOT EXISTS affiliate_commissions (
  id              CHAR(36)      PRIMARY KEY,
  affiliate_id    CHAR(36)      NOT NULL,
  decode_id       CHAR(36)      NOT NULL,
  subscription_id CHAR(36)      NULL,

  reference_month DATE          NOT NULL,

  plan_name       VARCHAR(120)  NOT NULL,
  plan_price      DECIMAL(12,2) NOT NULL,
  commission_rate DECIMAL(5,2)  NOT NULL,
  commission_amount DECIMAL(12,2) NOT NULL,

  status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',

  carencia_until  DATE          NOT NULL,

  payout_run_id   CHAR(36)      NULL,
  paid_at         TIMESTAMP     NULL,
  paid_reference  VARCHAR(180)  NULL,
  reversed_at     TIMESTAMP     NULL,
  reverse_reason  TEXT          NULL,

  notes           TEXT          NULL,
  created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_comm_affiliate    FOREIGN KEY (affiliate_id)    REFERENCES affiliates(id)     ON DELETE CASCADE,
  CONSTRAINT fk_comm_decode       FOREIGN KEY (decode_id)       REFERENCES decodes(id)        ON DELETE CASCADE,
  CONSTRAINT fk_comm_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)  ON DELETE SET NULL,
  CONSTRAINT uk_comm_unique_period UNIQUE (affiliate_id, decode_id, reference_month)
);

CREATE INDEX idx_comm_affiliate_month ON affiliate_commissions(affiliate_id, reference_month DESC);
CREATE INDEX idx_comm_status          ON affiliate_commissions(status);
CREATE INDEX idx_comm_payout_run      ON affiliate_commissions(payout_run_id);

-- 4) Payout runs
CREATE TABLE IF NOT EXISTS affiliate_payout_runs (
  id                CHAR(36)      PRIMARY KEY,
  reference_month   DATE          NOT NULL,
  total_affiliates  INT           NOT NULL DEFAULT 0,
  total_commissions INT           NOT NULL DEFAULT 0,
  total_amount      DECIMAL(14,2) NOT NULL DEFAULT 0,

  status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',

  generated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at       TIMESTAMP     NULL,
  reviewed_by       CHAR(36)      NULL,
  completed_at      TIMESTAMP     NULL,
  notes             TEXT          NULL,

  updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_payout_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT uk_payout_month UNIQUE (reference_month)
);

CREATE INDEX idx_payout_status ON affiliate_payout_runs(status);

-- FK da comissao para o payout run
ALTER TABLE affiliate_commissions
  ADD CONSTRAINT fk_comm_payout_run
  FOREIGN KEY (payout_run_id) REFERENCES affiliate_payout_runs(id) ON DELETE SET NULL;

-- 5) Link afiliado -> leads e decodes
ALTER TABLE leads
  ADD COLUMN affiliate_id CHAR(36) NULL;
ALTER TABLE leads
  ADD CONSTRAINT fk_leads_affiliate FOREIGN KEY (affiliate_id) REFERENCES affiliates(id) ON DELETE SET NULL;

CREATE INDEX idx_leads_affiliate ON leads(affiliate_id);

ALTER TABLE decodes
  ADD COLUMN affiliate_id CHAR(36) NULL;
ALTER TABLE decodes
  ADD COLUMN affiliate_attached_at TIMESTAMP NULL;
ALTER TABLE decodes
  ADD CONSTRAINT fk_decodes_affiliate FOREIGN KEY (affiliate_id) REFERENCES affiliates(id) ON DELETE SET NULL;

CREATE INDEX idx_decodes_affiliate ON decodes(affiliate_id);

-- 6) Log de clicks
CREATE TABLE IF NOT EXISTS affiliate_click_logs (
  id           CHAR(36)     PRIMARY KEY,
  ref_code     VARCHAR(40)  NOT NULL,
  affiliate_id CHAR(36)     NULL,
  source_ip    VARCHAR(60)  NULL,
  user_agent   VARCHAR(500) NULL,
  referer      VARCHAR(500) NULL,
  landing_url  VARCHAR(500) NULL,
  clicked_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_clicks_affiliate FOREIGN KEY (affiliate_id) REFERENCES affiliates(id) ON DELETE SET NULL
);

CREATE INDEX idx_clicks_affiliate ON affiliate_click_logs(affiliate_id, clicked_at DESC);
CREATE INDEX idx_clicks_refcode   ON affiliate_click_logs(ref_code);
