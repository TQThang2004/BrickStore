-- V2: Khởi tạo bảng Danh mục, Thương hiệu, Sản phẩm, Biến thể & Hình ảnh

-- 1. Bảng Categories (Danh mục sản phẩm, hỗ trợ đa cấp)
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    image_url TEXT,
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng Brands (Thương hiệu sản phẩm)
CREATE TABLE IF NOT EXISTS brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    logo_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng Products (Sản phẩm chính)
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    brand_id BIGINT REFERENCES brands(id),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(280) NOT NULL UNIQUE,
    description TEXT,
    short_description VARCHAR(500),
    base_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    thumbnail_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    avg_rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    review_count INT NOT NULL DEFAULT 0,
    sold_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng Product Variants (Biến thể SKU: phiên bản, hộp trưng bày, tình trạng)
CREATE TABLE IF NOT EXISTS product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(15, 2) NOT NULL,
    compare_price NUMERIC(15, 2),
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    attributes JSONB,
    image_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Bảng Product Images (Bộ sưu tập hình ảnh sản phẩm)
CREATE TABLE IF NOT EXISTS product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    alt_text VARCHAR(255),
    sort_order INT DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Chỉ mục tối ưu truy vấn
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_brands_slug ON brands(slug);
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_brand_id ON products(brand_id);
CREATE INDEX IF NOT EXISTS idx_products_slug ON products(slug);
CREATE INDEX IF NOT EXISTS idx_products_featured ON products(is_featured);
CREATE INDEX IF NOT EXISTS idx_products_active_created ON products(is_active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_variants_sku ON product_variants(sku);
CREATE INDEX IF NOT EXISTS idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);

-- 6. Seed dữ liệu mẫu ban đầu cho ngành LEGO & Đồ trưng bày
INSERT INTO categories (name, slug, description, sort_order) VALUES
    ('LEGO Star Wars', 'lego-star-wars', 'Các bộ lắp ráp chủ đề Chiến Tranh Giữa Các Vì Sao kinh điển', 1),
    ('LEGO Technic', 'lego-technic', 'Các dòng siêu xe, máy móc cơ khí chuyển động phức tạp', 2),
    ('LEGO Icons & Creator Expert', 'lego-icons', 'Các công trình kiến trúc, biểu tượng nghệ thuật cao cấp', 3),
    ('Mô hình & Đồ trưng bày', 'mo-hinh-trung-bay', 'Mô hình tĩnh, figure và diorama trưng bày', 4),
    ('Hộp mica & Phụ kiện LED', 'hop-mica-phu-kien', 'Hộp chống bụi cao cấp và bộ đèn LED trang trí', 5)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO brands (name, slug, description, logo_url) VALUES
    ('LEGO', 'lego', 'Tập đoàn sản xuất đồ chơi gạch lắp ráp hàng đầu thế giới từ Đan Mạch', 'https://example.com/lego-logo.png'),
    ('CaDA', 'cada', 'Thương hiệu gạch mô hình cơ khí điều khiển từ xa cao cấp', 'https://example.com/cada-logo.png'),
    ('Mould King', 'mould-king', 'Thương hiệu mô hình lắp ghép chi tiết lớn dành cho dân sưu tầm', 'https://example.com/mould-king-logo.png'),
    ('BrickDisplay', 'brick-display', 'Xưởng gia công hộp mica acrylic cao cấp chống tia UV', 'https://example.com/brickdisplay-logo.png')
ON CONFLICT (slug) DO NOTHING;

-- Seed sản phẩm mẫu: LEGO Star Wars Millennium Falcon 75192
INSERT INTO products (id, category_id, brand_id, name, slug, description, short_description, base_price, thumbnail_url, is_featured, avg_rating, review_count, sold_count)
VALUES (
    1,
    (SELECT id FROM categories WHERE slug = 'lego-star-wars'),
    (SELECT id FROM brands WHERE slug = 'lego'),
    'LEGO Star Wars 75192 Millennium Falcon UCS',
    'lego-star-wars-75192-millennium-falcon-ucs',
    'Siêu phẩm mô hình LEGO lớn nhất mọi thời đại với hơn 7541 mảnh ghép chi tiết, thuộc dòng Ultimate Collector Series.',
    'Bộ LEGO Millennium Falcon UCS 7541 chi tiết cao cấp nhất dành cho dân sưu tầm.',
    23500000.00,
    'https://example.com/falcon-thumb.jpg',
    TRUE,
    5.0,
    18,
    42
) ON CONFLICT (slug) DO NOTHING;

-- Seed các biến thể cho Millennium Falcon 75192
INSERT INTO product_variants (product_id, sku, name, price, compare_price, stock_quantity, attributes, image_url)
VALUES 
    (1, 'LEGO-75192-STD', 'Bản Tiêu Chuẩn (Nguyên Hộp)', 23500000.00, 25000000.00, 5, '{"edition": "Standard", "packaging": "Original Box"}'::jsonb, 'https://example.com/falcon-std.jpg'),
    (1, 'LEGO-75192-MICA', 'Bản Kèm Hộp Mica Trưng Bày Đèn LED', 26500000.00, 28000000.00, 3, '{"edition": "Collector Bundle", "packaging": "With UV Acrylic Display & LED Kit"}'::jsonb, 'https://example.com/falcon-bundle.jpg')
ON CONFLICT (sku) DO NOTHING;

-- Seed hình ảnh cho Millennium Falcon 75192
INSERT INTO product_images (product_id, image_url, alt_text, sort_order, is_primary)
VALUES
    (1, 'https://example.com/falcon-primary.jpg', 'Ảnh chính diện phi thuyền Millennium Falcon', 1, TRUE),
    (1, 'https://example.com/falcon-interior.jpg', 'Chi tiết nội thất buồng lái và phòng họp bên trong phi thuyền', 2, FALSE)
ON CONFLICT DO NOTHING;

-- Cập nhật lại Sequence cho id của bảng products nếu có
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));

