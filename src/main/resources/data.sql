-- 初期ユーザーデータ（パスワードは "password123" をBCryptでハッシュ化したもの）
-- 管理者ユーザー
INSERT INTO users (user_name, password, role, created_at) 
VALUES ('admin', '$2a$10$tPonV4A9FFiYn4RnyU0Z6ejJGglYg.NsNLm.MwhqeftToZkgT8weK', 'ROLE_ADMIN', NOW())
ON CONFLICT (user_name) DO NOTHING;

-- 一般ユーザー
INSERT INTO users (user_name, password, role, created_at) 
VALUES ('user1', '$2a$10$tPonV4A9FFiYn4RnyU0Z6ejJGglYg.NsNLm.MwhqeftToZkgT8weK', 'ROLE_USER', NOW())
ON CONFLICT (user_name) DO NOTHING;

-- 初期商品データ
INSERT INTO products (name, description, price, created_at, version) 
VALUES 
    ('Spring Boot入門', 'Spring Bootの基礎から実践まで学べる入門書', 3000, NOW(), 0),
    ('Docker実践ガイド', 'Dockerを使ったコンテナ開発の実践的なガイド', 3500, NOW(), 0),
    ('React開発集中講座', 'モダンフロントエンド開発の集中講座', 4000, NOW(), 0)
ON CONFLICT DO NOTHING;

-- 初期在庫データ
INSERT INTO inventory (product_id, stock_quantity, updated_at, version)
SELECT id, 100, NOW(), 0
FROM products
ON CONFLICT (product_id) DO NOTHING;

