package com.farmermarket.db;

import com.farmermarket.dao.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    /** The single shared instance. */
    private static DatabaseConnection instance;

    /** The underlying JDBC connection. */
    private Connection connection;



    private DatabaseConnection() throws DataAccessException {
        Properties props = loadProperties();
        String url = buildUrl(props);
        try {
            log.info("Opening JDBC connection to {}", url);
            this.connection = DriverManager.getConnection(
                    url,
                    props.getProperty("db.user"),
                    props.getProperty("db.password")
            );
            log.info("JDBC connection established successfully.");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to open database connection: " + e.getMessage(), e);
        }
    }


    public static synchronized DatabaseConnection getInstance() throws DataAccessException {
        try {
            if (instance == null || instance.connection.isClosed()) {
                instance = new DatabaseConnection();
            }
        } catch (SQLException e) {
            // isClosed() itself threw — treat as closed and re-create
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }


    public static synchronized void close() {
        if (instance != null) {
            try {
                if (!instance.connection.isClosed()) {
                    instance.connection.close();
                    log.info("JDBC connection closed.");
                }
            } catch (SQLException e) {
                log.warn("Error closing JDBC connection: {}", e.getMessage());
            } finally {
                instance = null;
            }
        }
    }


    private static Properties loadProperties() throws DataAccessException {
        Properties props = new Properties();
        try (InputStream is = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is == null) {
                throw new DataAccessException(
                        "db.properties not found on classpath. " +
                                "Copy db.properties.template and fill in your credentials.");
            }
            props.load(is);
        } catch (IOException e) {
            throw new DataAccessException("Failed to load db.properties: " + e.getMessage(), e);
        }
        return props;
    }


    private static String buildUrl(Properties props) {
        return "jdbc:mysql://"
                + props.getProperty("db.host", "localhost")
                + ":"
                + props.getProperty("db.port", "3306")
                + "/"
                + props.getProperty("db.name", "farmermarket")
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }
}
