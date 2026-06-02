package com.farmermarket.dao;

import com.farmermarket.dao.exception.DataAccessException;
import com.farmermarket.model.Product;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductDAO {
    List<Product> findAll() throws DataAccessException;
    List<Product> findByFarmerId(int farmerId) throws DataAccessException;
    Optional<Product> findById(int id) throws DataAccessException;
    Product save(Product product) throws DataAccessException;
    void update(Product product) throws DataAccessException;
    void delete(int productId) throws DataAccessException;
    Map<String, Double> getAveragePriceByCategory() throws DataAccessException;
    Map<String, Integer> getTotalSupplyByCategory() throws DataAccessException;
}
