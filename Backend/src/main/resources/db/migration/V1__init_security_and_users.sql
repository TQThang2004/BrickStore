-- V1: Khởi tạo bảng Người dùng, Phân quyền & Refresh Token

-- 1. Bảng Roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng Users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255),
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url TEXT,
    provider VARCHAR(20) DEFAULT 'LOCAL', -- LOCAL, GOOGLE, FACEBOOK
    provider_id VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng liên kết Users - Roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 4. Bảng Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Chỉ mục tối ưu truy vấn
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 5. Seed dữ liệu mặc định Roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Người dùng thông thường, mua sắm trên hệ thống'),
    ('ROLE_ADMIN', 'Quản trị viên hệ thống')
ON CONFLICT (name) DO NOTHING;

-- Seed tài khoản Quản trị viên mặc định (password: admin123, mã hóa BCrypt)
-- Hash: $2a$10$Du7lfqVnuL8AwYobYJNu2OQ3pHnvIFtnOXU0ridKVJDqv8/bo7hsq
INSERT INTO users (email, password, full_name, phone, provider, is_active)
VALUES (
    'admin@brickstore.com',
    '$2a$10$Du7lfqVnuL8AwYobYJNu2OQ3pHnvIFtnOXU0ridKVJDqv8/bo7hsq', -- admin123
    'BrickStore Administrator',
    '0901234567',
    'LOCAL',
    TRUE
) ON CONFLICT (email) DO NOTHING;

-- Gán quyền ADMIN cho tài khoản admin mặc định
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@brickstore.com' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

