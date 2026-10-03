package com.ruoyi.web.controller.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.web.service.PermissionService;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysRoleService;

class SysRoleControllerTest
{
    private final ISysRoleService roleService = org.mockito.Mockito.mock(ISysRoleService.class);
    private final TokenService tokenService = org.mockito.Mockito.mock(TokenService.class);
    private final SysPermissionService permissionService = org.mockito.Mockito.mock(SysPermissionService.class);
    private final SysRoleController controller = new SysRoleController();

    SysRoleControllerTest()
    {
        ReflectionTestUtils.setField(controller, "roleService", roleService);
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
        ReflectionTestUtils.setField(controller, "permissionService", permissionService);
    }

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void successfulRoleStatusChangeRefreshesOnlinePermissions()
    {
        setCurrentUser("admin");
        SysRole role = role(7L, "1");
        doNothing().when(roleService).checkRoleAllowed(role);
        doNothing().when(roleService).checkRoleDataScope(7L);
        when(roleService.updateRoleStatus(role)).thenReturn(1);

        AjaxResult result = controller.changeStatus(role);

        assertEquals(200, result.get("code"));
        verify(tokenService).refreshPermissionByRoleId(7L, permissionService);
    }

    @Test
    void failedRoleStatusChangeDoesNotRefreshPermissions()
    {
        setCurrentUser("admin");
        SysRole role = role(7L, "1");
        when(roleService.updateRoleStatus(role)).thenReturn(0);

        AjaxResult result = controller.changeStatus(role);

        assertEquals(500, result.get("code"));
        verify(tokenService, never()).refreshPermissionByRoleId(7L, permissionService);
    }

    private SysRole role(Long roleId, String status)
    {
        SysRole role = new SysRole(roleId);
        role.setStatus(status);
        return role;
    }

    private void setCurrentUser(String userName)
    {
        SysUser user = new SysUser();
        user.setUserName(userName);
        LoginUser loginUser = new LoginUser(user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, "", List.of()));
    }
}
