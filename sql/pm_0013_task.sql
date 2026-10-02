-- PM-0013: create project tasks with a requirement version, categories and owners.
-- Safe to run repeatedly; no default task statuses or categories are inserted.

CREATE TABLE IF NOT EXISTS pm_task (
    task_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    requirement_id BIGINT NOT NULL COMMENT '所属需求ID',
    creator_id BIGINT NOT NULL COMMENT '创建者用户ID',
    current_version_id BIGINT NULL COMMENT '当前任务版本ID',
    status VARCHAR(100) NOT NULL COMMENT '任务状态字典值',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (task_id),
    KEY idx_pm_task_project_time (project_id, is_deleted, create_time, task_id),
    KEY idx_pm_task_requirement (requirement_id, is_deleted),
    KEY idx_pm_task_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='项目任务表';

CREATE TABLE IF NOT EXISTS pm_task_version (
    version_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务版本ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    version_no INT NOT NULL COMMENT '任务版本号',
    title VARCHAR(255) NOT NULL COMMENT '任务标题',
    description LONGTEXT NOT NULL COMMENT '任务说明',
    requirement_version_id BIGINT NOT NULL COMMENT '依据的需求版本ID',
    created_by BIGINT NOT NULL COMMENT '版本创建者用户ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (version_id),
    UNIQUE KEY uk_pm_task_version (task_id, version_no),
    KEY idx_pm_task_version_requirement (requirement_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='任务内容版本表';

CREATE TABLE IF NOT EXISTS pm_task_category (
    task_id BIGINT NOT NULL COMMENT '任务ID',
    category_value VARCHAR(100) NOT NULL COMMENT '任务分类字典值',
    PRIMARY KEY (task_id, category_value),
    KEY idx_pm_task_category_value (category_value, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='任务分类关系表';

CREATE TABLE IF NOT EXISTS pm_task_owner (
    task_id BIGINT NOT NULL COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '负责人用户ID',
    PRIMARY KEY (task_id, user_id),
    KEY idx_pm_task_owner_user (user_id, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='任务负责人关系表';

INSERT INTO sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '任务状态', 'pm_task_status', '0', 'admin', SYSDATE(), '项目任务状态列表'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type WHERE dict_type = 'pm_task_status'
);

INSERT INTO sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '任务分类', 'pm_task_category', '0', 'admin', SYSDATE(), '项目任务分类列表'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type WHERE dict_type = 'pm_task_category'
);

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT
    '项目任务新增', 0, 1, '', NULL, NULL, '', 1, 0,
    'F', '0', '0', 'project:task:add', '#', 'admin', SYSDATE(), '', NULL, '项目任务新增权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'project:task:add'
);
