package com.ruoyi.framework.web.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.service.ISysRoleService;

class TokenServiceTest
{
    @Test
    void refreshPermissionReloadsRoleStatusBeforeCalculatingPermissions()
    {
        RedisCache redisCache = org.mockito.Mockito.mock(RedisCache.class);
        ISysRoleService roleService = org.mockito.Mockito.mock(ISysRoleService.class);
        SysPermissionService permissionService = org.mockito.Mockito.mock(SysPermissionService.class);
        TokenService tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "redisCache", redisCache);
        ReflectionTestUtils.setField(tokenService, "roleService", roleService);
        ReflectionTestUtils.setField(tokenService, "expireTime", 30);

        SysRole cachedRole = role(7L, "0");
        SysRole currentRole = role(7L, "1");
        SysUser user = new SysUser();
        user.setUserId(23L);
        user.setUserName("member");
        user.setRoles(List.of(cachedRole));
        LoginUser loginUser = new LoginUser(user, Set.of("stale:permission"));
        loginUser.setUserId(23L);
        loginUser.setToken("token-1");
        List<SysRole> currentRoles = List.of(currentRole);
        when(redisCache.keys("login_tokens:*")).thenReturn(Set.of("login_tokens:token-1"));
        when(redisCache.getCacheObject("login_tokens:token-1")).thenReturn(loginUser);
        when(roleService.selectRolesByUserId(23L)).thenReturn(currentRoles);
        when(permissionService.getMenuPermission(user)).thenReturn(Set.of());

        tokenService.refreshPermissionByRoleId(7L, permissionService);

        assertSame(currentRoles, loginUser.getUser().getRoles());
        verify(roleService).selectRolesByUserId(23L);
        verify(permissionService).getMenuPermission(user);
        verify(redisCache).setCacheObject(org.mockito.ArgumentMatchers.eq("login_tokens:token-1"),
            org.mockito.ArgumentMatchers.same(loginUser), org.mockito.ArgumentMatchers.eq(30),
            org.mockito.ArgumentMatchers.any());
    }

    private SysRole role(Long roleId, String status)
    {
        SysRole role = new SysRole(roleId);
        role.setRoleKey("common");
        role.setStatus(status);
        return role;
    }
}
