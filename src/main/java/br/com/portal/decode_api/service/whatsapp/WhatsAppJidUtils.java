package br.com.portal.decode_api.service.whatsapp;

/**
 * Utilitario para normalizar numeros em JIDs aceitos pelo whatsapp-bridge.
 * Regras extraidas do bridge Baileys: usuario individual termina em
 * {@code @s.whatsapp.net}; grupos ({@code @g.us}) e broadcasts nao sao aceitos.
 */
public final class WhatsAppJidUtils {

    private WhatsAppJidUtils() {}

    public static String normalizeRemoteJid(String input) {
        if (input == null) return null;
        String value = input.trim();
        if (value.isEmpty()) return null;

        int deviceSep = value.indexOf(':');
        int atSep = value.indexOf('@');
        if (deviceSep > 0 && (atSep < 0 || deviceSep < atSep)) {
            value = value.substring(0, deviceSep) + value.substring(atSep > 0 ? atSep : value.length());
        }
        if (value.endsWith("@c.us")) {
            value = value.substring(0, value.length() - "@c.us".length()) + "@s.whatsapp.net";
        }
        if (!value.contains("@")) {
            String digits = value.replaceAll("\\D", "");
            if (digits.isEmpty()) return null;
            value = digits + "@s.whatsapp.net";
        }

        int newAt = value.indexOf('@');
        if (newAt <= 0) return null;
        String local = value.substring(0, newAt).replaceAll("\\D", "");
        String domain = value.substring(newAt).toLowerCase();
        if (local.isEmpty()) return null;
        return local + domain;
    }

    /** Garante prefixo 55 (Brasil) para numeros com 10/11 digitos. */
    public static String ensureBrazilCountryCode(String input) {
        String jid = normalizeRemoteJid(input);
        if (jid == null) return null;
        int at = jid.indexOf('@');
        String digits = jid.substring(0, at);
        if (digits.length() == 10 || digits.length() == 11) {
            digits = "55" + digits;
        }
        return digits + jid.substring(at);
    }
}