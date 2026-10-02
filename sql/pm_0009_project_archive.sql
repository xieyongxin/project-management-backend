-- PM-0009: add project archive status.
-- Safe to run repeatedly; preserves existing projects and defaults them to ACTIVE.

SET @pm0009_add_status = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE pm_project ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT ''ACTIVE'' COMMENT ''项目状态'' AFTER creator_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pm_project'
      AND column_name = 'status'
);
PREPARE pm0009_stmt FROM @pm0009_add_status;
EXECUTE pm0009_stmt;
DEALLOCATE PREPARE pm0009_stmt;
