-- PM-0008: register the project-scoped operation log query permission.
-- Safe to run repeatedly; does not grant the permission to any role.

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT
    '查询项目操作日志', 0, 2, '', NULL, NULL, '', 1, 0,
    'F', '0', '0', 'project:log:list', '#', 'admin', SYSDATE(), '', NULL, '项目操作日志查询权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'project:log:list'
);
