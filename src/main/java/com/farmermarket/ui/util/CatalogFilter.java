package com.farmermarket.ui.util;

import com.farmermarket.model.Product;

import java.util.List;
import java.util.stream.Collectors;

/*
 Pure utility class for filtering the product catalog.
 */
public final class CatalogFilter {


    private CatalogFilter() {}


    public static List<Product> search(List<Product> products, String query) {
        if (query == null || query.trim().isEmpty()) {
            return products;
        }
        String lowerQuery = query.toLowerCase();
        return products.stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerQuery)
                        || p.getCategory().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }


    public static List<Product> filterByCategory(List<Product> products, String category) {
        if (category == null || category.trim().isEmpty() || category.equals("All")) {
            return products;
        }
        return products.stream()
                .filter(p -> p.getCategory().equals(category))
                .collect(Collectors.toList());
    }
}
