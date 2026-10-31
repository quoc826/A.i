# Shopping Cart — Spring Boot

Triển khai bài tập Spring boot_Shopping Cart (tuần 6–10) trên Java 17, Spring Boot 4.1.2, Spring AI 2.0.1.

## Chạy nhanh, không cần database

Trong PowerShell tại thư mục project:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Mở http://localhost:8080. Profile demo dùng H2 trong RAM; dữ liệu mất khi tắt ứng dụng.

| Tài khoản demo | Mật khẩu | Quyền |
|---|---|---|
| admin | 123 | ADMIN |
| customer | 111 | CUSTOMER |

Tài khoản mẫu chỉ được tạo khi bật seed. Guest không cần đăng nhập.

## Database MariaDB / MySQL

Tạo database bằng `CREATE DATABASE shoppingdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`.
Mặc định kết nối localhost:3306/shoppingdb với tài khoản local đã cấu hình trong application.properties. Máy hiện tại dùng MySQL 8; khi chuyển sang MariaDB, đặt DB_DIALECT như ví dụ dưới. Có thể thay bằng biến môi trường:

```powershell
$env:DB_URL="jdbc:mariadb://localhost:3306/shoppingdb?allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-password"
$env:DB_DIALECT="org.hibernate.dialect.MariaDBDialect"
$env:SEED_DEMO="true"
.\mvnw.cmd spring-boot:run
```

Sau lần khởi tạo, tắt SEED_DEMO. Seed không ghi đè database đã có tài khoản. Hibernate tự tạo/cập nhật bảng, không cần chạy SQL mẫu. Không dùng profile demo với database cần lưu lâu dài.

## Chức năng theo đề

- 7 entity và repository: Customer, User, Category, Product, Comment, Order, OrderLine; quan hệ JPA theo ERD. Product thuộc Category.
- Guest xem danh sách và chi tiết sản phẩm. Đặc tả quyền Guest được ưu tiên hơn đoạn SecurityConfig mẫu mâu thuẫn trong PDF.
- Customer thêm/cập nhật/xóa sản phẩm trong giỏ, đặt hàng, xem lịch sử và chi tiết đơn của mình, viết bình luận.
- Admin tạo/sửa/khóa sản phẩm, danh mục, khách hàng và tài khoản; tạo/xem/cập nhật ngày và trạng thái/khóa đơn hàng.
- Khóa dùng active=false, giữ nguyên dữ liệu và lịch sử. Khóa đơn không hủy giao dịch hay hoàn tồn kho.
- Đặt hàng trong transaction, khóa sản phẩm theo thứ tự ID, kiểm tra tồn, trừ kho và lưu purchasePrice. Số lượng nhập sản phẩm phải >0; tồn kho sau mua được phép bằng 0.
- Validation tên Product/Category bắt đầu chữ hoa và ít nhất 2 ký tự; giá, lượng nhập >0; ngày đặt từ hôm nay; khách hàng ít nhất 2 từ, bắt đầu hoa; ngày tham gia trước hôm nay; email Gmail.
- CSRF mặc định bật; BCrypt cho mật khẩu; kiểm tra chủ sở hữu đơn ở service.
- Ảnh upload tối đa 2MB, kiểm tra và mã hóa lại PNG, lưu vào uploads/ ngoài classpath.

Các URL chính: /home, /product, /product/{id}, /cart, /order, /order/{id}, /assistant, /admin/customer, /admin/category.

## Spring AI

Chatbox dùng ChatClient, tìm kiếm ngữ nghĩa và vector store theo eShopping_research.docx. Có chat nổi trên website, lịch sử phiên, bộ lọc giá/tồn kho và tracking PAGE_VIEW/PRODUCT_VIEW/SEARCH/ADD_TO_CART. Không truyền thông tin cá nhân khách hàng vào prompt; chỉ tra cứu đơn người đăng nhập được phép xem.

```powershell
$env:OPENAI_API_KEY="your-api-key"
$env:OPENAI_MODEL="gpt-4o-mini"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=ai"
```

Khi chưa bật AI, chatbox hoạt động ở chế độ tra cứu từ khóa/danh mục và hiển thị đúng trạng thái này. Cần key và quyền truy cập model để dùng chat AI và embedding thật.
Xem [hướng dẫn Chatbox, ChromaDB và Tracking](docs/AI-CHATBOX.md). Mở /assistant để chat và /admin/research để xem thống kê.
Tham khảo: [OpenAI Chat Completions](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create).

## Kiểm thử

```powershell
.\mvnw.cmd test
```

Test dùng H2 và mô hình AI giả lập: quyền, CSRF, Thymeleaf, checkout/tồn kho, validation, chat và lịch sử phiên, tracking, đồng bộ vector và dữ liệu RAG. Kết nối database MySQL trên máy và giao diện desktop/mobile đã được kiểm tra trực tiếp; OpenAI thật và ChromaDB cần môi trường có API key/Docker.
