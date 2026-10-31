# Chatbox, Semantic Search và User Tracking

Triển khai dựa trên eShopping_research.docx. Tài liệu mô tả nghiên cứu Spring AI/Embedding, Vector Database, tìm kiếm ngữ nghĩa và tracking. Chatbox tích hợp các phần này bằng luồng RAG: tìm sản phẩm liên quan trước, lấy giá/tồn kho hiện tại trong SQL, rồi cung cấp dữ liệu cho ChatClient.

## Sử dụng

- Website: http://localhost:8080
- Chat toàn trang: http://localhost:8080/assistant
- Nút “Hỏi eShopping” ở góc phải trên các trang.
- Admin: http://localhost:8080/admin/research, tài khoản demo admin/123.

Guest có thể tìm sản phẩm và chat; phải đăng nhập mới xem được đơn hàng. Lịch sử lưu trong session, tối đa 16 tin nhắn. Đăng xuất hoặc đổi danh tính làm mất lịch sử cũ; nút ↻ bắt đầu cuộc trò chuyện mới.

Bộ lọc giá (USD) và còn hàng được áp dụng bằng dữ liệu SQL, không phụ thuộc mô hình. Chat không tạo, hủy hay sửa đơn. Thẻ sản phẩm dẫn đến trang chi tiết và giỏ hàng hiện có.

## Bật OpenAI và vector store demo

Không đưa API key vào mã nguồn hoặc JavaScript. Trong PowerShell:

Ứng dụng cũng tự đọc file .env ở thư mục project (đã được Git bỏ qua). Có thể lưu OPENAI_API_KEY trong file này; đặt spring.profiles.default=ai để bật AI mặc định trên máy local. Không chia sẻ file .env hoặc đưa file này vào Git.

```powershell
$env:OPENAI_API_KEY="your-api-key"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=ai"
```

Nếu chạy IntelliJ: Run → Edit Configurations → Environment variables, thêm OPENAI_API_KEY; Active profiles đặt ai.
Tắt phiên đang chiếm port 8080 trước khi chạy phiên khác.

- Chat model mặc định: gpt-4o-mini; đổi bằng OPENAI_MODEL.
- Embedding: text-embedding-3-small, 1536 chiều, theo báo cáo.
- SimpleVectorStore dùng cosine similarity, lưu file trong data/vector-index/.
- product_embeddings lưu fingerprint, product ID và thời điểm đồng bộ.
- Chỉ nội dung ngữ nghĩa thay đổi mới cần tạo embedding lại. Giá/tồn kho luôn đọc từ SQL.
- Không có key/profile ai: tra cứu từ khóa và danh mục, UI hiển thị “Tra cứu danh mục”; đây không phải semantic search hoặc mô hình ngôn ngữ.
- Có lỗi dịch vụ AI: quay về tra cứu danh mục, giữ thao tác mua sắm hoạt động.

SimpleVectorStore phục vụ demo, không phải vector database chuyên dụng cho production.

## ChromaDB theo kiến trúc trong báo cáo

Cần Docker và OPENAI_API_KEY:

```powershell
docker compose up -d chroma
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=ai,chroma"
```

Chroma chạy localhost:8000, volume chroma-data giữ dữ liệu. Có thể thay CHROMA_URL và CHROMA_COLLECTION trong môi trường.
Profile chroma dùng ChromaVectorStore của Spring AI; sản phẩm/đơn hàng/tracking vẫn lưu tại database quan hệ.
Nếu thay embedding model/số chiều, dùng collection và thư mục index mới rồi đồng bộ lại; không truy vấn vector cũ bằng mô hình khác.
Chroma/Docker và OpenAI thật cần được kiểm tra tại môi trường có những dịch vụ này.

## Đồng bộ và dữ liệu sản phẩm

Văn bản embedding gồm tên, danh mục, thương hiệu, mô tả và thông số. Loại HTML, chuẩn hóa khoảng trắng. Document metadata có productId và contentHash.

ProductService phát sự kiện sau transaction; tác vụ async đồng bộ sau commit. Tác vụ mỗi 60 giây đối chiếu toàn bộ danh mục, bao gồm thay đổi category hoặc thay đổi ngoài ứng dụng. Admin có thể bấm đồng bộ để thử lại ngay.

Document ID ổn định, không sinh bản ghi trùng khi retry. VectorStore.add tạo embedding một lần cho mỗi Document thay đổi; không gọi embed riêng rồi add lần nữa. Mục lỗi giữ fingerprint cũ để thử lại, ghi cảnh báo và hiển thị số lỗi trên dashboard.

Sản phẩm bị khóa hoặc thuộc danh mục khóa bị loại khỏi index ở lần đồng bộ kế tiếp. Mỗi truy vấn luôn xác minh lại active, category, hash, giá và tồn kho trong SQL, nên vector cũ không đủ để đề xuất sản phẩm đã thay đổi hoặc bị khóa.

Ứng dụng dùng ddl-auto=update để thêm brand, description, specifications, product_embeddings và tracking_events. Database thực tế của máy này là MySQL 8, nên dialect mặc định là MySQLDialect; khi dùng MariaDB đặt DB_DIALECT=org.hibernate.dialect.MariaDBDialect.

File database/research-demo.sql bổ sung mô tả cho các sản phẩm mẫu ban đầu chưa có mô tả và thêm “Tai nghe thể thao Demo”. Đây là mẫu minh họa nghiên cứu, không phải thông số của một sản phẩm Sony có thật. Chạy sau khi ứng dụng đã nâng cấp schema. Không ghi đè mô tả người dùng đã nhập.

## API

| Method | URL | Mục đích |
|---|---|---|
| GET | /api/v1/chat | Lịch sử session và trạng thái AI |
| POST | /api/v1/chat | JSON: message, maxPrice (tùy chọn), inStock (tùy chọn) |
| DELETE | /api/v1/chat | Xóa lịch sử |
| GET | /api/v1/search?query=... | Tìm kiếm; thêm maxPrice và inStock |
| POST | /api/v1/tracking/events | Nhận sự kiện trình duyệt |
| GET | /admin/research | Dashboard admin |
| POST | /admin/research/reindex | Đồng bộ và thử lại các mục lỗi |

Các request thay đổi dữ liệu giữ CSRF. Frontend lấy token từ meta tag. Giới hạn theo session: 15 tin nhắn/phút, 30 tìm kiếm API/phút, 120 tracking events/phút.

## Tracking

JavaScript gửi bốn loại sự kiện trong báo cáo:

| Sự kiện | Dữ liệu |
|---|---|
| PAGE_VIEW | Path (không query string), tiêu đề |
| PRODUCT_VIEW | Product ID |
| SEARCH | Từ khóa, số kết quả |
| ADD_TO_CART | Product ID, số lượng; giá đọc lại ở server |

Server lấy user ID từ Authentication, không tin userId từ trình duyệt. Khách chưa đăng nhập được gán UUID trong session. UUID event chống ghi trùng khi gửi lại cùng sự kiện. Lưu cả timestamp client và receivedAt của server. Tracking là log hành vi do client báo cáo, không phải sổ giao dịch thanh toán.

Chỉ gửi ADD_TO_CART sau khi thêm giỏ thành công. Request tracking được giới hạn thời gian; lỗi tracking không ngăn thao tác mua hàng. Dashboard admin xem tổng theo loại và 50 sự kiện mới nhất.

## Kiểm chứng

```powershell
.\mvnw.cmd test
```

Các bài test kiểm tra: guest chat/CSRF/validation/throttle; lịch sử tách phiên và danh tính; quyền đơn hàng; tracking/dedup/giá server; bộ lọc giá; cosine retrieval, lưu/đọc vector và retry; bỏ vector đã lỗi thời; không embed lại khi chỉ đổi giá/tồn; ChatClient nhận đúng dữ liệu RAG và fallback khi mô hình lỗi.

Embedding và chat trong các bài test dùng mô hình giả lập. Kết quả đó kiểm tra tính đúng đắn của luồng phần mềm, không chứng minh chất lượng hiểu ngữ nghĩa của OpenAI. Cần chạy lại ví dụ “tai nghe để chạy bộ ngoài trời” với key thật, so sánh thứ tự sản phẩm và lưu ảnh cho báo cáo.

Đã kiểm tra trình duyệt desktop/mobile: gửi tin nhắn, reload giữ lịch sử, đăng nhập, thêm giỏ, tra cứu đơn; kiểm tra tracking đã ghi vào database thật.

## Tham khảo

- [Spring AI Chroma](https://docs.spring.io/spring-ai/reference/api/vectordbs/chroma.html)
- [Spring AI SimpleVectorStore](https://docs.spring.io/spring-ai/docs/current/api/org/springframework/ai/vectorstore/SimpleVectorStore.html)
- [OpenAI embeddings](https://developers.openai.com/api/docs/guides/embeddings)
