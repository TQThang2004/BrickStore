# 📑 Đánh Giá Kế Hoạch & Kiến Trúc Backend Dự Án BrickStore

> **Tài liệu tham chiếu:** [`plan.md`](file:///c:/DuAn/Web%20BrickStore%20Java/plan.md)  
> **Người đánh giá:** Senior Java Backend Architect / Tech Lead  
> **Mục tiêu:** Phân tích khách quan các ưu điểm, chỉ ra những nhược điểm/rủi ro kỹ thuật tiềm ẩn và đưa ra đề xuất cải tiến khả thi giúp dự án triển khai mượt mà, đúng tiến độ.

---

## 1. Tổng Quan Nhận Xét

Bản kế hoạch trong [`plan.md`](file:///c:/DuAn/Web%20BrickStore%20Java/plan.md) là một tài liệu kỹ thuật **rất chất lượng, thực tế và có tư duy thiết kế bài bản**. 

Kế hoạch không mắc bẫy "thời thượng hóa" (như cố chia microservices ngay từ đầu khi chưa cần thiết), đồng thời nắm bắt rất chính xác các bài toán đặc thù của thương mại điện tử (phân biệt biến thể sản phẩm, snapshot đơn hàng, tách biệt vòng đời Order/Payment, gateway pattern).

---

## 2. Ưu Điểm Nổi Bật (Strengths)

### 2.1. Quyết định kiến trúc Modular Monolith hợp lý
- **Tiết kiệm tài nguyên & chi phí**: Tránh được sự phức tạp của hệ thống phân tán (Distributed Transaction, Service Mesh, Network Overhead, hạ tầng đắt đỏ).
- **Phân chia theo Package by Feature**: Code được nhóm theo nghiệp vụ (`catalog`, `cart`, `order`, `payment`,...) thay vì nhóm kỹ thuật (`controller`, `service`, `repository`). Điều này giúp ranh giới module rõ ràng, giảm phụ thuộc chéo và tạo bàn đạp vững chắc nếu sau này cần trích xuất (extract) thành Microservice độc lập.

### 2.2. Lựa chọn PostgreSQL + pgvector sắc bén
- Thay vì dựng thêm Elasticsearch, Milvus hay Qdrant (gây tốn RAM, chi phí vận hành và bài toán đồng bộ dữ liệu giữa 2 DB), việc tận dụng **pgvector** ngay trên PostgreSQL giúp:
  - Dữ liệu sản phẩm và vector embedding nằm chung một cơ sở dữ liệu.
  - Hỗ trợ ACID transaction đầy đủ.
  - Dễ dàng thực hiện **Hybrid Search** (kết hợp SQL query lọc theo danh mục, khoảng giá + Cosine similarity vector search trong cùng 1 câu query).

### 2.3. Tư duy nghiệp vụ E-commerce chuẩn xác
- **Quản lý biến thể ở mức `ProductVariant`**: Nhận thức đúng đắn rằng LEGO và đồ trưng bày phụ thuộc rất nhiều vào biến thể (bản tiêu chuẩn, bản có hộp mica trưng bày, màu sắc, tình trạng gạch). Việc quản lý SKU, giá bán, tồn kho ở cấp Variant là bắt buộc.
- **Snapshot dữ liệu đơn hàng (`order_items`)**: Đã lưu lại tên SP, SKU, giá, thuộc tính tại thời điểm mua, ngăn chặn lỗi sai lệch báo cáo tài chính khi sản phẩm gốc đổi giá trong tương lai.
- **Tách riêng `OrderStatus` và `PaymentStatus`**: Tránh lỗi thiết kế kinh điển gom chung trạng thái, đáp ứng được các kịch bản thực tế như COD (giao xong mới thanh toán), hoặc thanh toán online thành công nhưng shop chưa chuẩn bị kịp hàng.
- **Áp dụng Pattern linh hoạt**: `PaymentGateway` interface với Factory pattern giúp bổ sung MoMo, VNPay, ZaloPay độc lập mà không can thiệp vào logic xử lý đơn hàng chính.
- **Nguyên tắc ưu tiên thực tế**: Nhấn mạnh quy tắc *"Không bắt đầu bằng AI Image Search ngay"* mà xây dựng vững chắc Core Commerce trước.

---

## 3. Nhược Điểm & Rủi Ro Kỹ Thuật Tiềm Ẩn (Weaknesses & Risks)

### 3.1. Rủi ro xử lý tranh chấp tồn kho (Inventory Concurrency & Race Condition)
- **Vấn đề**: Các mặt hàng LEGO độc lạ hoặc hàng trưng bày phiên bản giới hạn thường có số lượng rất ít (1-2 hộp) và lượng khách săn cùng lúc cao. Kế hoạch hiện tại mới chỉ ghi *"Kiểm tra và giữ tồn kho"*, chưa làm rõ cơ chế khóa.
- **Hậu quả tiềm ẩn**: Nếu 2 khách hàng cùng bấm thanh toán đồng thời (Concurrent checkout), hệ thống dùng câu lệnh `SELECT ... UPDATE` thông thường dễ dẫn đến **Overselling** (bán vượt số lượng trong kho).

### 3.2. Kiến trúc Image Embedding chưa rõ cách thực thi runtime
- **Vấn đề**: Kế hoạch đề xuất mô hình CLIP để tạo embedding vector ảnh, nhưng Spring Boot (Java) không phải môi trường lý tưởng để load các mô hình PyTorch/Transformers nặng hàng trăm MB.
- **Rủi ro**:
  - Nếu cố dùng thư viện Java (như DJL, ONNX Runtime Java) để load model AI trực tiếp trong Spring Boot: gây ngốn RAM nghiêm trọng (dễ sập JVM container), ảnh hưởng đến toàn bộ luồng xử lý Web chính.
  - Chưa quy định rõ là gọi qua 1 service Python nhỏ (FastAPI), qua Cloud Vision API (Google Vertex AI / Gemini Embeddings), hay chạy local container riêng.

### 3.3. Cơ chế giao tiếp giữa các Module dễ bị dính chặt (Coupling)
- Mặc dù chia thư mục theo module, nhưng nếu trong `OrderService` lại inject trực tiếp `InventoryService`, `PaymentService`, `EmailService`, `NotificationService`,... thì hệ thống sẽ trở thành **Spaghetti Monolith**.
- Việc thiếu cơ chế **Domain Events nội bộ** (Spring Application Events) sẽ làm việc mở rộng tính năng sau này rất cồng kềnh.

### 3.4. Vấn đề Xử lý Idempotency & Webhook Callback chưa có giải pháp chi tiết
- Cổng thanh toán (VNPay, MoMo) thường retry gửi Webhook nhiều lần (nếu server phản hồi chậm hoặc timeout mạng).
- Nếu không có cơ chế chặn trùng lặp giao dịch (Idempotency Key / Transaction Locking), đơn hàng có thể bị kích hoạt xử lý 2 lần (cộng điểm 2 lần, gửi 2 email, trừ kho 2 lần).

### 3.5. Phạm vi (Scope) quá lớn so với thời lượng 8–12 tuần của 1 developer
- Hệ thống ôm trọn: Core E-commerce + Admin Dashboard + Tích hợp 3 cổng thanh toán + Image Search (pgvector) + AI Recommendation (Collaborative Filtering + Content Based) + Review/Wishlist + Promotion + Shipping.
- **Rủi ro**: Khối lượng này thường tương đương khối lượng công việc của 1 team 2-3 người trong 3-4 tháng. Một mình làm sẽ rất dễ bị kiệt sức (burnout), dẫn đến việc cắt xén unit test, code ẩu hoặc dự án bị bỏ dở giữa chừng.

---

## 4. Các Điểm Cần Cải Thiện & Đề Xuất Giải Pháp (Actionable Recommendations)

### 4.1. Bổ sung giải pháp Khóa Tồn Kho (Concurrency Control)
* **Giải pháp khuyến nghị**:
  1. Dùng **Pessimistic Locking** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) trên bảng `product_variants` hoặc `inventory` trong transaction checkout để đảm bảo tại một thời điểm chỉ có 1 thread được trừ kho.
  2. Hoặc kết hợp **Redis Distributed Lock (Redisson)** với key `lock:variant:{id}` để tối ưu thời gian khóa ngoài database.
  3. Cơ chế **Giữ kho tạm thời (Stock Reservation)**: Khi tạo phiên thanh toán ví điện tử, giữ tồn kho trong 15 phút. Dùng Spring `@Scheduled` hoặc Redis TTL Key Expiration Event để tự động nhả kho nếu user không thanh toán.

### 4.2. Làm rõ phương án triển khai Image Embedding
* **Đề xuất 2 phương án thực tế nhất**:
  - **Phương án A (Khuyến nghị cho dự án độc lập/Production nhẹ)**: Tạo một micro-helper bằng **Python FastAPI** chạy mô hình `CLIP (clip-ViT-B-32)` nhẹ nhàng qua Docker, Spring Boot chỉ cần gọi HTTP request sang service này để lấy vector mảng số thực `float[]`, sau đó insert vào PostgreSQL.
  - **Phương án B (Nhanh nhất, không cần hạ tầng AI)**: Dùng API Embedding có sẵn qua mạng (như Google Vertex AI Multimodal Embedding hoặc Cloudflare Workers AI), trả tiền theo lượt gọi, không tốn tài nguyên server.

```
[Spring Boot Backend] ──(Upload ảnh)──> [Python FastAPI / CLIP Service]
         │                                          │
         │<────────(Trả về mảng float[512])─────────┘
         │
         ▼
[PostgreSQL + pgvector] (Lưu hoặc truy vấn cosine distance)
```

### 4.3. Giảm phụ thuộc chéo bằng Domain Events (Spring ApplicationEvent)
* Thay vì `OrderService` gọi trực tiếp tất cả service khác, chuyển sang mô hình Event-Driven nội bộ:
  ```java
  // Khi đơn hàng đặt thành công:
  eventPublisher.publishEvent(new OrderPlacedEvent(order));
  ```
* Các listener độc lập lắng nghe:
  - `InventoryEventListener` -> Cập nhật kho
  - `NotificationEventListener` -> Bắn thông báo real-time
  - `EmailEventListener` -> Gửi mail xác nhận (chạy `@Async`)
  - `CartEventListener` -> Dọn giỏ hàng

### 4.4. Cơ chế Idempotency & Bảo vệ Webhook thanh toán
* Thêm bảng `payment_transactions` có ràng buộc duy nhất (Unique Constraint) trên cặp `(provider, provider_transaction_id)`.
* Khi nhận webhook:
  1. Kiểm tra chữ ký số (Checksum HMAC-SHA256).
  2. Bắt đầu transaction -> Kiểm tra nếu transaction ID đã tồn tại với trạng thái `SUCCESS` thì return ngay mã thành công cho cổng thanh toán (không xử lý lại).
  3. Cập nhật trạng thái đơn hàng.

### 4.5. Tối ưu lại Lộ trình MVP (Phasing) thực dụng hơn
Chia lại phạm vi thành 2 giai đoạn rõ rệt:

* **MVP Core (Tuần 1 - 8): Bắt buộc để Go-Live**
  - Auth (JWT, Role).
  - Catalog + Biến thể + Quản lý hình ảnh.
  - Cart + Checkout + Giữ kho + COD + **1 Cổng thanh toán duy nhất (VNPay)**.
  - Order Management & Admin cơ bản.
  - Tìm kiếm từ khóa cơ bản (PostgreSQL Full-Text Search hoặc ILIKE).

* **Giai đoạn 2 (Tuần 9 - 12+): Nâng cấp AI & Mở rộng**
  - Tích hợp pgvector + Image Search bằng hình ảnh.
  - Tích hợp thêm MoMo / ZaloPay.
  - AI Recommendation & Lịch sử gợi ý.
  - Wishlist, Đánh giá sản phẩm (Reviews), Báo cáo doanh thu nâng cao.

---

## 5. Kết Luận

Kế hoạch của bạn đã đạt mức **8.5/10** về mặt thiết kế kiến trúc cho một hệ sinh thái Spring Boot hiện đại. Chỉ cần bổ sung chi tiết về **xử lý tranh chấp kho**, **mô hình chạy AI Embedding**, và **tinh gọn phạm vi MVP cho giai đoạn đầu**, dự án sẽ có tính khả thi cực kỳ cao và giảm thiểu tối đa các rủi ro kỹ thuật khi đi vào code thực tế.

