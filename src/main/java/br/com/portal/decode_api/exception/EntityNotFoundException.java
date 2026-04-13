package br.com.portal.decode_api.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entity, Object id) {
        super(entity + " não encontrado(a) com id: " + id);
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
