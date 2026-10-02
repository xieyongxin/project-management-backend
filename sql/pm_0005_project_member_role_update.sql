-- PM-0005: project administrator assigns a global role to an existing member.
-- Safe to run repeatedly; does not alter existing member assignments.

CREATE TABLE IF NOT EXISTS pm_project_operation_log (
    log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目操作日志ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    operator_id BIGINT NOT NULL COMMENT '操作人用户ID',
    target_user_id BIGINT NULL COMMENT '被操作成员用户ID',
    previous_role_id BIGINT NULL COMMENT '变更前全局角色ID',
    new_role_id BIGINT NULL COMMENT '变更后全局角色ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    detail VARCHAR(1000) NULL COMMENT '操作说明',
    create_time DATETIME NOT NULL COMMENT '操作时间',
    PRIMARY KEY (log_id),
    KEY idx_pm_project_log_project_time (project_id, create_time, log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='项目操作日志表';
