-- V9: Expansao da tabela subscriptions para virar "planos" comerciais
-- Permite cadastrar plano sem decode vinculado (pre-venda) e adiciona
-- estabelecimento, cliente e desconto.

ALTER TABLE subscriptions
    ALTER COLUMN decode_id DROP NOT NULL;

ALTER TABLE subscriptions
    ADD COLUMN establishment_name VARCHAR(160) NULL;
ALTER TABLE subscriptions
    ADD COLUMN client_name VARCHAR(160) NULL;
ALTER TABLE subscriptions
    ADD COLUMN discount_pct DECIMAL(5,2) NOT NULL DEFAULT 0.00;

CREATE INDEX idx_subscriptions_establishment ON subscriptions(establishment_name);
CREATE INDEX idx_subscriptions_client ON subscriptions(client_name);
