-- PM-0016 -- project task list permission.
-- Safe to run repeatedly; this script does not alter business tables.

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT
    '项目任务查看', 0, 2, '', NULL, NULL, '', 1, 0,
    'F', '0', '0', 'project:task:list', '#', 'admin', SYSDATE(), '', NULL, '项目任务列表查看权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'project:task:list'
);
