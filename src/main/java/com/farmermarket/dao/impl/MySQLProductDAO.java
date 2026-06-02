package com.farmermarket.dao.impl;

import com.farmermarket.dao.ProductDAO;
import com.farmermarket.dao.exception.DataAccessException;
import com.farmermarket.db.DatabaseConnection;
import com.farmermarket.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * MySQL implementation.
 */
public class MySQLProductDAO implements ProductDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQLProductDAO.class);

    private static final String SELECT_COLS =
            "id, farmer_id, name, category, quantity, price_per_unit";

    // ProductDAO implementation


    @Override
    public List<Product> findAll() throws DataAccessException {
        final String sql = "SELECT " + SELECT_COLS + " FROM products";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return collectRows(rs);
        } catch (SQLException e) {
            log.error("findAll products failed", e);
            throw new DataAccessException("Failed to fetch all products", e);
        }
    }

    @Override
    public List<Product> findByFarmerId(int farmerId) throws DataAccessException {
        final String sql = "SELECT " + SELECT_COLS + " FROM products WHERE farmer_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, farmerId);
            try (ResultSet rs = ps.executeQuery()) {
                return collectRows(rs);
            }
        } catch (SQLException e) {
            log.error("findByFarmerId failed for farmerId={}", farmerId, e);
            throw new DataAccessException("Failed to fetch products for farmer: " + farmerId, e);
        }
    }

    @Override
    public Optional<Product> findById(int id) throws DataAccessException {
        final String sql = "SELECT " + SELECT_COLS + " FROM products WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("findById failed for productId={}", id, e);
            throw new DataAccessException("Failed to find product by id: " + id, e);
        }
    }

    @Override
    public Product save(Product product) throws DataAccessException {
        final String sql =
                "INSERT INTO products (farmer_id, name, category, quantity, price_per_unit) " +
                        "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, product.getFarmerId());
            ps.setString(2, product.getName());
            ps.setString(3, product.getCategory());
            ps.setInt(4, product.getQuantity());
            ps.setDouble(5, product.getPricePerUnit());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) product.setId(keys.getInt(1));
            }
            log.info("Saved product id={}, name={}", product.getId(), product.getName());
            return product;
        } catch (SQLException e) {
            log.error("save product failed for name={}", product.getName(), e);
            throw new DataAccessException("Failed to save product: " + product.getName(), e);
        }
    }

    @Override
    public void update(Product product) throws DataAccessException {
        final String sql =
                "UPDATE products SET name=?, category=?, quantity=?, price_per_unit=? WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setInt(3, product.getQuantity());
            ps.setDouble(4, product.getPricePerUnit());
            ps.setInt(5, product.getId());
            ps.executeUpdate();
            log.info("Updated product id={}", product.getId());
        } catch (SQLException e) {
            log.error("update product failed for id={}", product.getId(), e);
            throw new DataAccessException("Failed to update product id: " + product.getId(), e);
        }
    }

    @Override
    public void delete(int productId) throws DataAccessException {
        final String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
            log.info("Deleted product id={}", productId);
        } catch (SQLException e) {
            log.error("delete product failed for id={}", productId, e);
            throw new DataAccessException("Failed to delete product id: " + productId, e);
        }
    }

    @Override
    public Map<String, Double> getAveragePriceByCategory() throws DataAccessException {
        final String sql =
                "SELECT category, AVG(price_per_unit) AS avg_price FROM products GROUP BY category";
        Map<String, Double> result = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("category"), rs.getDouble("avg_price"));
            }
        } catch (SQLException e) {
            log.error("getAveragePriceByCategory failed", e);
            throw new DataAccessException("Failed to compute average prices by category", e);
        }
        return result;
    }

    @Override
    public Map<String, Integer> getTotalSupplyByCategory() throws DataAccessException {
        final String sql =
                "SELECT category, SUM(quantity) AS total_supply FROM products GROUP BY category";
        Map<String, Integer> result = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("category"), rs.getInt("total_supply"));
            }
        } catch (SQLException e) {
            log.error("getTotalSupplyByCategory failed", e);
            throw new DataAccessException("Failed to compute total supply by category", e);
        }
        return result;
    }


    // Private helpers

    private List<Product> collectRows(ResultSet rs) throws SQLException {
        List<Product> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getInt("farmer_id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getInt("quantity"),
                rs.getDouble("price_per_unit")
        );
    }

    private Connection getConnection() throws DataAccessException {
        return DatabaseConnection.getInstance().getConnection();
    }
}
