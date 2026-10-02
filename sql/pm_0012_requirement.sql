-- PM-0012: create project requirements with owners and an initial content version.
-- Safe to run repeatedly; no default requirement statuses are inserted.

CREATE TABLE IF NOT EXISTS pm_requirement (
    requirement_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '需求ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    creator_id BIGINT NOT NULL COMMENT '创建者用户ID',
    current_version_id BIGINT NULL COMMENT '当前内容版本ID',
    status VARCHAR(100) NOT NULL COMMENT '需求状态字典值',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (requirement_id),
    KEY idx_pm_requirement_project_time (project_id, is_deleted, create_time, requirement_id),
    KEY idx_pm_requirement_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='项目需求表';

CREATE TABLE IF NOT EXISTS pm_requirement_version (
    version_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '需求版本ID',
    requirement_id BIGINT NOT NULL COMMENT '需求ID',
    version_no INT NOT NULL COMMENT '需求版本号',
    title VARCHAR(255) NOT NULL COMMENT '版本标题',
    content LONGTEXT NOT NULL COMMENT '版本正文',
    attachment_snapshot TEXT NULL COMMENT '附件清单快照JSON',
    created_by BIGINT NOT NULL COMMENT '版本创建者用户ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (version_id),
    UNIQUE KEY uk_pm_requirement_version (requirement_id, version_no),
    KEY idx_pm_requirement_version_requirement (requirement_id, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='需求内容版本表';

CREATE TABLE IF NOT EXISTS pm_requirement_owner (
    requirement_id BIGINT NOT NULL COMMENT '需求ID',
    user_id BIGINT NOT NULL COMMENT '负责人用户ID',
    PRIMARY KEY (requirement_id, user_id),
    KEY idx_pm_requirement_owner_user (user_id, requirement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='需求负责人关系表';

INSERT INTO sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '需求状态', 'pm_requirement_status', '0', 'admin', SYSDATE(), '项目需求状态列表'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type WHERE dict_type = 'pm_requirement_status'
);

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT
    '项目需求查询', 0, 1, '', NULL, NULL, '', 1, 0,
    'F', '0', '0', 'project:requirement:list', '#', 'admin', SYSDATE(), '', NULL, '项目需求查询权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'project:requirement:list'
);

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT
    '项目需求新增', 0, 2, '', NULL, NULL, '', 1, 0,
    'F', '0', '0', 'project:requirement:add', '#', 'admin', SYSDATE(), '', NULL, '项目需求新增权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'project:requirement:add'
);
