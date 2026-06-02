package com.farmermarket.dao;

import com.farmermarket.dao.exception.DataAccessException;
import com.farmermarket.model.User;

import java.util.Optional;

public interface UserDAO {

    Optional<User> findByEmail(String email) throws DataAccessException;
    Optional<User> findById(int id) throws DataAccessException;
    User save(User user) throws DataAccessException;
}
