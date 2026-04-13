package br.com.portal.decode_api.enums;

public enum AffiliateStatus {
    PENDING,    // cadastro recebido, aguardando aprovacao manual
    ACTIVE,     // aprovado, pode indicar e receber comissao
    SUSPENDED,  // temporariamente bloqueado (sem acesso ao portal)
    BANNED      // banimento definitivo
}
