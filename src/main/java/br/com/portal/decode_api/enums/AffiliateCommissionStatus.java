package br.com.portal.decode_api.enums;

public enum AffiliateCommissionStatus {
    PENDING,   // calculada, em carencia (antes do 2o mes efetivo)
    APPROVED,  // liberada para pagamento
    PAID,      // paga via PIX
    REVERSED,  // cliente cancelou, estornada
    HELD       // retida por fraude/investigacao
}
