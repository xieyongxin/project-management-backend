-- PM-0028: permission for editing requirement title and body content.
-- Safe to run repeatedly; this script only registers the permission.

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT
    '项目需求内容编辑', 0, 5, '', NULL, NULL, '', 1, 0,
    'F', '0', '0', 'project:requirement:edit', '#', 'admin', SYSDATE(), '', NULL, '项目需求标题和正文内容编辑权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'project:requirement:edit'
);
