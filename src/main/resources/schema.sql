

CREATE DATABASE IF NOT EXISTS farmermarket
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE farmermarket;


CREATE TABLE IF NOT EXISTS users (
                                     id            INT          NOT NULL AUTO_INCREMENT,
                                     name          VARCHAR(120) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('FARMER','BUYER','DELIVERY_PERSON') NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


ALTER TABLE users
    MODIFY COLUMN role ENUM('FARMER','BUYER','DELIVERY_PERSON') NOT NULL;


CREATE TABLE IF NOT EXISTS products (
                                        id             INT            NOT NULL AUTO_INCREMENT,
                                        farmer_id      INT            NOT NULL,
                                        name           VARCHAR(200)   NOT NULL,
    category       VARCHAR(100)   NOT NULL,
    quantity       INT            NOT NULL DEFAULT 0,
    price_per_unit DECIMAL(10,2)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_products_farmer
    FOREIGN KEY (farmer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_products_quantity  CHECK (quantity >= 0),
    CONSTRAINT chk_products_price     CHECK (price_per_unit > 0)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS orders (
                                      id          INT            NOT NULL AUTO_INCREMENT,
                                      buyer_id    INT            NOT NULL,
                                      product_id  INT            NOT NULL,
                                      quantity    INT            NOT NULL,
                                      total_price DECIMAL(10,2)  NOT NULL,
    status      ENUM('PENDING','SHIPPED','DELIVERED') NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_buyer
    FOREIGN KEY (buyer_id)   REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_orders_product
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT chk_orders_quantity CHECK (quantity > 0)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
