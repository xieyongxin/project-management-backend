-- PM-0003: enable self-registration by default
-- Safe to run repeatedly; does not reset users or other configuration.

UPDATE sys_config
SET config_value = 'true',
    update_time = SYSDATE(),
    update_by = 'migration'
WHERE config_key = 'sys.account.registerUser'
  AND config_value <> 'true';
