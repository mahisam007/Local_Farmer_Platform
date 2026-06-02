package com.farmermarket.service;

import com.farmermarket.dao.OrderDAO;
import com.farmermarket.dao.ProductDAO;
import com.farmermarket.dao.exception.DataAccessException;
import com.farmermarket.dao.exception.ValidationException;
import com.farmermarket.model.Order;
import com.farmermarket.model.OrderStatus;
import com.farmermarket.model.Product;
import com.farmermarket.socket.NotificationServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;


public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderDAO   orderDAO;
    private final ProductDAO productDAO;


    private NotificationServer notificationServer;


    public OrderService(OrderDAO orderDAO, ProductDAO productDAO) {
        this.orderDAO   = orderDAO;
        this.productDAO = productDAO;
    }


    public void setNotificationServer(NotificationServer server) {
        this.notificationServer = server;
    }


    public Order placeOrder(int buyerId, int productId, int quantity)
            throws ValidationException, DataAccessException {

        if (quantity <= 0) {
            throw new ValidationException("Order quantity must be greater than zero.");
        }

        Optional<Product> productOpt = productDAO.findById(productId);
        if (productOpt.isEmpty()) {
            throw new ValidationException("Product not found: " + productId);
        }
        Product product = productOpt.get();

        if (quantity > product.getQuantity()) {
            throw new ValidationException(
                    "Insufficient stock. Available: " + product.getQuantity()
                            + ", requested: " + quantity);
        }

        double totalPrice = quantity * product.getPricePerUnit();
        Order order = new Order(0, buyerId, productId, quantity, totalPrice, OrderStatus.PENDING);
        Order saved = orderDAO.save(order);

        product.setQuantity(product.getQuantity() - quantity);
        productDAO.update(product);

        log.info("Order placed: id={}, buyerId={}, productId={}, qty={}, total={}",
                saved.getId(), buyerId, productId, quantity, totalPrice);

        // Notify all clients that the catalog stock changed
        if (notificationServer != null) {
            notificationServer.broadcastCatalogUpdated();
        }

        return saved;
    }


    public void markShipped(int orderId) throws DataAccessException {
        orderDAO.updateStatus(orderId, OrderStatus.SHIPPED);
        log.info("Order {} marked as SHIPPED", orderId);

        // Push socket notification to the buyer
        if (notificationServer != null) {
            Optional<Order> orderOpt = orderDAO.findById(orderId);
            orderOpt.ifPresent(order ->
                    notificationServer.notifyOrderShipped(orderId, order.getBuyerId())
            );
        }
    }


    public void markDelivered(int orderId) throws DataAccessException {
        orderDAO.updateStatus(orderId, OrderStatus.DELIVERED);
        log.info("Order {} marked as DELIVERED", orderId);

        // Push socket notification to the buyer
        if (notificationServer != null) {
            Optional<Order> orderOpt = orderDAO.findById(orderId);
            orderOpt.ifPresent(order ->
                    notificationServer.notifyOrderDelivered(orderId, order.getBuyerId())
            );
        }
    }


    public List<Order> getActiveOrders() throws DataAccessException {
        return orderDAO.findAllActive();
    }


    public List<Order> getAllOrders() throws DataAccessException {
        return orderDAO.findAll();
    }


    public List<Order> getOrdersForBuyer(int buyerId) throws DataAccessException {
        return orderDAO.findByBuyerId(buyerId);
    }


    public List<Order> getOrdersForFarmer(int farmerId) throws DataAccessException {
        return orderDAO.findByFarmerId(farmerId);
    }
}
