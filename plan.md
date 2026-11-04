# BrickStore — Kế hoạch và Kiến trúc Backend (Bản Hoàn Thiện)

> **Dự án:** Website thương mại điện tử chuyên đồ chơi LEGO & mô hình trưng bày  
> **Ngôn ngữ & Framework:** Java 21 / Spring Boot 3.x  
> **Phiên bản:** 2.0 (Đã nâng cấp và khắc phục rủi ro kiến trúc theo tài liệu đánh giá)  
> **Cập nhật:** 04/11/2026  

---

## 1. Tổng quan dự án

**BrickStore** là nền tảng thương mại điện tử chuyên biệt cho các dòng sản phẩm LEGO, mô hình sưu tầm và phụ kiện trưng bày (hộp mica, đèn led, diorama). Hệ thống được thiết kế hướng tới tính ổn định cao, quản lý biến thể sản phẩm chặt chẽ, chống bán vượt tồn kho (overselling) và tích hợp tìm kiếm thông minh bằng hình ảnh qua vector embedding.

### Kiến trúc đề xuất

- **Modular Monolith kết hợp Clean Architecture & Domain-Driven Design (DDD) thực dụng**: Tổ chức mã nguồn theo module nghiệp vụ độc lập (**Package by Feature**), phân lớp rõ ràng (`controller`, `dto`, `service`, `entity`, `repository`, `event`).
- **Giao tiếp liên module lỏng lẻo (Loose Coupling)**: Áp dụng cơ chế **Internal Domain Events** (Spring `ApplicationEventPublisher`) để loại bỏ việc các Service gọi chéo nhau trực tiếp (chống biến tướng thành Spaghetti Monolith).
- **PostgreSQL + pgvector**: Tận dụng cơ sở dữ liệu quan hệ cho toàn bộ nghiệp vụ ACID kết hợp mở rộng vector search ngay trong cùng DB, không phân mảnh hạ tầng.
- **Micro-helper cho AI Image Embedding**: Tách riêng tác vụ trích xuất vector ảnh (CLIP model) sang một service Python FastAPI nhỏ nhẹ, giữ cho ứng dụng Spring Boot chạy nhẹ, ổn định và an toàn về bộ nhớ JVM.

---

## 2. Công nghệ đề xuất

| Thành phần | Công nghệ lựa chọn | Mục đích & Lý do |
|---|---|---|
| **Programming Language** | Java 21 (LTS) | Hỗ trợ Virtual Threads, Pattern Matching, Record patterns |
| **Backend Framework** | Spring Boot 3.3+ | Nền tảng micro-framework mạnh mẽ, bảo mật cao |
| **Build Tool** | Maven | Quản lý dependencies và build lifecycle |
| **Database** | PostgreSQL 16+ | RDBMS mạnh mẽ, hỗ trợ JSONB và extension đa dạng |
| **Vector Search** | Extension `pgvector` | Tìm kiếm vector trực tiếp trong PostgreSQL (không cần dựng Elasticsearch/Milvus) |
| **AI Helper Service** | Python (FastAPI + OpenCLIP) | Service độc lập chuyên vector hóa hình ảnh (clip-ViT-B-32) |
| **ORM & Data Access** | Spring Data JPA / Hibernate 6 | Thao tác DB, kết hợp JPA Specification cho dynamic query |
| **Concurrency & Lock** | Pessimistic Lock & Redis (Redisson) | Chống race condition khi trừ tồn kho và giữ hàng tạm |
| **Cache & Session** | Redis 7+ | Caching dữ liệu catalog, lưu session giỏ hàng guest |
| **Database Migration** | Flyway | Quản lý version schema DB nhất quán |
| **Authentication & AuthZ**| Spring Security 6 + JWT | Stateless Authentication, phân quyền RBAC |
| **Object Storage** | Cloudinary / MinIO | Lưu trữ ảnh sản phẩm, ảnh review |
| **Payment Gateways** | VNPay (MVP) → MoMo, ZaloPay | Cổng thanh toán trực tuyến đa kênh |
| **Object Mapping** | MapStruct | Ánh xạ DTO ↔ Entity hiệu năng cao tại compile-time |
| **API Documentation** | Springdoc OpenAPI (Swagger 3) | Tài liệu hóa REST API tự động |
| **Containerization** | Docker, Docker Compose | Đóng gói môi trường đồng nhất (Postgres, Redis, Python AI, Spring App) |

---

## 3. Phân chia chức năng hệ thống

### 3.1. Customer — Chức năng người dùng

#### A. Product & Catalog
- Xem danh sách sản phẩm (phân trang, lọc theo danh mục, thương hiệu, độ tuổi, số mảnh ghép, mức giá).
- Xem chi tiết sản phẩm và các biến thể (Edition, Hộp trưng bày, Màu sắc, Tình trạng hộp).
- Xem các sản phẩm liên quan và sản phẩm cùng bộ sưu tập.

#### B. Shopping Cart & Checkout
- Giỏ hàng linh hoạt (hỗ trợ cả khách vãng lai qua Redis session và khách đã đăng nhập qua DB).
- Tự động gộp giỏ hàng (merge cart) khi khách vãng lai đăng nhập.
- Giữ tồn kho tạm thời (Stock Reservation) khi bước vào tiến trình thanh toán (thời hạn 15 phút).
- Áp dụng mã giảm giá (coupon), tính phí vận chuyển và kiểm tra tính hợp lệ trước khi đặt hàng.

#### C. Payment & Order
- Thanh toán khi nhận hàng (COD).
- Thanh toán trực tuyến qua cổng thanh toán (VNPay trước, sau đó mở rộng MoMo/ZaloPay).
- Xem chi tiết lịch sử đơn hàng, dòng thời gian trạng thái giao hàng.
- Mua lại đơn hàng cũ (Reorder - tự động kiểm tra lại giá và tồn kho hiện tại).
- Hủy đơn hàng tự động khi hết hạn giữ chỗ hoặc người dùng chủ động hủy (kèm hoàn tồn kho).

#### D. Search & AI
- Tìm kiếm từ khóa toàn văn (Full-Text Search tiếng Việt trên PostgreSQL).
- Tìm kiếm sản phẩm bằng tải ảnh lên (Image Similarity Search qua vector pgvector).
- Gợi ý từ khóa tìm kiếm (Autocomplete).
- Tìm kiếm kết hợp (Hybrid Search: Lọc theo danh mục/giá + Xếp hạng theo độ tương đồng ảnh).

#### E. User Account
- Đăng ký, đăng nhập JWT, refresh token, đổi mật khẩu.
- Quản lý sổ địa chỉ giao hàng (chọn địa chỉ mặc định).
- Danh sách yêu thích (Wishlist).
- Đánh giá sản phẩm đã mua (Verified Purchase Reviews).

### 3.2. Admin — Chức năng quản trị

| Nhóm | Chức năng chi tiết |
|---|---|
| **Dashboard** | Doanh thu ngày/tuần/tháng, số đơn mới, tỉ lệ thanh toán thành công, top SP bán chạy |
| **Product Management** | CRUD sản phẩm, thông tin SEO slug, mô tả, ảnh đại diện, danh mục, thương hiệu |
| **Variant Management** | Quản lý SKU, giá bán, giá so sánh, thuộc tính (JSONB), ảnh riêng từng biến thể |
| **Inventory Management** | Quản lý tồn kho theo từng biến thể SKU, theo dõi lịch sử nhập/xuất kho |
| **Order Management** | Xem danh sách đơn, lọc theo trạng thái, duyệt đơn, in vận đơn, hủy đơn |
| **Payment Management** | Đối soát giao dịch, theo dõi log webhook, kiểm tra trạng thái thanh toán lỗi |
| **Customer Management**| Quản lý danh sách tài khoản, khóa/mở tài khoản người dùng |
| **Promotion Management**| Tạo mã coupon (% hoặc tiền cố định, giới hạn lượt dùng, hạn sử dụng) |
| **Review Management** | Kiểm duyệt nội dung đánh giá và hình ảnh feedback của khách |
| **AI Index Management** | Trigger re-index embedding ảnh khi upload/cập nhật ảnh sản phẩm |

---

## 4. Kiến trúc Backend & Giải pháp Kỹ thuật Trọng tâm

### 4.1. Sơ đồ kiến trúc tổng thể

```text
                           CLIENT APPLICATIONS
                     ┌───────────────────────────────┐
                     │  Web Client / Admin Dashboard │
                     └───────────────┬───────────────┘
                                     │  HTTPS / REST API
                                     ▼
                     ┌───────────────────────────────┐
                     │     Spring Boot Application   │
                     │       (Modular Monolith)      │
                     │                               │
                     │  ┌─────────────────────────┐  │
                     │  │    Security & Filters   │  │
                     │  │    (JWT, CORS, Idemp)   │  │
                     │  └────────────┬────────────┘  │
                     │               ▼               │
                     │  ┌─────────────────────────┐  │
                     │  │       REST API DTOs     │  │
                     │  └────────────┬────────────┘  │
                     │               ▼               │
                     │  ┌─────────────────────────┐  │
                     │  │    Application Services │  │
                     │  │   (Spring Event Bus)    │  │
                     │  └──────┬───────────┬──────┘  │
                     │         │           │         │
                     │         ▼           ▼         │
                     │    JPA Domain    Redisson     │
                     └─────────┼───────────┼─────────┘
                               │           │
       ┌───────────────────────┼───────────┴──────────┐
       │                       │                      │
       ▼                       ▼                      ▼
┌──────────────┐        ┌──────────────┐       ┌──────────────┐
│  PostgreSQL  │        │    Redis     │       │Python FastAPI│
│  + pgvector  │        │ (Lock/Cache) │       │ (CLIP Model) │
└──────────────┘        └──────────────┘       └──────────────┘
```

### 4.2. Giải pháp 1: Cơ chế chống bán vượt kho (Inventory Concurrency Control)

Đối với các bộ LEGO giá trị cao và số lượng có hạn, hệ thống áp dụng chiến lược đa tầng:

```text
[Yêu cầu Checkout]
       │
       ▼
[Redis Distributed Lock] (lock:variant:{id}) ──(Ngăn race condition tức thời)
       │
       ▼
[Database Transaction] ──(Dùng Pessimistic Write Lock: SELECT ... FOR UPDATE)
       │
       ├── Nếu Đủ Hàng:
       │    ├─ Trừ tồn kho khả dụng (available_stock -= qty)
       │    ├─ Tăng tồn kho giữ chỗ (reserved_stock += qty)
       │    └─ Tạo bản ghi `stock_reservations` (hết hạn sau 15 phút)
       │
       └── Nếu Thiếu Hàng:
            └─ Throw InsufficientStockException (Báo người dùng)
```

- **Cơ chế nhả kho tự động (Auto-release Reservation)**:
  - Một scheduled job (`@Scheduled(cron = "0 */1 * * * *")`) quét các bản ghi giữ chỗ quá 15 phút mà đơn hàng vẫn ở trạng thái `PENDING_PAYMENT`.
  - Tự động hoàn lại số lượng tồn kho khả dụng và đánh dấu đơn hàng `EXPIRED_CANCELLED`.

### 4.3. Giải pháp 2: Kiến trúc trích xuất vector cho AI Image Search

Thay vì nhúng trực tiếp mô hình AI nặng vào Spring Boot JVM, hệ thống tách riêng:

1. **Service độc lập (`brickstore-ai-service`)**:
   - Viết bằng Python FastAPI, tải mô hình `open_clip:ViT-B-32`.
   - Cung cấp endpoint: `POST /api/v1/embeddings/image` (nhận binary file ảnh hoặc image URL, trả về vector `float[512]`).
   - Đóng gói trong container Docker riêng với cấu hình memory giới hạn rõ ràng.
2. **Tích hợp phía Spring Boot (`ImageSearchService`)**:
   - Gọi REST sang Python service thông qua Spring `RestClient` hoặc `WebClient`.
   - Lưu trữ vector 512 chiều vào cột kiểu `vector(512)` của bảng `product_embeddings` trên PostgreSQL.
   - Truy vấn tương đồng bằng toán tử cosine `<=>` của pgvector:
     ```sql
     SELECT product_id, (embedding <=> :queryVector) AS distance
     FROM product_embeddings
     ORDER BY distance ASC
     LIMIT 20;
     ```

### 4.4. Giải pháp 3: Giao tiếp lỏng lẻo bằng Domain Events (Spring Application Events)

Để tránh việc `OrderService` bị phụ thuộc cứng (tight coupling) vào các service khác, áp dụng mô hình Event Bus nội bộ:

```java
// Khi OrderService tạo đơn thành công, chỉ phát sinh Event:
eventPublisher.publishEvent(new OrderPlacedEvent(order));
```

Các module khác lắng nghe thông qua `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` và `@Async`:
- **`InventoryEventListener`**: Chuyển trạng thái giữ chỗ sang xuất kho chính thức.
- **`CartEventListener`**: Xóa các item đã mua khỏi giỏ hàng.
- **`NotificationEventListener`**: Tạo thông báo cho người dùng trên web.
- **`EmailEventListener`**: Gửi email hóa đơn xác nhận (chạy ngầm không chặn luồng trả lời HTTP).

### 4.5. Giải pháp 4: Chống xử lý trùng lặp Webhook (Payment Idempotency)

Các cổng thanh toán như VNPay, MoMo thường retry webhook nhiều lần khi mạng chậm. Để đảm bảo an toàn tuyệt đối:

1. **Ràng buộc cơ sở dữ liệu**: Bảng `payment_transactions` có Unique Index trên cặp `(provider, provider_transaction_id)`.
2. **Quy trình xử lý Idempotent Webhook**:
   - **Bước 1**: Verify chữ ký số (Checksum HMAC-SHA512/SHA256). Nếu sai -> trả về lỗi ngay lập tức.
   - **Bước 2**: Mở transaction -> Tìm giao dịch theo `provider_transaction_id`:
     - Nếu đã tồn tại và đã `SUCCESS`: Return `200 OK` (hoặc mã thành công tương ứng của cổng) ngay lập tức, không xử lý lại.
     - Nếu chưa tồn tại: Ghi nhận giao dịch mới, cập nhật `PaymentStatus = PAID`, kích hoạt `PaymentCompletedEvent`.

---

## 5. Cấu trúc thư mục Backend

Dự án áp dụng kiến trúc **Modular Monolith** với tổ chức **Package by Feature**, kèm thư mục service AI bên cạnh.

```text
brickstore/
├── brickstore-backend/                  # Ứng dụng Spring Boot chính
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/brickstore/
│   │   │   │   ├── BrickstoreApplication.java
│   │   │   │   │
│   │   │   │   ├── common/              # Thành phần dùng chung toàn hệ thống
│   │   │   │   │   ├── constant/        # AppConstants, SecurityConstants
│   │   │   │   │   ├── exception/       # GlobalExceptionHandler, BusinessException
│   │   │   │   │   ├── response/        # ApiResponse, PageResponse
│   │   │   │   │   ├── util/            # DateTimeUtils, SecurityUtils
│   │   │   │   │   └── enums/           # Common enums
│   │   │   │   │
│   │   │   │   ├── config/              # Cấu hình Spring Beans, JPA, Redis, OpenAPI
│   │   │   │   │   ├── ApplicationConfig.java
│   │   │   │   │   ├── JpaAuditingConfig.java
│   │   │   │   │   ├── RedisConfig.java
│   │   │   │   │   └── OpenApiConfig.java
│   │   │   │   │
│   │   │   │   ├── security/            # Xác thực & Phân quyền
│   │   │   │   │   ├── jwt/             # JwtTokenProvider, JwtAuthFilter
│   │   │   │   │   ├── CustomUserDetails.java
│   │   │   │   │   └── SecurityConfig.java
│   │   │   │   │
│   │   │   │   ├── user/                # Module Quản lý User, Địa chỉ
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── dto/
│   │   │   │   │   ├── entity/          # User, Role, Address
│   │   │   │   │   ├── repository/
│   │   │   │   │   └── service/
│   │   │   │   │
│   │   │   │   ├── catalog/             # Module Sản phẩm, Danh mục, Biến thể
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── dto/
│   │   │   │   │   ├── entity/          # Product, ProductVariant, Category, Brand
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── mapper/
│   │   │   │   │   └── specification/   # ProductSpecification (Dynamic filters)
│   │   │   │   │
│   │   │   │   ├── inventory/           # Module Quản lý Kho & Giữ hàng
│   │   │   │   │   ├── entity/          # Inventory, StockReservation
│   │   │   │   │   ├── repository/      # Lock queries (@Lock PESSIMISTIC_WRITE)
│   │   │   │   │   ├── service/         # InventoryService, StockReservationService
│   │   │   │   │   ├── listener/        # InventoryEventListener
│   │   │   │   │   └── scheduler/       # StockReleaseScheduler (quét đơn hết hạn)
│   │   │   │   │
│   │   │   │   ├── cart/                # Module Giỏ hàng
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── dto/
│   │   │   │   │   ├── entity/          # Cart, CartItem
│   │   │   │   │   ├── service/         # Hỗ trợ merge cart và redis guest cart
│   │   │   │   │   └── listener/        # CartEventListener
│   │   │   │   │
│   │   │   │   ├── order/               # Module Đơn hàng
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── dto/
│   │   │   │   │   ├── entity/          # Order, OrderItem, OrderStatusHistory
│   │   │   │   │   ├── event/           # OrderPlacedEvent, OrderCancelledEvent
│   │   │   │   │   ├── repository/
│   │   │   │   │   └── service/
│   │   │   │   │
│   │   │   │   ├── payment/             # Module Thanh toán
│   │   │   │   │   ├── controller/      # WebhookController, PaymentController
│   │   │   │   │   ├── gateway/         # PaymentGateway (Interface), VNPayGateway,...
│   │   │   │   │   ├── entity/          # Payment, PaymentTransaction
│   │   │   │   │   ├── event/           # PaymentCompletedEvent
│   │   │   │   │   └── service/         # Idempotent webhook processing
│   │   │   │   │
│   │   │   │   ├── search/              # Module Tìm kiếm (Text + Image)
│   │   │   │   │   ├── client/          # RestClient gọi sang Python AI Service
│   │   │   │   │   ├── controller/      # SearchController
│   │   │   │   │   ├── dto/
│   │   │   │   │   ├── entity/          # ProductEmbedding
│   │   │   │   │   ├── repository/      # PgVectorSearchRepository
│   │   │   │   │   └── service/         # HybridSearchService
│   │   │   │   │
│   │   │   │   ├── promotion/           # Module Mã giảm giá (Coupon)
│   │   │   │   ├── review/              # Module Đánh giá (Review)
│   │   │   │   └── notification/        # Module Email & Notification
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       └── db/migration/        # Flyway schema versioning
│   │   │           ├── V1__init_security_and_users.sql
│   │   │           ├── V2__init_catalog_and_variants.sql
│   │   │           ├── V3__init_inventory_and_reservations.sql
│   │   │           ├── V4__init_cart_and_orders.sql
│   │   │           ├── V5__init_payments_and_transactions.sql
│   │   │           └── V6__init_pgvector_and_embeddings.sql
│   │   │
│   │   └── test/java/com/brickstore/
│   │
│   ├── Dockerfile
│   └── pom.xml
│
├── brickstore-ai-service/               # Python Micro-helper cho CLIP Image Embedding
│   ├── app/
│   │   ├── main.py                      # FastAPI App
│   │   ├── model.py                     # Load OpenCLIP model ViT-B-32
│   │   └── schemas.py
│   ├── Dockerfile
│   └── requirements.txt
│
├── docker-compose.yml                   # Khởi chạy PostgreSQL, Redis, AI Service, Spring App
├── .env.example
├── README.md
└── plan.md
```

---

## 6. Thiết kế Cơ sở dữ liệu (PostgreSQL + pgvector)

### 6.1. Bật Extension & Thiết kế bảng Vector

```sql
-- Migration V6: Kích hoạt extension pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Bảng lưu trữ vector embedding ảnh sản phẩm
CREATE TABLE product_embeddings (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    image_url TEXT NOT NULL,
    embedding vector(512) NOT NULL, -- 512 dimensions của mô hình CLIP ViT-B-32
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tạo Index HNSW để tăng tốc độ tìm kiếm vector gần đúng (ANN)
CREATE INDEX idx_product_embeddings_hnsw 
ON product_embeddings USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
```

### 6.2. Các bảng quản lý Tồn kho & Tranh chấp

```sql
-- Quản lý số lượng tồn theo Variant
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    variant_id BIGINT NOT NULL UNIQUE REFERENCES product_variants(id) ON DELETE CASCADE,
    available_stock INT NOT NULL CHECK (available_stock >= 0),
    reserved_stock INT NOT NULL DEFAULT 0 CHECK (reserved_stock >= 0),
    version BIGINT NOT NULL DEFAULT 0, -- Dùng cho Optimistic Locking nếu cần
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Lưu vết giữ hàng tạm khi khách tiến hành thanh toán
CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(30) NOT NULL, -- RESERVED, COMMITTED, RELEASED
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_reservations_status_expires ON stock_reservations (status, expires_at);
```

### 6.3. Bảng Giao dịch & Bảo mật Webhook Idempotency

```sql
CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    provider VARCHAR(50) NOT NULL, -- VNPAY, MOMO, ZALOPAY
    provider_transaction_id VARCHAR(100) NOT NULL, -- Transaction code từ cổng
    amount NUMERIC(15, 2) NOT NULL,
    status VARCHAR(30) NOT NULL, -- SUCCESS, FAILED
    raw_payload JSONB, -- Lưu trữ log nguyên bản phục vụ đối soát
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_transaction UNIQUE (provider, provider_transaction_id)
);
```

### 6.4. Snapshot dữ liệu trong `order_items`

```sql
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    product_name VARCHAR(255) NOT NULL,     -- Snapshot tên sản phẩm lúc mua
    variant_name VARCHAR(255) NOT NULL,     -- Snapshot tên biến thể lúc mua
    sku VARCHAR(100) NOT NULL,              -- Snapshot SKU
    price NUMERIC(15, 2) NOT NULL,          -- Snapshot giá tại thời điểm chốt đơn
    quantity INT NOT NULL,
    image_url TEXT                          -- Snapshot ảnh sản phẩm
);
```

---

## 7. Quy trình mua hàng & Thanh toán chi tiết

### 7.1. Sơ đồ luồng Checkout và Giữ hàng

```text
[Khách hàng]
     │
     ▼
[Bấm Đặt hàng / Thanh toán]
     │
     ▼
[Spring Boot: OrderService.createOrder()]
     │
     ├── 1. Kiểm tra giỏ hàng hợp lệ & tính toán lại giá
     ├── 2. Bắt đầu Transaction:
     │      ├─ Lấy Lock và gọi InventoryService.reserveStock()
     │      ├─ Tạo Order (Status = PENDING_PAYMENT)
     │      └─ Tạo StockReservation (15 phút)
     ├── 3. Tạo Payment (Status = PENDING)
     │
     ├── NẾU CHỌN COD:
     │    ├─ Commit Reservation thành COMMITTED
     │    ├─ Cập nhật Order = CONFIRMED
     │    └─ Bắn OrderPlacedEvent -> Gửi email xác nhận
     │
     └── NẾU CHỌN VÍ ĐIỆN TỬ (VNPAY):
          ├─ Tạo VNPay Payment URL
          └─ Trả URL về cho Frontend redirect
                 │
                 ▼
          [Khách thanh toán trên cổng VNPay]
                 │
                 ▼
          [VNPay gọi Webhook Callback]
                 │
                 ▼
          [PaymentWebhookController]
                 ├─ Verify Checksum (HMAC-SHA512)
                 ├─ Kiểm tra Idempotency Key (uk_provider_transaction)
                 ├─ Cập nhật Payment = PAID
                 ├─ Cập nhật Order = CONFIRMED
                 ├─ Commit StockReservation
                 └─ Bắn PaymentCompletedEvent -> Gửi thông báo
```

---

## 8. Thiết kế RESTful API chuẩn hóa

Tất cả các API tuân theo định dạng phản hồi chuẩn:

```json
{
  "success": true,
  "message": "Thành công",
  "data": { ... },
  "timestamp": "2026-11-04T07:00:00Z"
}
```

Prefix API: `/api/v1/`

### 8.1. API Khách hàng (Customer Endpoints)

| Phân hệ | Phương thức | Endpoint | Chức năng |
|---|---|---|---|
| **Auth** | POST | `/auth/register` | Đăng ký tài khoản mới |
| | POST | `/auth/login` | Đăng nhập nhận Access + Refresh Token |
| | POST | `/auth/refresh-token` | Lấy Access Token mới bằng Refresh Token |
| | GET | `/auth/me` | Lấy thông tin user hiện tại |
| **Catalog** | GET | `/products` | Danh sách SP (lọc danh mục, giá, thương hiệu, phân trang) |
| | GET | `/products/{slug}` | Chi tiết sản phẩm & danh sách biến thể |
| | GET | `/categories/tree` | Danh mục đa cấp dạng cây |
| **Cart** | GET | `/cart` | Xem chi tiết giỏ hàng hiện tại |
| | POST | `/cart/items` | Thêm biến thể sản phẩm vào giỏ |
| | PUT | `/cart/items/{id}` | Sửa số lượng sản phẩm trong giỏ |
| | DELETE | `/cart/items/{id}` | Xóa sản phẩm khỏi giỏ |
| **Order** | POST | `/orders/checkout` | Thực hiện đặt hàng và giữ tồn kho |
| | GET | `/orders` | Lịch sử mua hàng của người dùng |
| | GET | `/orders/{orderNumber}` | Xem chi tiết và tracking trạng thái đơn |
| | POST | `/orders/{orderNumber}/cancel` | Hủy đơn và hoàn tồn kho |
| | POST | `/orders/{orderNumber}/reorder` | Mua lại đơn hàng cũ |
| **Payment** | POST | `/payments/{orderNumber}/pay-url`| Lấy lại link thanh toán khi muốn trả lại |
| | POST | `/webhooks/vnpay` | Nhận kết quả thanh toán từ VNPay IPN |
| **Search** | GET | `/search/products` | Tìm kiếm text toàn văn |
| | POST | `/search/image` | Tải ảnh lên tìm kiếm tương đồng (Image Search) |
| | GET | `/search/suggestions` | Gợi ý từ khóa tự động |

### 8.2. API Quản trị (Admin Endpoints - Cần Role `ROLE_ADMIN`)

Prefix: `/api/v1/admin/`

| Phân hệ | Phương thức | Endpoint | Chức năng |
|---|---|---|---|
| **Dashboard** | GET | `/dashboard/overview` | Số liệu doanh thu, đơn hàng, người dùng |
| **Catalog** | POST | `/products` | Tạo sản phẩm mới |
| | PUT | `/products/{id}` | Cập nhật thông tin sản phẩm |
| | POST | `/products/{id}/variants` | Thêm biến thể cho sản phẩm |
| | PUT | `/products/variants/{variantId}` | Sửa giá/thuộc tính biến thể |
| **Inventory** | GET | `/inventory` | Danh sách tồn kho theo từng SKU biến thể |
| | PUT | `/inventory/{variantId}/adjust` | Điều chỉnh hoặc nhập hàng vào kho |
| **Orders** | GET | `/orders` | Danh sách đơn hàng toàn hệ thống |
| | PUT | `/orders/{id}/status` | Chuyển trạng thái đơn (CONFIRMED -> SHIPPING -> DELIVERED) |
| **AI Search** | POST | `/search/reindex` | Trigger re-index lại vector embedding toàn kho |

---

## 9. Lộ trình Triển khai Phân kỳ Thực tế (Phased Roadmap)

Để tránh rủi ro quá tải (burnout) và đảm bảo dự án có sản phẩm chạy được sớm nhất, kế hoạch được chia thành **2 Giai đoạn**:

### 🎯 GIAI ĐOẠN 1: MVP Core — Sẵn sàng Bán hàng (Tuần 1 – Tuần 8)
*Mục tiêu: Hoàn thiện toàn bộ luồng mua bán, quản lý tồn kho và thanh toán online cơ bản.*

| Tuần | Trọng tâm công việc | Kết quả đầu ra |
|---|---|---|
| **Tuần 1** | **Foundation & Setup**: Khởi tạo Spring Boot 3, Java 21, PostgreSQL + Docker Compose, cấu hình Flyway, Global Exception Handler, Swagger UI. | Dự án build pass, kết nối DB thông suốt |
| **Tuần 2** | **Auth & RBAC**: Module User, Spring Security 6, JWT Access/Refresh Token, phân quyền USER/ADMIN. | Đăng ký, đăng nhập, bảo vệ endpoint |
| **Tuần 3** | **Catalog & Variants**: Module Catalog, CRUD Category, Brand, Product và ProductVariant (quản lý SKU, giá bán, attributes JSONB). | Admin quản lý danh mục và sản phẩm biến thể |
| **Tuần 4** | **Inventory & Concurrency**: Bảng `inventory`, `stock_reservations`. Cơ chế Pessimistic Lock khi checkout và Scheduled Task nhả tồn kho. | Không thể bán vượt tồn kho kể cả khi concurrent |
| **Tuần 5** | **Cart & Checkout**: Giỏ hàng (Redis guest + DB logged in), hàm merge cart. Luồng tạo đơn hàng, lưu snapshot `order_items`. | Luồng từ xem hàng đến chốt đơn hoàn tất |
| **Tuần 6** | **Payment Gateway (VNPay)**: Tích hợp VNPay sandbox, tạo URL thanh toán, xử lý Webhook IPN bảo mật (Idempotency Key). | Thanh toán online và cập nhật đơn tự động |
| **Tuần 7** | **Order Flow & Admin Core**: Admin duyệt đơn, chuyển trạng thái đơn hàng. User tra cứu lịch sử, hủy đơn, reorder. | Vòng đời đơn hàng khép kín |
| **Tuần 8** | **Testing & Bug Fixing GĐ 1**: Viết Integration Test cho luồng Checkout/Payment, audit bảo mật, chuẩn bị môi trường Staging. | **Chốt phiên bản MVP v1.0 Go-Live** |

---

### 🚀 GIAI ĐOẠN 2: AI Search & Tính năng Nâng cao (Tuần 9 – Tuần 12+)
*Mục tiêu: Đưa tính năng thông minh vào hệ thống để gia tăng trải nghiệm người dùng.*

| Tuần | Trọng tâm công việc | Kết quả đầu ra |
|---|---|---|
| **Tuần 9** | **AI Helper Service**: Dựng Python FastAPI container chạy mô hình OpenCLIP ViT-B-32, endpoint trích xuất vector 512 dims. | Service vector hóa ảnh độc lập chạy ổn định |
| **Tuần 10** | **pgvector Integration**: Kích hoạt `pgvector`, bảng `product_embeddings`, HNSW index. Endpoint tìm kiếm sản phẩm bằng ảnh (`/search/image`). | Tìm được sản phẩm LEGO tương tự từ ảnh upload |
| **Tuần 11** | **Hybrid Search & Đa Cổng**: Kết hợp Full-Text Search + Vector Search. Tích hợp thêm ví MoMo / ZaloPay bằng Gateway Factory đã dựng. | Tìm kiếm thông minh và thanh toán đa kênh |
| **Tuần 12** | **Engagement & Polish**: Đánh giá sản phẩm đã mua (Verified Reviews), Danh sách yêu thích (Wishlist), Tối ưu Redis cache. | **Hệ thống hoàn chỉnh v2.0** |

---

## 10. Checklist Khởi tạo Dự án (Action Items ngay)

- [ ] Tạo project Spring Boot 3.3.x với Java 21, Maven.
- [ ] Thêm các starter: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`.
- [ ] Thêm dependencies: PostgreSQL Driver, Flyway Core, Redisson, MapStruct, Lombok, Springdoc OpenAPI.
- [ ] Soạn thảo `docker-compose.yml` gồm:
  - PostgreSQL 16 (có cài sẵn extension pgvector).
  - Redis 7 Alpine.
- [ ] Tạo file `.env.example` định nghĩa các biến môi trường cấu hình DB, JWT secret, Cloudinary key, VNPay sandbox.
- [ ] Thiết lập migration Flyway đầu tiên `V1__init_security_and_users.sql`.
- [ ] Tạo khung thư mục Package by Feature như cấu trúc đề xuất.
- [ ] Chạy kiểm thử kết nối và verify trên Swagger UI: `http://localhost:8080/swagger-ui.html`.
