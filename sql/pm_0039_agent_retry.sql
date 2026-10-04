-- PM-0038: preserve Agent failure snapshots and retry lineage.
-- Safe to run repeatedly; existing calls remain readable and are never rewritten.

SET @pm0038_retry_of_call = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE pm_agent_call ADD COLUMN retry_of_call_id BIGINT NULL COMMENT ''重试来源调用ID'' AFTER initiator_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pm_agent_call' AND column_name = 'retry_of_call_id'
);
PREPARE pm0038_stmt FROM @pm0038_retry_of_call;
EXECUTE pm0038_stmt;
DEALLOCATE PREPARE pm0038_stmt;

SET @pm0038_retry_count = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE pm_agent_call ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT ''已执行的人工重试次数'' AFTER retry_of_call_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pm_agent_call' AND column_name = 'retry_count'
);
PREPARE pm0038_stmt FROM @pm0038_retry_count;
EXECUTE pm0038_stmt;
DEALLOCATE PREPARE pm0038_stmt;

SET @pm0038_input_title = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE pm_agent_call ADD COLUMN input_title VARCHAR(255) NULL COMMENT ''发送时需求标题快照'' AFTER retry_count',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pm_agent_call' AND column_name = 'input_title'
);
PREPARE pm0038_stmt FROM @pm0038_input_title;
EXECUTE pm0038_stmt;
DEALLOCATE PREPARE pm0038_stmt;
