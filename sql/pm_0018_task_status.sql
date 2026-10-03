-- PM-0018: allow authorized project members to update task status.
-- Safe to run repeatedly; this script only registers the permission.

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT
    '项目任务状态修改', 0, 3, '', NULL, NULL, '', 1, 0,
    'F', '0', '0', 'project:task:status', '#', 'admin', SYSDATE(), '', NULL, '项目任务状态修改权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'project:task:status'
);
