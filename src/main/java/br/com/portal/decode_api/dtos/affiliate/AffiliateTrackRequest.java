package br.com.portal.decode_api.dtos.affiliate;

/** Endpoint publico para registrar click no link do afiliado. */
public record AffiliateTrackRequest(
        String refCode,
        String userAgent,
        String referer,
        String landingUrl
) {}
