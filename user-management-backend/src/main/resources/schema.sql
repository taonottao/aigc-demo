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

INSERT INTO sys_org (id, parent_id, name, code, leader, description, sort_no, status)
VALUES (1, 0, '集团总部', 'HQ', '系统管理员', '默认根组织', 1, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_role (id, name, code, status)
VALUES
    (1, '超级管理员', 'ADMIN', 1),
    (2, '部门主管', 'MANAGER', 1),
    (3, '普通员工', 'STAFF', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id)
SELECT id, 1 FROM sys_user WHERE username = 'smile'
ON CONFLICT DO NOTHING;
