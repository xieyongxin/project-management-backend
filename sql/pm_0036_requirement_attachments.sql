-- PM-0036: private requirement-version attachments.
-- Safe to run repeatedly; old rows and files are never rewritten.

CREATE TABLE IF NOT EXISTS pm_requirement_attachment (
    attachment_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '附件ID',
    requirement_id BIGINT NOT NULL COMMENT '需求ID',
    version_id BIGINT NULL COMMENT '所属需求版本ID；版本生成后回填',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    storage_path VARCHAR(1000) NOT NULL COMMENT '私有存储相对路径',
    extension VARCHAR(20) NOT NULL COMMENT '文件扩展名',
    content_type VARCHAR(255) NULL COMMENT '媒体类型',
    file_size BIGINT NOT NULL COMMENT '文件字节数',
    created_by BIGINT NOT NULL COMMENT '上传用户ID',
    create_time DATETIME NOT NULL COMMENT '上传时间',
    PRIMARY KEY (attachment_id),
    KEY idx_pm_requirement_attachment_requirement (requirement_id, version_id),
    KEY idx_pm_requirement_attachment_version (version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='需求版本附件表';
