package br.com.portal.decode_api.enums;

public enum AffiliateReferralStatus {
    CLICKED,    // apenas o click no link foi registrado
    LEAD,       // virou lead na base
    CONVERTED,  // virou decode (cliente pagante)
    CHURNED     // cliente cancelou a assinatura
}
