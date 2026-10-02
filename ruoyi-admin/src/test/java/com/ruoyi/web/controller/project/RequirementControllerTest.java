package com.ruoyi.web.controller.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.system.domain.RequirementOwner;
import com.ruoyi.system.service.IRequirementService;
import com.ruoyi.web.domain.project.RequirementCreateRequest;
import com.ruoyi.web.domain.project.RequirementView;

class RequirementControllerTest
{
    private final IRequirementService requirementService = org.mockito.Mockito.mock(IRequirementService.class);
    private final RequirementController controller = new RequirementController(requirementService);

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void requirementEndpointsDeclarePermissions() throws NoSuchMethodException
    {
        PreAuthorize list = RequirementController.class.getMethod("list", String.class)
            .getAnnotation(PreAuthorize.class);
        PreAuthorize create = RequirementController.class
            .getMethod("create", String.class, RequirementCreateRequest.class)
            .getAnnotation(PreAuthorize.class);
        assertNotNull(list);
        assertEquals("@ss.hasPermi('project:requirement:list')", list.value());
        assertNotNull(create);
        assertEquals("@ss.hasPermi('project:requirement:add')", create.value());
    }

    @Test
    void memberCanListRequirementsAndStatuses()
    {
        setCurrentUser(23L);
        Requirement requirement = new Requirement();
        requirement.setRequirementId(8L);
        requirement.setTitle("登录");
        RequirementOwner owner = new RequirementOwner();
        owner.setUserId(23L);
        owner.setUserName("alice");
        requirement.setOwners(List.of(owner));
        when(requirementService.selectRequirementsForUser(41L, 23L)).thenReturn(List.of(requirement));
        SysDictData status = new SysDictData();
        status.setDictValue("todo");
        status.setDictLabel("待处理");
        when(requirementService.selectActiveStatuses(41L, 23L)).thenReturn(List.of(status));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        ResponseEntity<?> listResponse = controller.list("41");
        ResponseEntity<?> statusResponse = controller.statuses("41");

        assertEquals(200, listResponse.getStatusCode().value());
        assertEquals(1, ((TableDataInfo) listResponse.getBody()).getTotal());
        assertEquals(200, statusResponse.getStatusCode().value());
        assertEquals(1, ((List<?>) ((java.util.Map<?, ?>) statusResponse.getBody()).get("data")).size());
        verify(requirementService).selectRequirementsForUser(41L, 23L);
        verify(requirementService).selectActiveStatuses(41L, 23L);
    }

    @Test
    void nonMemberListUsesHttp404AndMalformedIdDoesNotQueryService()
    {
        setCurrentUser(23L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(requirementService.selectRequirementsForUser(41L, 23L)).thenReturn(null);

        ResponseEntity<?> denied = controller.list("41");
        verify(requirementService).selectRequirementsForUser(41L, 23L);
        ResponseEntity<?> malformed = controller.list("bad");

        assertEquals(404, denied.getStatusCode().value());
        assertEquals(404, malformed.getStatusCode().value());
    }

    @Test
    void memberCanCreateRequirementAndServiceErrorsPropagate()
    {
        setCurrentUser(23L);
        Requirement requirement = new Requirement();
        requirement.setRequirementId(8L);
        requirement.setTitle("登录");
        when(requirementService.createRequirement(41L, 23L, "登录", "<p>正文</p>", "todo", List.of(23L)))
            .thenReturn(requirement);
        RequirementCreateRequest request = request();

        ResponseEntity<AjaxResult> response = controller.create("41", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(8L, ((RequirementView) response.getBody().get("data")).getRequirementId());

        doThrow(new ServiceException("归档项目不能开展新的业务操作", HttpStatus.BAD_REQUEST))
            .when(requirementService).createRequirement(41L, 23L, "登录", "<p>正文</p>", "todo", List.of(23L));
        ResponseEntity<AjaxResult> error = controller.create("41", request);
        assertEquals(400, error.getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST, error.getBody().get("code"));
    }

    @Test
    void requirementRequestRequiresTitleBodyStatusAndOwner()
    {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory())
        {
            var validator = factory.getValidator();
            RequirementCreateRequest request = new RequirementCreateRequest();
            assertEquals(4, validator.validate(request).size());
            request.setTitle("Title");
            request.setContent("Body");
            request.setStatus("todo");
            request.setOwnerIds(List.of(23L));
            assertEquals(0, validator.validate(request).size());
        }
    }

    @Test
    void requirementEndpointsRequirePermissionsWhenMethodSecurityIsEnabled()
    {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext context =
            new org.springframework.context.annotation.AnnotationConfigApplicationContext(SecurityConfiguration.class))
        {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
            RequirementController secured = context.getBean(RequirementController.class);
            setCurrentUser(23L, Set.of());
            assertThrows(AccessDeniedException.class, () -> secured.list("41"));
        }
    }

    private RequirementCreateRequest request()
    {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle("登录");
        request.setContent("<p>正文</p>");
        request.setStatus("todo");
        request.setOwnerIds(List.of(23L));
        return request;
    }

    private void setCurrentUser(Long userId)
    {
        setCurrentUser(userId, Set.of("project:requirement:list", "project:requirement:add"));
    }

    private void setCurrentUser(Long userId, Set<String> permissions)
    {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setPermissions(permissions);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, "", List.of()));
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class SecurityConfiguration
    {
        @org.springframework.context.annotation.Bean("ss")
        com.ruoyi.framework.web.service.PermissionService permissionService()
        {
            return new com.ruoyi.framework.web.service.PermissionService();
        }

        @org.springframework.context.annotation.Bean
        IRequirementService requirementService()
        {
            return org.mockito.Mockito.mock(IRequirementService.class);
        }

        @org.springframework.context.annotation.Bean
        RequirementController requirementController(IRequirementService requirementService)
        {
            return new RequirementController(requirementService);
        }
    }
}
