package com.farmermarket.service;

/**
 * Pure computation class that calculates a Fair Price
 */
public class FairPriceCalculator {


    private static final double MAX_DISCOUNT = 0.85;


    private static final double MAX_PREMIUM  = 1.20;

    /*
     * Computes the fair price for a product category.     */
    public double calculate(double averageListedPrice, int totalSupply, int totalPendingDemand) {
        if (averageListedPrice <= 0) {
            return 0.0;
        }


        if (totalSupply == 0 && totalPendingDemand == 0) {
            return averageListedPrice;
        }

        double ratio = (totalSupply == 0)
                ? Double.MAX_VALUE
                : (double) totalPendingDemand / totalSupply;

        double factor;

        if (ratio < 1.0) {
            factor = MAX_DISCOUNT + (1.0 - MAX_DISCOUNT) * ratio;

        } else if (ratio > 1.0) {
            double cappedRatio = Math.min(ratio, 2.0);
            factor = 1.0 + (MAX_PREMIUM - 1.0) * (cappedRatio - 1.0);

        } else {

            factor = 1.0;
        }

        return averageListedPrice * factor;
    }
}
