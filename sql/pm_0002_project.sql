-- PM-0002: project creation and project access isolation
-- Safe to run repeatedly; does not reset or modify existing business data.

CREATE TABLE IF NOT EXISTS pm_project (
    project_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    project_name VARCHAR(255) NOT NULL COMMENT '项目名称',
    project_name_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '规范化项目名称',
    creator_id BIGINT NOT NULL COMMENT '创建者用户ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (project_id),
    UNIQUE KEY uk_pm_project_name_key (project_name_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='项目表';

CREATE TABLE IF NOT EXISTS pm_project_member (
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NULL COMMENT '若依全局角色ID',
    is_project_admin TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否项目管理员',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (project_id, user_id),
    KEY idx_pm_member_user_project (user_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='项目成员表';

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT
    '创建项目', 0, 1, '', NULL, NULL, '', 1, 0,
    'F', '0', '0', 'project:create', '#', 'admin', SYSDATE(), '', NULL, '创建项目权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'project:create'
);
