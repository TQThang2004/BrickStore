-- ==============================================================================
-- BRICKSTORE - MIGRATION V3: INVENTORY, STOCK RESERVATIONS & LOGS
-- ==============================================================================

-- 1. Bảng inventory (Quản lý tồn kho chi tiết theo biến thể SKU)
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    variant_id BIGINT NOT NULL UNIQUE REFERENCES product_variants(id) ON DELETE CASCADE,
    available_stock INT NOT NULL DEFAULT 0 CHECK (available_stock >= 0),
    reserved_stock INT NOT NULL DEFAULT 0 CHECK (reserved_stock >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_variant ON inventory(variant_id);

-- 2. Bảng stock_reservations (Giữ hàng tạm thời khi khách checkout, hết hạn sau X phút)
CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity > 0),
    order_reference VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'RESERVED', -- RESERVED, COMMITTED, RELEASED
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reservations_status_expires ON stock_reservations (status, expires_at);
CREATE INDEX idx_reservations_variant ON stock_reservations (variant_id);
CREATE INDEX idx_reservations_order_ref ON stock_reservations (order_reference);

-- 3. Bảng inventory_logs (Ghi nhận lịch sử biến động kho hàng)
CREATE TABLE inventory_logs (
    id BIGSERIAL PRIMARY KEY,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    change_quantity INT NOT NULL,
    previous_stock INT NOT NULL,
    current_stock INT NOT NULL,
    action_type VARCHAR(50) NOT NULL, -- RESTOCK, RESERVE, RELEASE, COMMIT, MANUAL_ADJUST
    reference_id VARCHAR(100),
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_logs_variant ON inventory_logs (variant_id, created_at DESC);

-- 4. Đồng bộ dữ liệu ban đầu từ product_variants sang inventory
INSERT INTO inventory (variant_id, available_stock, reserved_stock, version)
SELECT id, stock_quantity, 0, 0
FROM product_variants
ON CONFLICT (variant_id) DO NOTHING;

-- 5. Ghi nhận log khởi tạo ban đầu
INSERT INTO inventory_logs (variant_id, change_quantity, previous_stock, current_stock, action_type, note)
SELECT id, stock_quantity, 0, stock_quantity, 'RESTOCK', 'Khởi tạo tồn kho ban đầu từ danh mục SKU'
FROM product_variants
WHERE stock_quantity > 0;

