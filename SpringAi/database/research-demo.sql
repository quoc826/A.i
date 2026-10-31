-- Run after starting the updated application once (Hibernate adds the research columns/tables).
-- Supplement only the original sample products whose descriptions are still empty.
USE shoppingdb;
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE products SET brand='Dell', description='Laptop phục vụ học tập, lập trình và làm việc văn phòng.', specifications='Xem thông tin cấu hình cụ thể tại cửa hàng.', version=version+1
WHERE name='Laptop Dell Inspiron' AND (description IS NULL OR description='');
UPDATE products SET brand='Sony', description='Tai nghe phục vụ nghe nhạc, học trực tuyến và làm việc.', specifications='Vui lòng xác nhận model và thông số kết nối trước khi mua.', version=version+1
WHERE name='Headphones Sony' AND (description IS NULL OR description='');
UPDATE products SET brand='Logitech', description='Bàn phím dùng cho máy tính, học tập và làm việc văn phòng.', specifications='Xem loại kết nối và bố cục tại cửa hàng.', version=version+1
WHERE name='Keyboard Logitech' AND (description IS NULL OR description='');
UPDATE products SET brand='Logitech', description='Chuột máy tính dành cho học tập và thao tác văn phòng.', specifications='Xem thông số kết nối tại cửa hàng.', version=version+1
WHERE name='Mouse Logitech' AND (description IS NULL OR description='');
UPDATE products SET brand='Samsung', description='Màn hình máy tính dành cho không gian học tập và làm việc.', version=version+1
WHERE name='Monitor Samsung' AND (description IS NULL OR description='');
UPDATE products SET brand='Logitech', description='Webcam cho họp video và học trực tuyến trên máy tính.', version=version+1
WHERE name='Webcam Logitech' AND (description IS NULL OR description='');
UPDATE products SET description='Ghế văn phòng cho góc học tập và bàn làm việc.', version=version+1
WHERE name='Office Chair' AND (description IS NULL OR description='');
UPDATE products SET description='Đèn bàn cho góc học tập, đọc sách và làm việc.', version=version+1
WHERE name='Desk Lamp' AND (description IS NULL OR description='');
INSERT INTO products (name,brand,description,specifications,price,quantity,category_id,active,version)
SELECT 'Tai nghe thể thao Demo','Demo',
 'Dữ liệu minh họa: tai nghe không dây dành cho chạy bộ, tập thể thao ngoài trời.',
 'Mẫu giả lập phục vụ nghiên cứu: Bluetooth, chống nước, móc tai.',
 39.00,15,c.id,TRUE,0
FROM categories c WHERE c.name='Electronics' AND c.active=TRUE
AND NOT EXISTS (SELECT 1 FROM products WHERE name='Tai nghe thể thao Demo')
ORDER BY c.id LIMIT 1;
COMMIT;
