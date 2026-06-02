package com.farmermarket.dao.exception;

/*
 * Thrown by the Service layer when user-supplied input fails validation.
 */
public class ValidationException extends FarmerMarketException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
