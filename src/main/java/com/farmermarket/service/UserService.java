package com.farmermarket.service;

import com.farmermarket.dao.UserDAO;
import com.farmermarket.dao.exception.AuthenticationException;
import com.farmermarket.dao.exception.DataAccessException;
import com.farmermarket.dao.exception.ValidationException;
import com.farmermarket.model.Role;
import com.farmermarket.model.User;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/*
  Business logic for user registration and authentication.
 */
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserDAO userDAO;


    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }



    public User register(String name, String email, String rawPassword, Role role)
            throws ValidationException, DataAccessException {


        if (isBlank(name)) {
            throw new ValidationException("Name is required.");
        }
        if (isBlank(email)) {
            throw new ValidationException("Email is required.");
        }
        if (isBlank(rawPassword)) {
            throw new ValidationException("Password is required.");
        }
        if (role == null) {
            throw new ValidationException("Role must be selected.");
        }


        Optional<User> existing = userDAO.findByEmail(email.trim());
        if (existing.isPresent()) {
            throw new ValidationException("An account with this email already exists.");
        }


        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        User user = new User(0, name.trim(), email.trim(), hash, role);
        User saved = userDAO.save(user);
        log.info("Registered new user id={}, role={}", saved.getId(), saved.getRole());
        return saved;
    }


    public User authenticate(String email, String rawPassword)
            throws AuthenticationException, DataAccessException {

        if (isBlank(email) || isBlank(rawPassword)) {
            throw new AuthenticationException("Email and password are required.");
        }

        Optional<User> userOpt = userDAO.findByEmail(email.trim());
        if (userOpt.isEmpty()) {

            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();
        if (!BCrypt.checkpw(rawPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        log.info("User authenticated: id={}, role={}", user.getId(), user.getRole());
        return user;
    }



    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
