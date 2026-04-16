-- V10: Adiciona coluna 'module' às assinaturas para permitir
-- assinaturas independentes por módulo (MESA / DELIVERY / COMPLETA).
-- Um decode pode ter, ao mesmo tempo, uma assinatura MESA e outra DELIVERY
-- com durações e vencimentos diferentes.

ALTER TABLE subscriptions
    ADD COLUMN module VARCHAR(20) NULL;

-- Preenche registros existentes com base no plan_name
UPDATE subscriptions SET module = 'MESA'     WHERE LOWER(plan_name) LIKE '%mesa%' AND module IS NULL;
UPDATE subscriptions SET module = 'DELIVERY'  WHERE LOWER(plan_name) LIKE '%delivery%' AND module IS NULL;
UPDATE subscriptions SET module = 'COMPLETA'  WHERE LOWER(plan_name) LIKE '%completa%' AND module IS NULL;
-- Qualquer registro remanescente recebe COMPLETA como padrão
UPDATE subscriptions SET module = 'COMPLETA'  WHERE module IS NULL;

-- Agora torna NOT NULL
ALTER TABLE subscriptions ALTER COLUMN module SET NOT NULL;

CREATE INDEX idx_subscriptions_module ON subscriptions(module);
CREATE INDEX idx_subscriptions_decode_module_status ON subscriptions(decode_id, module, status);
