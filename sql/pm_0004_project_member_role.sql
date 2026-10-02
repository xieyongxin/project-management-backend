-- PM-0004: read-only project member roster
-- Safe to run repeatedly; preserves existing creator membership rows.

SET @pm0004_add_role_id = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE pm_project_member ADD COLUMN role_id BIGINT NULL COMMENT ''若依全局角色ID'' AFTER user_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pm_project_member'
      AND column_name = 'role_id'
);
PREPARE pm0004_stmt FROM @pm0004_add_role_id;
EXECUTE pm0004_stmt;
DEALLOCATE PREPARE pm0004_stmt;
