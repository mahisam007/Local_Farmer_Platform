package com.farmermarket.dao.exception;

/*Thrown  when login credentials are invalid (unknown email or wrong password).
 */
public class AuthenticationException extends FarmerMarketException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
