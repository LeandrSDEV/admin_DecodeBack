-- V7: Subscription (assinatura) history for each Decode

CREATE TABLE IF NOT EXISTS subscriptions (
  id CHAR(36) PRIMARY KEY,
  decode_id CHAR(36) NOT NULL,
  plan_name VARCHAR(120) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  duration_days INT NOT NULL,
  features TEXT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  started_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  cancelled_at TIMESTAMP NULL,
  cancel_reason TEXT NULL,
  notes TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_subscriptions_decode FOREIGN KEY (decode_id) REFERENCES decodes(id) ON DELETE CASCADE
);

CREATE INDEX idx_subscriptions_decode ON subscriptions(decode_id, started_at DESC);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_expires ON subscriptions(expires_at);
