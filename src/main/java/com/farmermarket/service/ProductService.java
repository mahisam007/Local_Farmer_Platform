package com.farmermarket.service;

import com.farmermarket.dao.OrderDAO;
import com.farmermarket.dao.ProductDAO;
import com.farmermarket.dao.exception.DataAccessException;
import com.farmermarket.dao.exception.ValidationException;
import com.farmermarket.model.Product;
import com.farmermarket.socket.NotificationServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductDAO productDAO;
    private final OrderDAO   orderDAO;
    private final FairPriceCalculator calculator;


    private NotificationServer notificationServer;


    public ProductService(ProductDAO productDAO, OrderDAO orderDAO,
                          FairPriceCalculator calculator) {
        this.productDAO = productDAO;
        this.orderDAO   = orderDAO;
        this.calculator = calculator;
    }


    public void setNotificationServer(NotificationServer server) {
        this.notificationServer = server;
    }


    public Product addProduct(Product product) throws ValidationException, DataAccessException {
        validate(product);
        Product saved = productDAO.save(product);
        log.info("Added product id={}, name={}", saved.getId(), saved.getName());
        if (notificationServer != null) notificationServer.broadcastCatalogUpdated();
        return saved;
    }


    public void updateProduct(Product product) throws ValidationException, DataAccessException {
        validate(product);
        productDAO.update(product);
        log.info("Updated product id={}", product.getId());
        if (notificationServer != null) notificationServer.broadcastCatalogUpdated();
    }

    public void deleteProduct(int productId) throws DataAccessException {
        productDAO.delete(productId);
        log.info("Deleted product id={}", productId);
        if (notificationServer != null) notificationServer.broadcastCatalogUpdated();
    }


    public List<Product> getProductsForFarmer(int farmerId) throws DataAccessException {
        return productDAO.findByFarmerId(farmerId);
    }


    public List<Product> getCatalogWithFairPrices() throws DataAccessException {

        List<Product> allProducts = productDAO.findAll();
        List<Product> inStock = allProducts.stream()
                .filter(p -> p.getQuantity() > 0)
                .collect(Collectors.toList());

        Map<String, Double>  avgPrices  = productDAO.getAveragePriceByCategory();
        Map<String, Integer> supply     = productDAO.getTotalSupplyByCategory();
        Map<String, Integer> demand     = orderDAO.getTotalPendingDemandByCategory();


        for (Product p : inStock) {
            String cat = p.getCategory();
            double avg  = avgPrices.getOrDefault(cat, p.getPricePerUnit());
            int    sup  = supply.getOrDefault(cat, p.getQuantity());
            int    dem  = demand.getOrDefault(cat, 0);
            double fair = calculator.calculate(avg, sup, dem);
            p.setFairPrice(fair);
        }

        return inStock;
    }




    private void validate(Product product) throws ValidationException {
        if (isBlank(product.getName())) {
            throw new ValidationException("Product name is required.");
        }
        if (isBlank(product.getCategory())) {
            throw new ValidationException("Product category is required.");
        }
        if (product.getQuantity() < 0) {
            throw new ValidationException("Quantity cannot be negative.");
        }
        if (product.getPricePerUnit() <= 0) {
            throw new ValidationException("Price per unit must be greater than zero.");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
