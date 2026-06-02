package com.farmermarket.dao.exception;

/*
 * Thrown by DAO classes when a JDBC / SQL operation fails.
 */
public class DataAccessException extends FarmerMarketException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
