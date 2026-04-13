package br.com.portal.decode_api.enums;

public enum AffiliatePayoutRunStatus {
    DRAFT,       // gerada automaticamente pelo job mensal, aguarda revisao
    REVIEWED,    // admin aprovou, pronta para executar
    EXECUTING,   // pagamentos em andamento
    COMPLETED,   // todos os pagamentos efetuados
    CANCELLED    // cancelada antes de pagar
}
