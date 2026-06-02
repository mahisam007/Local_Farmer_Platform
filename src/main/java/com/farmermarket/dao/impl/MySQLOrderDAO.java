package com.farmermarket.dao.impl;

import com.farmermarket.dao.OrderDAO;
import com.farmermarket.dao.exception.DataAccessException;
import com.farmermarket.db.DatabaseConnection;
import com.farmermarket.model.Order;
import com.farmermarket.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;


public class MySQLOrderDAO implements OrderDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQLOrderDAO.class);

    private static final String SELECT_COLS =
            "o.id, o.buyer_id, o.product_id, o.quantity, o.total_price, o.status, " +
                    "p.name AS product_name, u.name AS buyer_name";

    private static final String BASE_SELECT =
            "SELECT " + SELECT_COLS + " FROM orders o " +
                    "JOIN products p ON o.product_id = p.id " +
                    "JOIN users u ON o.buyer_id = u.id ";



    @Override
    public List<Order> findAllActive() throws DataAccessException {
        final String sql = BASE_SELECT +
                "WHERE o.status IN ('PENDING', 'SHIPPED') ORDER BY o.id DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return collectRows(rs);
        } catch (SQLException e) {
            log.error("findAllActive failed", e);
            throw new DataAccessException("Failed to fetch active orders", e);
        }
    }

    @Override
    public List<Order> findAll() throws DataAccessException {
        final String sql = BASE_SELECT + "ORDER BY o.id DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return collectRows(rs);
        } catch (SQLException e) {
            log.error("findAll orders failed", e);
            throw new DataAccessException("Failed to fetch all orders", e);
        }
    }

    @Override
    public List<Order> findByBuyerId(int buyerId) throws DataAccessException {
        final String sql = BASE_SELECT + "WHERE o.buyer_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                return collectRows(rs);
            }
        } catch (SQLException e) {
            log.error("findByBuyerId failed for buyerId={}", buyerId, e);
            throw new DataAccessException("Failed to fetch orders for buyer: " + buyerId, e);
        }
    }

    @Override
    public List<Order> findByFarmerId(int farmerId) throws DataAccessException {
        // Join orders → products to filter by the farmer who owns the product
        final String sql = BASE_SELECT + "WHERE p.farmer_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, farmerId);
            try (ResultSet rs = ps.executeQuery()) {
                return collectRows(rs);
            }
        } catch (SQLException e) {
            log.error("findByFarmerId failed for farmerId={}", farmerId, e);
            throw new DataAccessException("Failed to fetch orders for farmer: " + farmerId, e);
        }
    }

    @Override
    public Optional<Order> findById(int id) throws DataAccessException {
        final String sql = BASE_SELECT + "WHERE o.id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("findById failed for orderId={}", id, e);
            throw new DataAccessException("Failed to find order by id: " + id, e);
        }
    }

    @Override
    public Order save(Order order) throws DataAccessException {
        final String sql =
                "INSERT INTO orders (buyer_id, product_id, quantity, total_price, status) " +
                        "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getBuyerId());
            ps.setInt(2, order.getProductId());
            ps.setInt(3, order.getQuantity());
            ps.setDouble(4, order.getTotalPrice());
            ps.setString(5, order.getStatus().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) order.setId(keys.getInt(1));
            }
            log.info("Saved order id={}, buyerId={}", order.getId(), order.getBuyerId());
            return order;
        } catch (SQLException e) {
            log.error("save order failed for buyerId={}", order.getBuyerId(), e);
            throw new DataAccessException("Failed to save order for buyer: " + order.getBuyerId(), e);
        }
    }

    @Override
    public void updateStatus(int orderId, OrderStatus status) throws DataAccessException {
        final String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
            log.info("Updated order id={} to status={}", orderId, status);
        } catch (SQLException e) {
            log.error("updateStatus failed for orderId={}", orderId, e);
            throw new DataAccessException("Failed to update status for order: " + orderId, e);
        }
    }

    @Override
    public Map<String, Integer> getTotalPendingDemandByCategory() throws DataAccessException {
        final String sql =
                "SELECT p.category, SUM(o.quantity) AS total_demand " +
                        "FROM orders o JOIN products p ON o.product_id = p.id " +
                        "WHERE o.status = 'PENDING' " +
                        "GROUP BY p.category";
        Map<String, Integer> result = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("category"), rs.getInt("total_demand"));
            }
        } catch (SQLException e) {
            log.error("getTotalPendingDemandByCategory failed", e);
            throw new DataAccessException("Failed to compute pending demand by category", e);
        }
        return result;
    }



    private List<Order> collectRows(ResultSet rs) throws SQLException {
        List<Order> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order order = new Order(
                rs.getInt("id"),
                rs.getInt("buyer_id"),
                rs.getInt("product_id"),
                rs.getInt("quantity"),
                rs.getDouble("total_price"),
                OrderStatus.valueOf(rs.getString("status"))
        );

        order.setProductName(rs.getString("product_name"));
        order.setBuyerName(rs.getString("buyer_name"));
        return order;
    }

    private Connection getConnection() throws DataAccessException {
        return DatabaseConnection.getInstance().getConnection();
    }
}
