-- PM-0030: allow authorized project members to logically delete tasks.
-- Safe to run repeatedly; this script only registers the permission.

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT
    '项目任务删除', 0, 4, '', NULL, NULL, '', 1, 0,
    'F', '0', '0', 'project:task:delete', '#', 'admin', SYSDATE(), '', NULL, '项目任务逻辑删除权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'project:task:delete'
);
