package com.farmermarket.dao.impl;

import com.farmermarket.dao.UserDAO;
import com.farmermarket.dao.exception.DataAccessException;
import com.farmermarket.db.DatabaseConnection;
import com.farmermarket.model.Role;
import com.farmermarket.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class MySQLUserDAO implements UserDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQLUserDAO.class);


    // UserDAO implementation


    @Override
    public Optional<User> findByEmail(String email) throws DataAccessException {
        final String sql = "SELECT id, name, email, password_hash, role FROM users WHERE email = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("findByEmail failed for email={}", email, e);
            throw new DataAccessException("Failed to find user by email: " + email, e);
        }
    }

    @Override
    public Optional<User> findById(int id) throws DataAccessException {
        final String sql = "SELECT id, name, email, password_hash, role FROM users WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("findById failed for id={}", id, e);
            throw new DataAccessException("Failed to find user by id: " + id, e);
        }
    }

    @Override
    public User save(User user) throws DataAccessException {
        final String sql =
                "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            log.info("Saved user id={}, email={}", user.getId(), user.getEmail());
            return user;
        } catch (SQLException e) {
            log.error("save failed for email={}", user.getEmail(), e);
            throw new DataAccessException("Failed to save user: " + user.getEmail(), e);
        }
    }


    // Private helpers


    /** Maps the current {@link ResultSet} row to a {@link User}. */
    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role"))
        );
    }

    /** Convenience accessor for the shared JDBC connection. */
    private Connection getConnection() throws DataAccessException {
        return DatabaseConnection.getInstance().getConnection();
    }
}
