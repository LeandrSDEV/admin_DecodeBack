package br.com.portal.decode_api.enums;

/**
 * Módulo coberto por uma assinatura.
 * Um decode pode ter assinaturas independentes para MESA e DELIVERY,
 * cada uma com duração e vencimento próprios.
 * COMPLETA cobre ambos os módulos em uma única assinatura.
 */
public enum SubscriptionModule {
    MESA,
    DELIVERY,
    COMPLETA;

    /** Retorna true se esta assinatura cobre o módulo de mesas. */
    public boolean coversMesa() {
        return this == MESA || this == COMPLETA;
    }

    /** Retorna true se esta assinatura cobre o módulo de delivery. */
    public boolean coversDelivery() {
        return this == DELIVERY || this == COMPLETA;
    }
}
