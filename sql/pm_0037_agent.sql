-- PM-0037: configurable Agent calls and auditable drafts.
CREATE TABLE IF NOT EXISTS pm_agent_call (
    call_id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    requirement_id BIGINT NOT NULL,
    requirement_version_id BIGINT NOT NULL,
    initiator_id BIGINT NOT NULL,
    provider VARCHAR(100) NOT NULL,
    model VARCHAR(200) NOT NULL,
    external_enabled TINYINT(1) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    selected_attachment_snapshot TEXT NULL,
    input_content LONGTEXT NULL,
    parsed_attachment_content LONGTEXT NULL,
    draft_tasks LONGTEXT NULL,
    error_message VARCHAR(2000) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (call_id),
    UNIQUE KEY uk_pm_agent_call_idempotency (project_id, initiator_id, idempotency_key),
    KEY idx_pm_agent_call_requirement (requirement_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC;

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT '项目Agent拆分',0,7,'',NULL,NULL,'',1,0,'F','0','0','project:agent:split','#','admin',SYSDATE(),'admin',SYSDATE(),'项目Agent拆分权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='project:agent:split');
INSERT INTO sys_menu (menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT '项目Agent日志',0,8,'',NULL,NULL,'',1,0,'F','0','0','project:agent:log','#','admin',SYSDATE(),'admin',SYSDATE(),'项目Agent调用记录查看权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='project:agent:log');

INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent服务商','project.agent.provider','local','Y','admin',SYSDATE(),'Agent适配器服务商'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.provider');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent模型','project.agent.model','local-draft-v1','Y','admin',SYSDATE(),'Agent适配器模型'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.model');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent允许外发','project.agent.external.enabled','false','Y','admin',SYSDATE(),'是否允许发送需求内容至外部服务'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.external.enabled');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent密钥','project.agent.api-key','','Y','admin',SYSDATE(),'外部Agent服务密钥，由部署环境配置，不写入调用记录'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.api-key');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent文件解析','project.agent.file.parse.enabled','true','Y','admin',SYSDATE(),'是否解析选中的需求附件内容'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.file.parse.enabled');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent解析最大字符数','project.agent.file.parse.max-chars','20000','Y','admin',SYSDATE(),'单个附件写入调用记录的最大解析字符数'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.file.parse.max-chars');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent超时毫秒','project.agent.timeout-ms','30000','Y','admin',SYSDATE(),'外部Agent请求超时配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.timeout-ms');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent最大重试次数','project.agent.retry.max','0','Y','admin',SYSDATE(),'Agent失败后的最大重试次数'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.retry.max');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent幂等开关','project.agent.idempotency.enabled','true','Y','admin',SYSDATE(),'是否按项目、用户和幂等键防止重复调用'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.idempotency.enabled');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent每分钟限流','project.agent.rate-limit-per-minute','60','Y','admin',SYSDATE(),'单用户每分钟最大Agent调用次数'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.rate-limit-per-minute');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent费用上限','project.agent.cost-limit','0','Y','admin',SYSDATE(),'费用上限，0表示由外部供应商或部署策略控制'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.cost-limit');
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Agent日志留存天数','project.agent.log-retention-days','0','Y','admin',SYSDATE(),'调用记录留存天数，0表示不自动清理'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='project.agent.log-retention-days');
