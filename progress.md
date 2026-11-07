# BrickStore — Tiến độ Phát triển Dự án (Development Progress)

> **Dự án:** BrickStore — Website thương mại điện tử chuyên đồ chơi LEGO & mô hình trưng bày  
> **Ngôn ngữ & Framework:** Java 21 LTS / Spring Boot 3.4.x  
> **Cập nhật lần cuối:** 07/11/2026  

---

## 📊 Bảng tổng quan tiến độ (Roadmap Status)

| Giai đoạn | Tuần | Hạng mục công việc | Trạng thái | Ghi chú |
|---|---|---|---|---|
| **Giai đoạn 1** | **Tuần 1** | **Foundation & Setup**: Khởi tạo Spring Boot 3, Java 21, PostgreSQL + pgvector, Redis, Flyway, Global Exception, Swagger UI | <span style="color:green">✔ **HOÀN THÀNH**</span> | Đã build pass, cấu trúc Modular Monolith chuẩn |
| | **Tuần 2** | **Auth & RBAC**: Module User, Spring Security 6, JWT Access/Refresh Token, RBAC USER/ADMIN | <span style="color:green">✔ **HOÀN THÀNH**</span> | 11/11 tests pass (100%), bảo mật toàn diện |
| | **Tuần 3** | **Catalog & Variants**: Module Catalog, CRUD Category, Brand, Product & ProductVariant, Dynamic Filtering | <span style="color:green">✔ **HOÀN THÀNH**</span> | 23/23 tests pass (100%), quản lý biến thể SKU, JSONB |
| | **Tuần 4** | **Inventory & Concurrency**: Quản lý tồn kho, Pessimistic Lock, Stock Reservation, Scheduler, Chống Over-selling | <span style="color:green">✔ **HOÀN THÀNH**</span> | 37/37 tests pass (100%), Concurrency Test 20 threads |
| | **Tuần 5** | **Cart & Checkout**: Giỏ hàng (Redis + DB), Cart Merge, Checkout luồng đơn hàng | ⏳ **SẴN SÀNG TRIỂN KHAI** | Đã sẵn sàng các điều kiện tiên quyết |
| | **Tuần 6** | **Payment Gateway (VNPay)**: VNPay Sandbox, Webhook IPN, Idempotency Control | ⚪ Chưa bắt đầu | |
| | **Tuần 7** | **Order Flow & Admin Core**: Xử lý vòng đời đơn, quản trị admin, tra cứu đơn hàng | ⚪ Chưa bắt đầu | |
| | **Tuần 8** | **Testing & Bug Fixing GĐ 1**: Integration test, audit bảo mật, chuẩn bị MVP Go-Live | ⚪ Chưa bắt đầu | |
| **Giai đoạn 2** | **Tuần 9 - 12** | **AI Search & Advanced Features**: Python CLIP AI Service, pgvector HNSW, Hybrid Search | ⚪ Chưa bắt đầu | |

---

## 📝 Chi tiết kết quả thực hiện Tuần 1: Foundation & Setup

### 1. Khởi tạo dự án & Quản lý Dependencies (`Backend/pom.xml`)
- [x] Thiết lập **Spring Boot 3.4.2** trên nền tảng **Java 21 LTS** với build tool Maven.
- [x] Tích hợp đầy đủ các dependencies cốt lõi:
  - **Web & Validation:** `spring-boot-starter-web`, `spring-boot-starter-validation`.
  - **Bảo mật:** `spring-boot-starter-security`, thư viện JWT mới nhất `io.jsonwebtoken:jjwt-api:0.12.6` (kèm `jjwt-impl` và `jjwt-jackson`).
  - **Dữ liệu & ORM:** `spring-boot-starter-data-jpa`, `org.postgresql:postgresql:42.7.5`.
  - **Database Migration:** `flyway-core:10.20.1`, `flyway-database-postgresql`.
  - **Cache & Distributed Lock:** `spring-boot-starter-data-redis`, `redisson:3.44.0`.
  - **API Documentation:** `springdoc-openapi-starter-webmvc-ui:2.8.5` (Swagger 3).
  - **Tiện ích phát triển:** `lombok`, `mapstruct:1.6.3`, `dotenv-java:3.1.0`.
  - **Cấu hình compiler:** Cấu hình `maven-compiler-plugin` gắn bộ xử lý annotation kết hợp `lombok` và `lombok-mapstruct-binding` giúp sinh mã DTO ↔ Entity tự động tại compile time.

### 2. Hạ tầng Container hóa & Môi trường cấu hình
- [x] Xây dựng file `docker-compose.yml`:
  - Service `postgres`: Sử dụng image `pgvector/pgvector:pg16` kèm volume gắn ngoài và health check tự động.
  - Service `redis`: Sử dụng image `redis:7-alpine` hỗ trợ AOF persistence và health check tự động.
- [x] Khởi tạo `.env.example` và `.env` chuẩn hóa toàn bộ cấu hình: Database, Redis, JWT Secret & Expiration, Cloudinary, VNPay.
- [x] Cấu hình file `application.yml` và `application-dev.yml`:
  - HikariCP connection pool tối ưu.
  - Tự động validate schema JPA kết hợp Flyway migration.
  - Linh hoạt bật/tắt Redis qua cờ `spring.data.redis.enabled`.
  - Cấu hình Swagger docs tại `/swagger-ui.html` và `/v3/api-docs`.

### 3. Cơ sở dữ liệu & Database Migration (`V1__init_security_and_users.sql`)
- [x] Khởi tạo kịch bản Flyway đầu tiên:
  - Bảng `roles`: Lưu danh sách vai trò hệ thống.
  - Bảng `users`: Lưu thông tin tài khoản người dùng, hỗ trợ nhà cung cấp (LOCAL, GOOGLE, FACEBOOK).
  - Bảng `user_roles`: Bảng nối quan hệ Nhiều - Nhiều giữa User và Role.
  - Bảng `refresh_tokens`: Lưu trữ Refresh Token hỗ trợ cơ chế cấp phát token an toàn và tính năng thu hồi (revocation).
  - Chỉ mục (Indexes): Đánh index cho `email` trên `users` và `token`, `user_id` trên `refresh_tokens`.
  - Dữ liệu khởi tạo (Seed data): Nạp sẵn vai trò `ROLE_USER`, `ROLE_ADMIN` và tài khoản quản trị mặc định (`admin@brickstore.com`).

### 4. Cấu trúc mã nguồn Modular Monolith (Package by Feature)
Đã tổ chức chuẩn hóa toàn bộ các gói module tại `Backend/src/main/java/com/brickstore/`:
- `common/`: Các thành phần dùng chung toàn hệ thống (`ApiResponse`, `PageResponse`, `GlobalExceptionHandler`, `BusinessException`, `ResourceNotFoundException`, `ErrorCode`, `BaseEntity`, `AppConstant`, `SecurityConstant`, `HealthCheckController`).
- `config/`: `JpaAuditingConfig`, `CorsConfig`, `OpenApiConfig`, `RedisConfig`.
- `security/`: `SecurityConfig`.
- Các package nghiệp vụ: `user/`, `catalog/`, `inventory/`, `cart/`, `order/`, `payment/`, `search/`, `promotion/`, `review/`, `notification/`.

---

## 📝 Chi tiết kết quả thực hiện Tuần 2: Authentication & Authorization (Auth & RBAC)

### 1. Mô hình Thực thể Dữ liệu (JPA Entities)
- [x] `Role.java` (`com.brickstore.user.entity`): Ánh xạ bảng `roles`, hỗ trợ phân quyền `ROLE_USER` và `ROLE_ADMIN`.
- [x] `User.java` (`com.brickstore.user.entity`): Ánh xạ bảng `users`, kế thừa `BaseEntity` (`createdAt`, `updatedAt`), quan hệ `@ManyToMany` nạp EAGER với `Role`.
- [x] `RefreshToken.java` (`com.brickstore.user.entity`): Ánh xạ bảng `refresh_tokens`, lưu token chuỗi, thời gian hết hạn (`expiresAt`), cờ thu hồi (`revoked`) và liên kết `@ManyToOne` với `User`.

### 2. Tầng Truy vấn Dữ liệu (Spring Data JPA Repositories)
- [x] `RoleRepository.java`: `findByName(String name)` hỗ trợ tìm kiếm vai trò theo tên.
- [x] `UserRepository.java`: `findByEmail(String email)`, `existsByEmail(String email)` tối ưu tìm kiếm và kiểm tra trùng lặp email.
- [x] `RefreshTokenRepository.java`: `findByToken(String token)`, `deleteByUser(User user)` hỗ trợ xoay vòng refresh token an toàn.

### 3. Thành phần Bảo mật & Tích hợp Spring Security 6
- [x] `CustomUserDetails.java`: Triển khai interface `UserDetails` của Spring Security, bao bọc thực thể `User`, trích xuất Authorities (`SimpleGrantedAuthority`) từ tập hợp vai trò.
- [x] `CustomUserDetailsService.java`: Triển khai `UserDetailsService`, tìm kiếm tài khoản theo email khi xác thực request.
- [x] `JwtTokenProvider.java`:
  - Khởi tạo khóa ký bí mật `HMAC-SHA` an toàn chuẩn RFC.
  - Sinh **Access Token** chứa claims (Subject email, Roles) với hạn sử dụng 15 phút (`900,000ms`).
  - Sinh **Refresh Token** lưu vết với hạn sử dụng 7 ngày (`604,800,000ms`).
  - Trích xuất thông tin Subject email và kiểm tra tính hợp lệ của token (bắt và log chi tiết các lỗi: `ExpiredJwtException`, `MalformedJwtException`, `SignatureException`, `UnsupportedJwtException`).
- [x] `JwtAuthenticationFilter.java`: Bộ lọc `OncePerRequestFilter` chặn các HTTP request, phân tích header `Authorization: Bearer <token>`, xác minh token và nạp `UsernamePasswordAuthenticationToken` vào `SecurityContextHolder`.
- [x] `JwtAuthenticationEntryPoint.java`: Bắt lỗi 401 Unauthorized khi truy cập trái phép và định dạng phản hồi chuẩn JSON thống nhất qua `ApiResponse.error("AUTH_001", ...)`.
- [x] `SecurityConfig.java`:
  - Kích hoạt `@EnableWebSecurity` và `@EnableMethodSecurity`.
  - Thiết lập Session Policy `STATELESS`.
  - Đăng ký `JwtAuthenticationFilter` trước `UsernamePasswordAuthenticationFilter`.
  - Cấu hình ngoại lệ phân quyền `jwtAuthenticationEntryPoint`.
  - Cho phép truy cập công khai: `/api/v1/auth/**`, `/api/v1/health`, Swagger docs `/v3/api-docs/**`, `/swagger-ui/**`.
  - Yêu cầu xác thực bắt buộc cho các endpoint còn lại (`/api/v1/users/**`).

### 4. Thiết kế DTOs & Object Mapping (MapStruct)
- [x] Request DTOs:
  - `RegisterRequest.java`: Validate email chuẩn, mật khẩu 6-50 ký tự, họ tên 2-100 ký tự, số điện thoại regex Việt Nam.
  - `LoginRequest.java`: Validate email và password không được để trống.
  - `RefreshTokenRequest.java`: Validate chuỗi refresh token.
  - `UpdateProfileRequest.java`: Validate thông tin cập nhật cá nhân.
  - `ChangePasswordRequest.java`: Validate mật khẩu cũ, mật khẩu mới và xác nhận mật khẩu mới.
- [x] Response DTOs:
  - `AuthResponse.java`: Trả về `accessToken`, `refreshToken`, `tokenType = "Bearer"`, `expiresIn`, và đối tượng `UserResponse`.
  - `UserResponse.java`: Trả về thông tin an toàn của tài khoản (ẩn mật khẩu), chuyển đổi `roles` thành `Set<String>`.
- [x] Mapper:
  - `UserMapper.java`: Ánh xạ MapStruct hiệu năng cao tại compile-time giữa `User` và `UserResponse`.
- [x] Tiện ích:
  - `SecurityUtils.java` (`com.brickstore.common.util`): Tiện ích tĩnh dùng chung giúp trích xuất email của người dùng đang đăng nhập từ `SecurityContextHolder`.

### 5. Nghiệp vụ Ứng dụng (Application Services)
- [x] `AuthService.java` & `AuthServiceImpl.java`:
  - `register()`: Kiểm tra trùng lặp email, mã hóa BCrypt mật khẩu, gán vai trò mặc định `ROLE_USER`, phát hành cặp token JWT và lưu `RefreshToken` vào cơ sở dữ liệu.
  - `login()`: Xác thực credentials qua `AuthenticationManager`, kiểm tra tài khoản có bị khóa không (`isActive`), phát hành cặp token JWT mới và cập nhật `RefreshToken` vào DB.
  - `refreshToken()`: Kiểm tra tính tồn tại, tính hợp lệ chữ ký, trạng thái thu hồi (`revoked`) và hạn sử dụng của token; cấp mới `accessToken`.
  - `logout()`: Thu hồi (`revoked = true`) refresh token trong cơ sở dữ liệu.
- [x] `UserService.java` & `UserServiceImpl.java`:
  - `getCurrentUser()`: Tra cứu thông tin tài khoản người dùng hiện tại từ token đăng nhập.
  - `updateProfile()`: Cho phép người dùng chỉnh sửa họ tên, số điện thoại, ảnh đại diện.
  - `changePassword()`: Kiểm tra khớp mật khẩu cũ, không cho trùng mật khẩu mới với cũ, kiểm tra xác nhận mật khẩu mới và lưu mật khẩu mã hóa BCrypt.
  - `getUserById()`: Hỗ trợ Quản trị viên (Admin) tra cứu thông tin tài khoản theo ID.

### 6. Tầng Giao tiếp API (REST Controllers)
- [x] `AuthController.java` (`/api/v1/auth`):
  - `POST /api/v1/auth/register`: Đăng ký tài khoản (HTTP 201 Created).
  - `POST /api/v1/auth/login`: Đăng nhập hệ thống (HTTP 200 OK).
  - `POST /api/v1/auth/refresh-token`: Cấp lại Access Token (HTTP 200 OK).
  - `POST /api/v1/auth/logout`: Đăng xuất (HTTP 200 OK).
- [x] `UserController.java` (`/api/v1/users`):
  - `GET /api/v1/users/me`: Lấy thông tin cá nhân (Authenticated).
  - `PUT /api/v1/users/me`: Cập nhật thông tin cá nhân.
  - `PUT /api/v1/users/me/change-password`: Thay đổi mật khẩu.
  - `GET /api/v1/users/{id}`: Tra cứu tài khoản theo ID (Bảo vệ phân quyền `@PreAuthorize("hasRole('ADMIN')")`).

### 7. Kiểm thử Tích hợp & Xác minh Chất lượng (Automated Integration Tests)
Đã viết bộ kiểm thử tự động toàn diện với MockMvc tại `src/test/java/com/brickstore/`:
- [x] `AuthControllerTest.java` (5 kịch bản):
  1. `register_Success`: Đăng ký thành công tài khoản mới, nhận đủ cặp token và quyền `ROLE_USER`.
  2. `register_DuplicateEmail_Conflict`: Bắt lỗi 409 Conflict khi đăng ký email đã tồn tại.
  3. `login_Admin_Success`: Đăng nhập thành công với tài khoản Admin mặc định, nhận quyền `ROLE_ADMIN`.
  4. `login_WrongPassword_Unauthorized`: Bắt lỗi 401 Unauthorized khi đăng nhập sai mật khẩu.
  5. `refreshToken_Success`: Refresh Access Token thành công bằng Refresh Token hợp lệ.
- [x] `UserControllerTest.java` (5 kịch bản):
  6. `getMe_WithoutToken_Unauthorized`: Chặn truy cập trái phép khi thiếu Authorization header (HTTP 401).
  7. `getMe_WithToken_Success`: Cho phép truy cập hồ sơ cá nhân khi gửi kèm Bearer JWT Token hợp lệ (HTTP 200).
  8. `updateProfile_Success`: Cập nhật họ tên, số điện thoại thành công.
  9. `changePassword_WrongCurrentPassword_BadRequest`: Bắt lỗi khi nhập sai mật khẩu hiện tại (HTTP 400).
  10. `changePassword_Success`: Đổi mật khẩu thành công khi thông tin hợp lệ.
- [x] `BrickstoreBackendApplicationTests.java` (1 kịch bản):
  11. `contextLoads`: Khởi động trơn tru toàn bộ Spring Context.

> **KẾT QUẢ KIỂM THỬ: 11 / 11 Tests Passed (100% Success, 0 Failures, 0 Errors).**

---

## 📝 Chi tiết kết quả thực hiện Tuần 3: Catalog & Variants (Danh mục, Thương hiệu, Sản phẩm & Biến thể SKU)

### 1. Cơ sở dữ liệu & Database Migration (`V2__init_catalog_and_variants.sql`)
- [x] Tạo bảng `categories`:
  - Quản lý phân cấp danh mục cha - con (`parent_id REFERENCES categories(id) ON DELETE SET NULL`).
  - Đường dẫn thân thiện SEO `slug` (UNIQUE, INDEX).
  - Thứ tự sắp xếp `sort_order` và trạng thái `is_active`.
- [x] Tạo bảng `brands`:
  - Lưu trữ thương hiệu sản phẩm (LEGO, CaDA, Mould King,...).
  - Khóa `slug` (UNIQUE, INDEX), `logo_url`, `is_active`.
- [x] Tạo bảng `products`:
  - Khóa ngoại `category_id` (NOT NULL) và `brand_id`.
  - Thông tin tên, `slug` duy nhất, mô tả, tóm tắt, giá gốc `base_price`, ảnh đại diện `thumbnail_url`.
  - Cờ trạng thái: `is_active`, `is_featured` (sản phẩm nổi bật).
  - Chỉ số tổng hợp: `avg_rating` (đánh giá TB), `review_count` (số lượt review), `sold_count` (đã bán).
  - Đánh chỉ mục hiệu năng: `idx_products_category`, `idx_products_brand`, `idx_products_price`, `idx_products_created_at`, `idx_products_is_featured`.
- [x] Tạo bảng `product_variants`:
  - Mã định danh biến thể tồn kho độc lập `sku` (UNIQUE, INDEX).
  - Giá bán `price`, giá so sánh `compare_price`, số lượng tồn kho `stock_quantity`.
  - Thuộc tính linh hoạt kiểu `JSONB` (`attributes`) lưu thông số phiên bản, số mảnh ghép, màu sắc, phụ kiện,...
  - Đánh chỉ mục GIN: `CREATE INDEX idx_product_variants_attributes_gin ON product_variants USING gin (attributes)`.
- [x] Tạo bảng `product_images`:
  - Quản lý album ảnh sản phẩm, cờ `is_primary`, thứ tự sắp xếp `sort_order`.
- [x] Seed dữ liệu ban đầu cho LEGO:
  - 5 danh mục LEGO và phụ kiện mô hình.
  - 4 thương hiệu nổi tiếng.
  - Sản phẩm mẫu: `LEGO Star Wars 75192 Millennium Falcon UCS` kèm 2 biến thể SKU và 2 hình ảnh chi tiết.

### 2. Mô hình Thực thể Dữ liệu (JPA Entities)
- [x] `Category.java` (`com.brickstore.catalog.entity`): Kế thừa `BaseEntity`, liên kết `@ManyToOne` cha và `@OneToMany` danh sách danh mục con.
- [x] `Brand.java` (`com.brickstore.catalog.entity`): Kế thừa `BaseEntity`, quản lý thương hiệu và logo.
- [x] `Product.java` (`com.brickstore.catalog.entity`): Kế thừa `BaseEntity`, liên kết `Category`, `Brand`, `@OneToMany` danh sách `variants` và `images` với cascade full lifecycle và helper methods `addVariant`, `removeVariant`, `addImage`, `removeImage`.
- [x] `ProductVariant.java` (`com.brickstore.catalog.entity`): Kế thừa `BaseEntity`, hỗ trợ `@JdbcTypeCode(SqlTypes.JSON)` lưu trữ `Map<String, Object>` vào cột `jsonb` của PostgreSQL.
- [x] `ProductImage.java` (`com.brickstore.catalog.entity`): Lưu trữ album ảnh và sắp xếp hiển thị.

### 3. Tầng Truy vấn Dữ liệu & Bộ lọc Động (Repositories & Specifications)
- [x] `CategoryRepository.java`: Hỗ trợ tìm kiếm theo slug, lấy cây danh mục gốc (`parent IS NULL`).
- [x] `BrandRepository.java`: Hỗ trợ tìm theo slug, kiểm tra trùng lặp slug, lấy danh sách thương hiệu đang hoạt động.
- [x] `ProductRepository.java`:
  - Kế thừa `JpaRepository` & `JpaSpecificationExecutor<Product>`.
  - `@EntityGraph(attributePaths = {"category", "brand"})` tối ưu truy vấn chi tiết sản phẩm, loại bỏ triệt để lỗi Cartesian Product và `MultipleBagFetchException`.
  - Truy vấn Top 10 sản phẩm nổi bật (`isFeatured = true`) và Top 10 sản phẩm mới nhất.
- [x] `ProductVariantRepository.java`: Quản lý SKU, kiểm tra tồn tại và trùng lặp SKU.
- [x] `ProductImageRepository.java`: Quản lý album ảnh theo thứ tự sắp xếp.
- [x] `ProductSpecification.java`: Xây dựng Predicates động bằng JPA Criteria API:
  - Tìm kiếm toàn văn theo từ khóa `keyword` (trong `name` và `description`).
  - Lọc theo `categoryId` hoặc `categorySlug`.
  - Lọc theo `brandId` hoặc `brandSlug`.
  - Lọc theo khoảng giá (`minPrice`, `maxPrice`).
  - Lọc theo cờ nổi bật `isFeatured` và trạng thái `isActive`.

### 4. Tiện ích & Mappers (MapStruct & Slug Generator)
- [x] `SlugUtils.java` (`com.brickstore.common.util`): Chuẩn hóa chuỗi tiếng Việt có dấu (xử lý ký tự Đ/đ, bỏ dấu thanh Unicode NFD, ký tự đặc biệt, chuyển thành slug SEO chuẩn).
- [x] `CategoryMapper.java`: Ánh xạ DTO ↔ Entity, hỗ trợ chuyển đổi đệ quy cây thư mục `CategoryTreeResponse`.
- [x] `BrandMapper.java`: Ánh xạ thương hiệu.
- [x] `ProductMapper.java`: Ánh xạ đa tầng giữa `Product`, `ProductVariant`, `ProductImage` và các DTOs chi tiết.

### 5. Dịch vụ Nghiệp vụ (Business Services)
- [x] `CategoryService` / `CategoryServiceImpl`:
  - Lấy danh sách danh mục phẳng và cây phân cấp (`getCategoryTree`).
  - CRUD danh mục với cơ chế sinh slug tự động, tự phân giải xung đột trùng slug bằng hậu tố tăng dần `-1`, `-2`.
  - Ràng buộc an toàn: Không cho phép chọn chính mình làm danh mục cha.
- [x] `BrandService` / `BrandServiceImpl`:
  - Lấy danh sách thương hiệu kích hoạt, CRUD thương hiệu kèm auto slug.
- [x] `ProductService` / `ProductServiceImpl`:
  - Tìm kiếm & lọc phân trang linh hoạt qua `PageResponse<ProductResponse>`.
  - Lấy chi tiết sản phẩm kèm toàn bộ danh sách biến thể SKU và album ảnh.
  - Tạo sản phẩm mới kèm khởi tạo đồng thời các biến thể và ảnh, kiểm tra chặt chẽ trùng lặp SKU (cả trong nội bộ request lẫn trong cơ sở dữ liệu).
  - Quản lý độc lập vòng đời biến thể: thêm biến thể mới (`addVariant`), cập nhật biến thể (`updateVariant`), xóa biến thể (`deleteVariant`).
  - Quản lý album ảnh: thêm ảnh (`addImage`), xóa ảnh (`deleteImage`).

### 6. Giao diện Lập trình Ứng dụng (REST Controllers)
- [x] **Public APIs (Khách hàng & Tìm kiếm):**
  - `CategoryController`:
    - `GET /api/v1/categories`: Lấy toàn bộ danh mục đang hoạt động.
    - `GET /api/v1/categories/tree`: Lấy cây danh mục phân cấp cha - con.
    - `GET /api/v1/categories/{slug}`: Lấy chi tiết danh mục theo slug.
  - `BrandController`:
    - `GET /api/v1/brands`: Lấy danh sách thương hiệu đang hoạt động.
    - `GET /api/v1/brands/{slug}`: Lấy chi tiết thương hiệu theo slug.
  - `ProductController`:
    - `GET /api/v1/products`: Tìm kiếm, lọc đa tiêu chí, phân trang sản phẩm.
    - `GET /api/v1/products/featured`: Lấy danh sách sản phẩm nổi bật.
    - `GET /api/v1/products/newest`: Lấy danh sách sản phẩm mới nhất.
    - `GET /api/v1/products/{slug}`: Lấy đầy đủ thông tin chi tiết sản phẩm, danh sách biến thể SKU và album ảnh.
- [x] **Admin APIs (Quản trị viên - Bắt buộc quyền `ROLE_ADMIN`):**
  - `AdminCategoryController` (`/api/v1/admin/categories`): Tạo mới, cập nhật, xóa danh mục.
  - `AdminBrandController` (`/api/v1/admin/brands`): Tạo mới, cập nhật, xóa thương hiệu.
  - `AdminProductController` (`/api/v1/admin/products`):
    - `GET /api/v1/admin/products`: Danh sách quản trị kèm phân trang và lọc trạng thái ẩn/hiện.
    - `POST /api/v1/admin/products`: Tạo sản phẩm kèm biến thể và ảnh.
    - `PUT /api/v1/admin/products/{id}`: Cập nhật thông tin sản phẩm.
    - `DELETE /api/v1/admin/products/{id}`: Xóa sản phẩm (cascade biến thể & ảnh).
    - `POST /api/v1/admin/products/{id}/variants`: Thêm biến thể SKU mới.
    - `PUT /api/v1/admin/products/{id}/variants/{variantId}`: Cập nhật biến thể.
    - `DELETE /api/v1/admin/products/{id}/variants/{variantId}`: Xóa biến thể.
    - `POST /api/v1/admin/products/{id}/images`: Thêm ảnh sản phẩm.
    - `DELETE /api/v1/admin/products/{id}/images/{imageId}`: Xóa ảnh sản phẩm.

### 7. Tự động hóa Kiểm thử Tích hợp (Automated Integration Tests)
Đã bổ sung bộ test toàn diện cho module Catalog nâng tổng số kịch bản test lên **23 tests**:
- [x] `ProductControllerTest.java` (7 kịch bản):
  1. `getProducts_Success`: Lấy danh sách sản phẩm phân trang thành công.
  2. `getProducts_FilterKeyword_Success`: Tìm kiếm sản phẩm theo từ khóa "Millennium" trả về đúng dữ liệu.
  3. `getProductBySlug_Success`: Lấy chi tiết sản phẩm theo slug hợp lệ, xác thực nạp đầy đủ danh sách biến thể và hình ảnh.
  4. `getProductBySlug_NotFound`: Trả về lỗi 404 Not Found (`SYS_003`) khi truy vấn slug không tồn tại.
  5. `getFeaturedProducts_Success`: Lấy danh sách sản phẩm nổi bật thành công.
  6. `getCategoryTree_Success`: Lấy cây phân cấp danh mục thành công.
  7. `getAllBrands_Success`: Lấy danh sách thương hiệu công khai thành công.
- [x] `AdminCatalogControllerTest.java` (5 kịch bản):
  8. `createCategory_Admin_Success`: Admin tạo danh mục mới thành công với auto slug chuẩn SEO.
  9. `createCategory_User_Forbidden`: Chặn người dùng thường khi gọi API quản trị (HTTP 403 Forbidden).
  10. `createBrand_Admin_Success`: Admin tạo thương hiệu mới thành công.
  11. `createProduct_WithVariantsAndImages_Success`: Admin tạo sản phẩm kèm biến thể SKU và album ảnh thành công.
  12. `createProduct_DuplicateSku_Conflict`: Bắt lỗi xung đột 409 Conflict (`SYS_005`) khi tạo biến thể với SKU đã tồn tại trong hệ thống.
- [x] Giữ vững 11 kịch bản kiểm thử bảo mật & tài khoản từ Tuần 2 (`AuthControllerTest`, `UserControllerTest`, `BrickstoreBackendApplicationTests`).

> **KẾT QUẢ KIỂM THỬ: 23 / 23 Tests Passed (100% Success, 0 Failures, 0 Errors).**

---

## 📝 Chi tiết kết quả thực hiện Tuần 4: Inventory & Concurrency (Quản lý tồn kho, Giữ chỗ & Chống Over-selling)

### 1. Cơ sở dữ liệu & Database Migration (`V3__init_inventory_and_reservations.sql`)
- [x] Tạo bảng `inventory`:
  - Khóa ngoại duy nhất `variant_id REFERENCES product_variants(id) ON DELETE CASCADE`.
  - Phân tách 2 trạng thái tồn kho: `available_stock` (khả dụng để mua) và `reserved_stock` (đang giữ chỗ thanh toán).
  - Ràng buộc toàn vẹn: `CHECK (available_stock >= 0)` và `CHECK (reserved_stock >= 0)`.
  - Cột `version` (Optimistic lock) và index `idx_inventory_variant`.
- [x] Tạo bảng `stock_reservations`:
  - Khóa chính UUID (`gen_random_uuid()`), liên kết `variant_id`.
  - `quantity`: Số lượng giữ chỗ (`CHECK (quantity > 0)`).
  - `order_reference`: Mã tham chiếu đơn hàng.
  - `status`: Trạng thái giữ chỗ (`RESERVED`, `COMMITTED`, `RELEASED`).
  - `expires_at`: Thời điểm hết hạn giữ chỗ (mặc định 15 phút).
  - Đánh chỉ mục hiệu năng: `idx_reservations_status_expires (status, expires_at)`, `idx_reservations_variant`, `idx_reservations_order_ref`.
- [x] Tạo bảng `inventory_logs`:
  - Lưu vết toàn bộ lịch sử xuất/nhập/điều chỉnh kho: `change_quantity`, `previous_stock`, `current_stock`, `action_type` (`RESTOCK`, `RESERVE`, `RELEASE`, `COMMIT`, `MANUAL_ADJUST`), `reference_id`, `note`.
  - Đánh index: `idx_inventory_logs_variant (variant_id, created_at DESC)`.
- [x] Đồng bộ dữ liệu ban đầu:
  - Tự động nạp dữ liệu tồn kho từ `product_variants.stock_quantity` sang bảng `inventory`.
  - Ghi nhận lịch sử log khởi tạo ban đầu (`RESTOCK`).

### 2. Mô hình Thực thể & Enums (JPA Entities)
- [x] `ReservationStatus.java` (`com.brickstore.inventory.enums`): `RESERVED`, `COMMITTED`, `RELEASED`.
- [x] `InventoryActionType.java` (`com.brickstore.inventory.enums`): `RESTOCK`, `RESERVE`, `RELEASE`, `COMMIT`, `MANUAL_ADJUST`.
- [x] `Inventory.java` (`com.brickstore.inventory.entity`): Kế thừa `BaseEntity`, liên kết `@OneToOne` với `ProductVariant`, phương thức tiện ích `getTotalStock()`.
- [x] `StockReservation.java` (`com.brickstore.inventory.entity`): Quản lý vòng đời yêu cầu giữ hàng, tự động quản lý `createdAt`, `updatedAt` qua JPA lifecycle callbacks.
- [x] `InventoryLog.java` (`com.brickstore.inventory.entity`): Thực thể kiểm toán lưu vết kho hàng.

### 3. Tầng Truy vấn Dữ liệu & Kỹ thuật Khóa Concurrency
- [x] `InventoryRepository.java`:
  - **Pessimistic Locking:** `@Lock(LockModeType.PESSIMISTIC_WRITE)` trên phương thức `findByVariantIdWithLock(Long variantId)` phát sinh câu lệnh SQL `SELECT ... FOR UPDATE`, đảm bảo tại một thời điểm chỉ có 1 transaction được quyền kiểm tra và cập nhật tồn kho của biến thể.
  - **Atomic SQL Updates:** Cung cấp các câu lệnh cập nhật nguyên tử ở cấp độ cơ sở dữ liệu:
    - `reserveStockAtomic`: Trừ `available_stock` và tăng `reserved_stock` với điều kiện bảo vệ `available_stock >= :qty`.
    - `commitStockAtomic`: Trừ `reserved_stock` khi thanh toán hoàn tất.
    - `releaseStockAtomic`: Trả lại `available_stock` từ `reserved_stock` khi hủy đơn hoặc hết hạn giữ chỗ.
  - `findAllWithVariant(Pageable pageable)`: Tối ưu eager fetch tránh N+1 query.
- [x] `StockReservationRepository.java`:
  - Tìm kiếm các reservation đã hết hạn: `findAllByStatusAndExpiresAtBefore(RESERVED, threshold)`.
  - Tìm kiếm theo `orderReference`.
- [x] `InventoryLogRepository.java`: Phân trang lịch sử biến động kho theo variant.

### 4. Dịch vụ Nghiệp vụ Tồn kho (Business Services)
- [x] `InventoryService` / `InventoryServiceImpl`:
  - `checkStock(variantId, quantity)`: Kiểm tra nhanh số lượng khả dụng.
  - `adjustStock(variantId, request)`: Quản trị viên nhập kho (Restock) hoặc điều chỉnh kho thủ công. Chặn giảm kho nếu làm âm tồn. Tự động đồng bộ số lượng tồn sang `product_variants.stock_quantity` và ghi log kiểm toán.
  - `reserveStock(request)`: Khóa hàng tạm thời trong 15 phút. Sử dụng Pessimistic Lock ngăn chặn tuyệt đối tình trạng bán vượt tồn kho (Over-selling).
  - `commitReservation(reservationId)`: Xác nhận đơn đã thanh toán thành công, trừ chính thức số lượng giữ chỗ khỏi kho.
  - `releaseReservation(reservationId)`: Giải phóng hàng giữ chỗ, hoàn trả lại vào tồn kho khả dụng.
  - `releaseExpiredReservations()`: Quét và giải phóng hàng loạt các reservation đã hết hạn.
  - `getInventoryLogs(variantId, pageable)`: Xem lịch sử biến động kho.

### 5. Tự động hóa Giải phóng Hàng quá hạn (Spring Scheduler)
- [x] `SchedulerConfig.java` (`com.brickstore.config`): Kích hoạt `@EnableScheduling` cho toàn hệ thống.
- [x] `InventoryScheduler.java` (`com.brickstore.inventory.scheduler`):
  - Định kỳ chạy ngầm mỗi 60 giây (`fixedDelay = 60000ms`, cấu hình linh hoạt qua `${app.inventory.reservation-cleanup-interval-ms}`).
  - Tự động gọi `releaseExpiredReservations()` để nhả lại các sản phẩm bị khách giữ chỗ nhưng bỏ dở phiên checkout không thanh toán, giúp sản phẩm quay lại kho cho người khác mua.

### 6. Giao diện Lập trình Ứng dụng (REST Controllers)
- [x] **Public / Customer APIs:**
  - `InventoryController` (`/api/v1/inventory`):
    - `GET /api/v1/inventory/{variantId}/check?quantity=1`: Khách kiểm tra tính khả dụng của SKU.
    - `POST /api/v1/inventory/reserve`: Đặt giữ chỗ hàng phục vụ luồng checkout đơn hàng.
- [x] **Admin APIs (`ROLE_ADMIN`):**
  - `AdminInventoryController` (`/api/v1/admin/inventory`):
    - `GET /api/v1/admin/inventory`: Phân trang danh sách tồn kho toàn bộ SKU trong hệ thống.
    - `GET /api/v1/admin/inventory/{variantId}`: Xem chi tiết tồn kho khả dụng và giữ chỗ của SKU.
    - `PUT /api/v1/admin/inventory/{variantId}/adjust`: Nhập hàng hoặc điều chỉnh kho.
    - `GET /api/v1/admin/inventory/{variantId}/logs`: Lịch sử biến động xuất/nhập/giữ/nhả kho.

### 7. Tự động hóa Kiểm thử Tích hợp & Đa luồng (Concurrency Tests)
Đã bổ sung 14 kịch bản kiểm thử mới cho module Inventory, nâng tổng số test lên **37 tests**:
- [x] `InventoryConcurrencyTest.java` (Kịch bản kiểm thử đa luồng cốt lõi):
  1. `testConcurrentReservation_PreventOverselling`:
     - Giả lập **20 luồng (threads) chạy song song cùng 1 thời điểm** (sử dụng `ExecutorService` & `CountDownLatch`) để tranh chấp mua một biến thể chỉ còn **5 sản phẩm tồn kho**.
     - Kết quả kiểm chứng: **Chính xác 5 luồng thành công**, **15 luồng bị từ chối** với lỗi `INSUFFICIENT_STOCK`.
     - Tồn kho khả dụng sau tranh chấp: **chính xác bằng 0**, tồn kho giữ chỗ: **chính xác bằng 5**.
     - **CHỨNG MINH 100% KHÔNG BỊ OVER-SELLING!**
- [x] `InventoryServiceTest.java` (8 kịch bản):
  2. `checkStock_Success`: Kiểm tra điều kiện đủ hàng và thiếu hàng.
  3. `adjustStock_Restock_Success`: Nhập thêm hàng thành công.
  4. `adjustStock_ExceedStock_ThrowsException`: Chặn điều chỉnh làm âm tồn kho.
  5. `reserveStock_Success`: Giữ chỗ tồn kho thành công, chuyển tồn khả dụng sang giữ chỗ.
  6. `reserveStock_Insufficient_ThrowsException`: Bắt lỗi khi giữ chỗ vượt tồn kho khả dụng.
  7. `commitReservation_Success`: Xác nhận xuất kho thành công khi thanh toán xong.
  8. `releaseReservation_Success`: Nhả hàng giữ chỗ hoàn về tồn kho khả dụng.
  9. `releaseExpiredReservations_Success`: Scheduler giải phóng thành công các reservation quá hạn.
- [x] `AdminInventoryControllerTest.java` (5 kịch bản):
  10. `getAllInventory_Admin_Success`: Phân trang danh sách tồn kho cho Admin.
  11. `adjustStock_Admin_Success`: Admin nhập hàng thành công.
  12. `adjustStock_User_Forbidden`: Chặn quyền người dùng thường khi cố điều chỉnh kho (HTTP 403 Forbidden).
  13. `checkStock_Public_Success`: Khách kiểm tra tồn kho công khai.
  14. `getInventoryLogs_Admin_Success`: Admin xem lịch sử biến động kho.
- [x] Giữ vững 23 kịch bản kiểm thử từ Tuần 1, 2 và 3 (`AuthControllerTest`, `UserControllerTest`, `ProductControllerTest`, `AdminCatalogControllerTest`, `BrickstoreBackendApplicationTests`).

> **KẾT QUẢ KIỂM THỬ: 37 / 37 Tests Passed (100% Success, 0 Failures, 0 Errors).**

---

## 🎯 Đánh giá tính sẵn sàng cho Tuần 5 (Cart & Checkout)

> **KẾT LUẬN: ĐÃ HOÀN TOÀN SẴN SÀNG (100%) ĐỂ THỰC HIỆN TUẦN 5.**

### Các điều kiện tiên quyết đã đáp ứng:
1. **Quản lý tồn kho & Giữ hàng đồng bộ:** `InventoryService.reserveStock()` và `releaseReservation()` đã hoạt động ổn định và được kiểm chứng chống Over-selling đa luồng.
2. **Catalog & SKU hoàn chỉnh:** Đã có đầy đủ sản phẩm, biến thể, giá bán, ảnh để phục vụ thêm vào giỏ hàng và lưu snapshot đơn hàng.
3. **Cơ chế xác thực an toàn:** Phân biệt rõ ràng người dùng ẩn danh (Guest) và người dùng đã đăng nhập (Logged-in User).

### Kế hoạch hành động cụ thể cho Tuần 5:
1. **Viết Migration Flyway `V4__init_cart_and_orders.sql`**:
   - Bảng `cart_items`: Lưu giỏ hàng trong Database cho người dùng đã đăng nhập.
   - Bảng `orders`: Thông tin đơn hàng (`order_number`, `user_id`, `total_amount`, `shipping_fee`, `status`, `shipping_address`, `payment_method`).
   - Bảng `order_items`: Lưu snapshot thông tin sản phẩm lúc mua (`product_name`, `variant_name`, `sku`, `price`, `quantity`, `image_url`).
2. **Quản lý Giỏ hàng Đa tầng (Redis Guest Cart + Database User Cart)**:
   - Khách vãng lai: Lưu giỏ hàng tạm trong Redis theo `sessionId` / `guestId` (hết hạn sau 7 ngày).
   - Khách đăng nhập: Lưu giỏ hàng trong Database bảng `cart_items`.
   - **Cart Merge Logic**: Khi khách đăng nhập, tự động gộp các sản phẩm từ guest cart vào tài khoản.
3. **Triển khai luồng Checkout & Tạo Đơn hàng**:
   - Validate giỏ hàng -> Gọi `InventoryService.reserveStock()` để giữ tồn kho trong 15 phút.
   - Tạo đơn hàng ở trạng thái `PENDING_PAYMENT` kèm snapshot `order_items`.
   - Phát hành Spring Application Event `OrderPlacedEvent`.
4. **Phát triển REST Controllers**:
   - `CartController`: `GET /api/v1/cart`, `POST /api/v1/cart/items`, `PUT /api/v1/cart/items/{id}`, `DELETE /api/v1/cart/items/{id}`, `POST /api/v1/cart/merge`.
   - `OrderController`: `POST /api/v1/orders/checkout`, `GET /api/v1/orders`, `GET /api/v1/orders/{orderNumber}`.
5. **Viết Integration Tests toàn diện cho Giỏ hàng & Checkout**.
