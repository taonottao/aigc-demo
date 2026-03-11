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

INSERT INTO sys_org (id, parent_id, name, code, leader, description, sort_no, status)
VALUES (1, 0, '集团总部', 'HQ', '系统管理员', '默认根组织', 1, 1)
ON CONFLICT (id) DO NOTHING;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255),
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_role_status ON sys_role(status);

-- 兼容旧库升级：补齐缺失字段
ALTER TABLE IF EXISTS sys_role
    ADD COLUMN IF NOT EXISTS description VARCHAR(255);

-- 用户-角色关联（多对多）
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user_id FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_user_role_role_id FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sys_user_role_role_id ON sys_user_role(role_id);

-- 菜单/按钮权限
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(128) NOT NULL,
    path VARCHAR(255),
    icon VARCHAR(64),
    perm_code VARCHAR(128),
    type VARCHAR(16) NOT NULL DEFAULT 'MENU', -- MENU/BUTTON
    sort_no INT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_menu_parent_id ON sys_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_menu_status ON sys_menu(status);

-- 角色-菜单/按钮关联
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_sys_role_menu_role_id FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_menu_menu_id FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sys_role_menu_menu_id ON sys_role_menu(menu_id);

-- 角色数据权限（按模块）
CREATE TABLE IF NOT EXISTS sys_role_data_scope (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    module_code VARCHAR(64) NOT NULL,
    scope VARCHAR(32) NOT NULL, -- ALL/ORG_AND_CHILDREN/ORG_ONLY/SELF_ONLY
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_data_scope UNIQUE (role_id, module_code),
    CONSTRAINT fk_sys_role_data_scope_role_id FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
);

-- 登录日志
CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    success BOOLEAN NOT NULL,
    ip VARCHAR(64),
    user_agent VARCHAR(255),
    message VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_login_log_created_at ON sys_login_log(created_at);

-- 操作日志（关键操作审计）
CREATE TABLE IF NOT EXISTS sys_op_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    module VARCHAR(64),
    action VARCHAR(64),
    detail VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_op_log_created_at ON sys_op_log(created_at);

-- 初始化：基础角色
INSERT INTO sys_role (id, name, code, description, status)
VALUES
    (1, '超级管理员', 'ADMIN', '系统最高权限', 1),
    (2, '部门主管', 'MANAGER', '部门范围管理', 1),
    (3, '审计员', 'AUDITOR', '只读审计', 1)
ON CONFLICT (id) DO NOTHING;

-- 初始化：基础菜单（用于前端菜单/按钮显隐）
INSERT INTO sys_menu (id, parent_id, name, path, icon, perm_code, type, sort_no, status)
VALUES
    (1, 0, '仪表盘', '/dashboard', 'dashboard', NULL, 'MENU', 1, 1),
    (2, 0, '组织管理', '/organizations', 'org', 'org:view', 'MENU', 2, 1),
    (3, 0, '用户管理', '/users', 'user', 'user:view', 'MENU', 3, 1),
    (4, 0, '角色权限', '/role-permission', 'role', 'role:view', 'MENU', 4, 1),
    (5, 0, '日志与安全', '/security', 'security', 'security:view', 'MENU', 5, 1),
    (101, 3, '新增用户', NULL, NULL, 'user:add', 'BUTTON', 1, 1),
    (102, 3, '编辑用户', NULL, NULL, 'user:edit', 'BUTTON', 2, 1),
    (103, 3, '删除用户', NULL, NULL, 'user:delete', 'BUTTON', 3, 1),
    (104, 3, '导入用户', NULL, NULL, 'user:import', 'BUTTON', 4, 1),
    (105, 3, '导出用户', NULL, NULL, 'user:export', 'BUTTON', 5, 1),
    (201, 2, '新增组织', NULL, NULL, 'org:add', 'BUTTON', 1, 1),
    (202, 2, '编辑组织', NULL, NULL, 'org:edit', 'BUTTON', 2, 1),
    (203, 2, '排序组织', NULL, NULL, 'org:sort', 'BUTTON', 3, 1),
    (204, 2, '删除组织', NULL, NULL, 'org:delete', 'BUTTON', 4, 1),
    (301, 4, '新增角色', NULL, NULL, 'role:add', 'BUTTON', 1, 1),
    (302, 4, '分配用户', NULL, NULL, 'role:assign', 'BUTTON', 2, 1),
    (303, 4, '保存权限绑定', NULL, NULL, 'perm:save', 'BUTTON', 3, 1)
ON CONFLICT (id) DO NOTHING;

-- 初始化：超级管理员绑定全部菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 初始化：部门主管默认拥有基础菜单与用户管理按钮
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
    (2, 1), (2, 2), (2, 3), (2, 4),
    (2, 101), (2, 102), (2, 105),
    (2, 201), (2, 202), (2, 203)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 初始化：审计员只读查看（无按钮）
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (3, 1), (3, 2), (3, 3), (3, 4), (3, 5)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 初始化：数据权限（按模块 USER）
INSERT INTO sys_role_data_scope (role_id, module_code, scope)
VALUES
    (1, 'USER', 'ALL'),
    (2, 'USER', 'ORG_AND_CHILDREN'),
    (3, 'USER', 'ORG_ONLY')
ON CONFLICT (role_id, module_code) DO NOTHING;

-- 初始化：默认用户（兼容旧库里已存在 id=1 的情况；密码为明文 123456，首次登录会自动升级为 BCrypt）
UPDATE sys_user
SET username = 'smile-admin',
    password = '123456',
    real_name = '系统管理员',
    phone = '13800138000',
    email = 'smile-admin@example.com',
    avatar = '',
    org_id = 1,
    status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

INSERT INTO sys_user (id, username, password, real_name, phone, email, avatar, org_id, status)
VALUES (1, 'smile-admin', '123456', '系统管理员', '13800138000', 'smile-admin@example.com', '', 1, 1)
ON CONFLICT (id) DO NOTHING;

-- 初始化：默认用户角色
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 对齐自增序列，避免初始化脚本显式插入 ID 后出现主键冲突
SELECT setval(pg_get_serial_sequence('sys_org', 'id'), COALESCE((SELECT MAX(id) FROM sys_org), 1), true);
SELECT setval(pg_get_serial_sequence('sys_user', 'id'), COALESCE((SELECT MAX(id) FROM sys_user), 1), true);
SELECT setval(pg_get_serial_sequence('sys_role', 'id'), COALESCE((SELECT MAX(id) FROM sys_role), 1), true);
SELECT setval(pg_get_serial_sequence('sys_menu', 'id'), COALESCE((SELECT MAX(id) FROM sys_menu), 1), true);
SELECT setval(pg_get_serial_sequence('sys_role_data_scope', 'id'), COALESCE((SELECT MAX(id) FROM sys_role_data_scope), 1), true);
SELECT setval(pg_get_serial_sequence('sys_login_log', 'id'), COALESCE((SELECT MAX(id) FROM sys_login_log), 1), true);
SELECT setval(pg_get_serial_sequence('sys_op_log', 'id'), COALESCE((SELECT MAX(id) FROM sys_op_log), 1), true);
