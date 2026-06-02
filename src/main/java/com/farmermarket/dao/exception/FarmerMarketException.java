package com.farmermarket.dao.exception;

/*
 Base unchecked exception for the Farmer–Market platform.
 */
public class FarmerMarketException extends RuntimeException {

    public FarmerMarketException(String message) {
        super(message);
    }

    public FarmerMarketException(String message, Throwable cause) {
        super(message, cause);
    }
}
