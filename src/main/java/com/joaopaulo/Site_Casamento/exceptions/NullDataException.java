package com.joaopaulo.Site_Casamento.exceptions;

public class NullDataException extends RuntimeException {
    public NullDataException(String message) {
        super(message);
    }
    public NullDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
