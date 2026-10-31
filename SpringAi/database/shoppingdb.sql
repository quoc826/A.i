-- MariaDB schema matching the application's JPA mappings.
-- No DROP/TRUNCATE: existing databases and rows are preserved.
CREATE DATABASE IF NOT EXISTS shoppingdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shoppingdb;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS categories (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customers (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(255) NOT NULL, customer_since DATE NOT NULL, email VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(255) NOT NULL, price DECIMAL(14,2) NOT NULL,
 quantity INT NOT NULL, category_id BIGINT NOT NULL, image VARCHAR(255),
 brand VARCHAR(100), description VARCHAR(4000), specifications VARCHAR(2000),
 CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
 CONSTRAINT chk_product_price CHECK (price > 0),
 CONSTRAINT chk_product_quantity CHECK (quantity >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 username VARCHAR(255) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL,
 role VARCHAR(255), customer_id BIGINT UNIQUE,
 CONSTRAINT fk_users_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 date DATE NOT NULL, status VARCHAR(255), customer_id BIGINT NOT NULL,
 CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orderlines (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 order_id BIGINT NOT NULL, product_id BIGINT NOT NULL,
 amount INT NOT NULL, purchase_price DECIMAL(38,2) NOT NULL,
 CONSTRAINT fk_orderlines_order FOREIGN KEY (order_id) REFERENCES orders(id),
 CONSTRAINT fk_orderlines_product FOREIGN KEY (product_id) REFERENCES products(id),
 CONSTRAINT chk_line_amount CHECK (amount > 0),
 CONSTRAINT chk_line_price CHECK (purchase_price > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS comments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 text VARCHAR(1000) NOT NULL, date DATE, product_id BIGINT NOT NULL, customer_id BIGINT NOT NULL,
 CONSTRAINT fk_comments_product FOREIGN KEY (product_id) REFERENCES products(id),
 CONSTRAINT fk_comments_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_embeddings (
 id VARCHAR(36) NOT NULL PRIMARY KEY, product_id BIGINT NOT NULL,
 store_key VARCHAR(200) NOT NULL, content_hash VARCHAR(64) NOT NULL, synced_at DATETIME(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tracking_events (
 id VARCHAR(36) NOT NULL PRIMARY KEY, event_type VARCHAR(20) NOT NULL,
 user_id BIGINT, anonymous_id VARCHAR(36) NOT NULL, product_id BIGINT,
 keyword VARCHAR(2000), result_count INT, quantity INT, price DECIMAL(38,2),
 path VARCHAR(500), title VARCHAR(200), client_timestamp DATETIME(6), received_at DATETIME(6) NOT NULL,
 INDEX idx_tracking_type_time (event_type,received_at), INDEX idx_tracking_user (user_id)
) ENGINE=InnoDB;

-- Run the ENTIRE script on one connection (e.g. HeidiSQL: F9).
-- Seed only if ALL seven tables are empty; no existing rows are overwritten.
START TRANSACTION;
SET @seed_shopping = (
 NOT EXISTS (SELECT 1 FROM categories)
 AND NOT EXISTS (SELECT 1 FROM customers)
 AND NOT EXISTS (SELECT 1 FROM products)
 AND NOT EXISTS (SELECT 1 FROM users)
 AND NOT EXISTS (SELECT 1 FROM orders)
 AND NOT EXISTS (SELECT 1 FROM orderlines)
 AND NOT EXISTS (SELECT 1 FROM comments)
);

INSERT INTO categories (id,name)
SELECT * FROM (
 SELECT 1 AS `id`, 'Electronics' AS `name`
 UNION ALL SELECT 2, 'Accessories'
 UNION ALL SELECT 3, 'Office'
) AS seed_rows
WHERE @seed_shopping = 1;

INSERT INTO customers (id,name,customer_since,email)
SELECT * FROM (
 SELECT 1 AS `id`, 'Nguyen An' AS `name`, CURDATE()-INTERVAL 90 DAY AS `customer_since`, 'customer@gmail.com' AS `email`
 UNION ALL SELECT 2, 'Tran Binh', CURDATE()-INTERVAL 60 DAY, 'binh@gmail.com'
 UNION ALL SELECT 3, 'Le Chi', CURDATE()-INTERVAL 30 DAY, 'chi@gmail.com'
) AS seed_rows
WHERE @seed_shopping = 1;

INSERT INTO products (id,name,price,quantity,category_id)
SELECT * FROM (
 SELECT 1 AS `id`, 'Laptop Dell Inspiron' AS `name`, 999.00 AS `price`, 18 AS `quantity`, 1 AS `category_id`
 UNION ALL SELECT 2, 'Headphones Sony', 79.00, 28, 1
 UNION ALL SELECT 3, 'Keyboard Logitech', 49.00, 39, 2
 UNION ALL SELECT 4, 'Mouse Logitech', 25.00, 49, 2
 UNION ALL SELECT 5, 'Monitor Samsung', 199.00, 15, 1
 UNION ALL SELECT 6, 'Webcam Logitech', 59.00, 20, 2
 UNION ALL SELECT 7, 'Office Chair', 129.00, 10, 3
 UNION ALL SELECT 8, 'Desk Lamp', 29.00, 25, 3
) AS seed_rows
WHERE @seed_shopping = 1;

INSERT INTO users (id,username,password,role,customer_id)
SELECT * FROM (
 SELECT 1 AS `id`, 'admin' AS `username`, '$2a$10$l7NKJJ/6DIxrfKGtMedX1u6jTe706GtQ6dfr3TpdoFH3BtTtQ//ty' AS `password`, 'ADMIN' AS `role`, NULL AS `customer_id`
 UNION ALL SELECT 2, 'customer', '$2a$10$RigCiNQXcjdV6mvyT/smI.xKJzHaiObTy8FkxSKmqjkISLkExB7ze', 'CUSTOMER', 1
 UNION ALL SELECT 3, 'binh', '$2a$10$RigCiNQXcjdV6mvyT/smI.xKJzHaiObTy8FkxSKmqjkISLkExB7ze', 'CUSTOMER', 2
 UNION ALL SELECT 4, 'chi', '$2a$10$RigCiNQXcjdV6mvyT/smI.xKJzHaiObTy8FkxSKmqjkISLkExB7ze', 'CUSTOMER', 3
) AS seed_rows
WHERE @seed_shopping = 1;

INSERT INTO orders (id,date,status,customer_id)
SELECT * FROM (
 SELECT 1 AS `id`, CURDATE() AS `date`, 'PLACED' AS `status`, 1 AS `customer_id`
 UNION ALL SELECT 2, CURDATE(), 'PROCESSING', 2
 UNION ALL SELECT 3, CURDATE(), 'COMPLETED', 3
) AS seed_rows
WHERE @seed_shopping = 1;

INSERT INTO orderlines (id,order_id,product_id,amount,purchase_price)
SELECT * FROM (
 SELECT 1 AS `id`, 1 AS `order_id`, 1 AS `product_id`, 1 AS `amount`, 999.00 AS `purchase_price`
 UNION ALL SELECT 2, 1, 3, 1, 49.00
 UNION ALL SELECT 3, 2, 2, 2, 79.00
 UNION ALL SELECT 4, 2, 4, 1, 25.00
 UNION ALL SELECT 5, 3, 1, 1, 999.00
) AS seed_rows
WHERE @seed_shopping = 1;

INSERT INTO comments (id,text,date,product_id,customer_id)
SELECT * FROM (
 SELECT 1 AS `id`, 'Laptop chạy mượt, phù hợp học lập trình.' AS `text`, CURDATE() AS `date`, 1 AS `product_id`, 1 AS `customer_id`
 UNION ALL SELECT 2, 'Tai nghe âm thanh tốt, đeo thoải mái.', CURDATE(), 2, 2
 UNION ALL SELECT 3, 'Bàn phím gõ êm, kết nối ổn định.', CURDATE(), 3, 1
 UNION ALL SELECT 4, 'Giao hàng nhanh, sản phẩm đúng mô tả.', CURDATE(), 1, 3
) AS seed_rows
WHERE @seed_shopping = 1;

COMMIT;
SELECT IF(@seed_shopping = 1, 'Demo data inserted.', 'Seed skipped: database already contains data.') AS seed_result;

SELECT 'categories' AS table_name,COUNT(*) AS row_count FROM categories
UNION ALL SELECT 'customers',COUNT(*) FROM customers
UNION ALL SELECT 'products',COUNT(*) FROM products
UNION ALL SELECT 'users',COUNT(*) FROM users
UNION ALL SELECT 'orders',COUNT(*) FROM orders
UNION ALL SELECT 'orderlines',COUNT(*) FROM orderlines
UNION ALL SELECT 'comments',COUNT(*) FROM comments;
