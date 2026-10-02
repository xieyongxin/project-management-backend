-- PM-0004: read-only project member roster
-- Safe to run repeatedly; preserves existing creator membership rows.

ALTER TABLE pm_project_member
    ADD COLUMN IF NOT EXISTS role_id BIGINT NULL COMMENT '若依全局角色ID' AFTER user_id;
