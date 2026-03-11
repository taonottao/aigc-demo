-- =========================================
-- 用户权限管理系统初始化脚本（PostgreSQL）
-- =========================================

-- 1. 创建数据库（UTF8 编码）
CREATE DATABASE "user-management"
    WITH ENCODING 'UTF8'
    TEMPLATE template0;

-- 2. 切换到业务库后执行后续语句
-- \c "user-management"

-- 3. 创建组织表（树形结构）
CREATE TABLE IF NOT EXISTS sys_org (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL UNIQUE,
    leader VARCHAR(64),
    description VARCHAR(255),
    sort_no INT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_org_parent_id ON sys_org(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_org_status ON sys_org(status);

-- 4. 创建用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32),
    email VARCHAR(128),
    avatar VARCHAR(255),
    org_id BIGINT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sys_user_org_id FOREIGN KEY (org_id) REFERENCES sys_org(id)
);

CREATE INDEX IF NOT EXISTS idx_sys_user_org_id ON sys_user(org_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_status ON sys_user(status);

-- 5. 创建角色表与关联表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL UNIQUE,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_role_status ON sys_role(status);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user_id FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_role_role_id FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE INDEX IF NOT EXISTS idx_sys_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_role_id ON sys_user_role(role_id);

-- 6. 初始化组织数据
INSERT INTO sys_org (id, parent_id, name, code, leader, description, sort_no, status)
VALUES
    (1, 0, '集团总部', 'HQ', '系统管理员', '默认根组织', 1, 1)
ON CONFLICT (id) DO NOTHING;

-- 7. 初始化角色数据
INSERT INTO sys_role (id, name, code, status)
VALUES
    (1, '超级管理员', 'ADMIN', 1),
    (2, '部门主管', 'MANAGER', 1),
    (3, '普通员工', 'STAFF', 1)
ON CONFLICT (id) DO NOTHING;

-- 8. 初始化用户数据
INSERT INTO sys_user (username, password, real_name, phone, email, avatar, org_id, status)
VALUES ('smile', '123456', '张三', '13800138000', 'smile@example.com', '', 1, 1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id)
SELECT id, 1 FROM sys_user WHERE username = 'smile'
ON CONFLICT DO NOTHING;
