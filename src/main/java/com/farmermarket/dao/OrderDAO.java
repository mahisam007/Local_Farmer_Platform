package com.farmermarket.dao;

import com.farmermarket.dao.exception.DataAccessException;
import com.farmermarket.model.Order;
import com.farmermarket.model.OrderStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface OrderDAO {

    List<Order> findAllActive() throws DataAccessException;


    List<Order> findAll() throws DataAccessException;


    List<Order> findByBuyerId(int buyerId) throws DataAccessException;


    List<Order> findByFarmerId(int farmerId) throws DataAccessException;


    Optional<Order> findById(int id) throws DataAccessException;


    Order save(Order order) throws DataAccessException;


    void updateStatus(int orderId, OrderStatus status) throws DataAccessException;


    Map<String, Integer> getTotalPendingDemandByCategory() throws DataAccessException;
}
