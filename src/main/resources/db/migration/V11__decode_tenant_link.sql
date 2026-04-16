-- Link entre decode (Admin) e tenant (backend operacional)
-- Permite sincronizar datas de assinatura automaticamente
ALTER TABLE decodes ADD COLUMN IF NOT EXISTS tenant_id BIGINT;