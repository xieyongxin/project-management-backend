package com.ruoyi.web.controller.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.system.domain.RequirementVersion;
import com.ruoyi.system.service.IRequirementService;
import com.ruoyi.web.domain.project.RequirementCreateRequest;
import com.ruoyi.web.domain.project.RequirementStatusUpdateRequest;
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

        PreAuthorize detail = RequirementController.class.getMethod("detail", String.class, String.class)
            .getAnnotation(PreAuthorize.class);
        PreAuthorize versions = RequirementController.class.getMethod("versions", String.class, String.class)
            .getAnnotation(PreAuthorize.class);
        PreAuthorize compareVersions = RequirementController.class
            .getMethod("compareVersions", String.class, String.class, String.class, String.class)
            .getAnnotation(PreAuthorize.class);
        assertNotNull(detail);
        assertEquals("@ss.hasPermi('project:requirement:list')", detail.value());
        assertNotNull(versions);
        assertEquals("@ss.hasPermi('project:requirement:list')", versions.value());
        assertNotNull(compareVersions);
        assertEquals("@ss.hasPermi('project:requirement:list')", compareVersions.value());
        PreAuthorize updateStatus = RequirementController.class
            .getMethod("updateStatus", String.class, String.class, RequirementStatusUpdateRequest.class)
            .getAnnotation(PreAuthorize.class);
        assertNotNull(updateStatus);
        assertEquals("@ss.hasPermi('project:requirement:status')", updateStatus.value());
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
    void memberCanReadRequirementDetailAndVersionsAndNonMemberGets404()
    {
        setCurrentUser(23L);
        Requirement requirement = new Requirement();
        requirement.setRequirementId(8L);
        requirement.setCurrentVersionId(18L);
        requirement.setTitle("登录");
        RequirementVersion version = new RequirementVersion();
        version.setVersionId(18L);
        version.setRequirementId(8L);
        version.setVersionNo(1);
        version.setTitle("登录");
        version.setContent("正文");
        when(requirementService.selectRequirementForUser(41L, 8L, 23L)).thenReturn(requirement);
        when(requirementService.selectRequirementVersionsForUser(41L, 8L, 23L)).thenReturn(List.of(version));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        ResponseEntity<?> detail = controller.detail("41", "8");
        ResponseEntity<?> versions = controller.versions("41", "8");

        assertEquals(200, detail.getStatusCode().value());
        assertEquals(8L, ((RequirementView) ((java.util.Map<?, ?>) detail.getBody()).get("data"))
            .getRequirementId());
        assertEquals(200, versions.getStatusCode().value());
        assertEquals(1, ((List<?>) ((java.util.Map<?, ?>) versions.getBody()).get("data")).size());

        when(requirementService.selectRequirementForUser(41L, 8L, 23L)).thenReturn(null);
        when(requirementService.selectRequirementVersionsForUser(41L, 8L, 23L)).thenReturn(null);
        assertEquals(404, controller.detail("41", "8").getStatusCode().value());
        assertEquals(404, controller.versions("41", "8").getStatusCode().value());
        verify(requirementService, org.mockito.Mockito.times(2)).selectRequirementForUser(41L, 8L, 23L);
        verify(requirementService, org.mockito.Mockito.times(2)).selectRequirementVersionsForUser(41L, 8L, 23L);
    }

    @Test
    void memberCanCompareTwoRequirementVersionsAndNonMemberGets404()
    {
        setCurrentUser(23L);
        RequirementVersion left = new RequirementVersion();
        left.setVersionId(18L);
        left.setRequirementId(8L);
        left.setVersionNo(1);
        left.setTitle("旧标题");
        left.setContent("旧正文");
        RequirementVersion right = new RequirementVersion();
        right.setVersionId(19L);
        right.setRequirementId(8L);
        right.setVersionNo(2);
        right.setTitle("新标题");
        right.setContent("新正文");
        when(requirementService.compareRequirementVersionsForUser(41L, 8L, 18L, 19L, 23L))
            .thenReturn(List.of(left, right));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        ResponseEntity<?> response = controller.compareVersions("41", "8", "18", "19");

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> data = (Map<?, ?>) ((AjaxResult) response.getBody()).get("data");
        assertEquals("旧标题", ((com.ruoyi.web.domain.project.RequirementVersionView) data.get("left")).getTitle());
        assertEquals("新正文", ((com.ruoyi.web.domain.project.RequirementVersionView) data.get("right")).getContent());

        when(requirementService.compareRequirementVersionsForUser(41L, 8L, 18L, 19L, 23L)).thenReturn(null);
        assertEquals(404, controller.compareVersions("41", "8", "18", "19").getStatusCode().value());
        verify(requirementService, org.mockito.Mockito.times(2))
            .compareRequirementVersionsForUser(41L, 8L, 18L, 19L, 23L);
    }

    @Test
    void malformedRequirementIdsDoNotQueryService()
    {
        setCurrentUser(23L);
        assertEquals(404, controller.detail("bad", "8").getStatusCode().value());
        assertEquals(404, controller.versions("41", "bad").getStatusCode().value());
        assertEquals(404, controller.compareVersions("41", "8", "bad", "19").getStatusCode().value());
        assertEquals(404, controller.updateStatus("41", "bad", statusRequest()).getStatusCode().value());
        verifyNoInteractions(requirementService);
    }

    @Test
    void memberCanUpdateRequirementStatus()
    {
        setCurrentUser(23L);
        Requirement requirement = new Requirement();
        requirement.setRequirementId(8L);
        requirement.setStatus("doing");
        when(requirementService.updateRequirementStatus(41L, 8L, 23L, "doing")).thenReturn(requirement);

        ResponseEntity<AjaxResult> response = controller.updateStatus("41", "8", statusRequest());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(8L, ((RequirementView) response.getBody().get("data")).getRequirementId());
        verify(requirementService).updateRequirementStatus(41L, 8L, 23L, "doing");
    }

    @Test
    void requirementStatusUpdateServiceErrorsUseBusinessStatus()
    {
        setCurrentUser(23L);
        doThrow(new ServiceException("归档项目不能开展新的业务操作", HttpStatus.BAD_REQUEST))
            .when(requirementService).updateRequirementStatus(41L, 8L, 23L, "doing");

        ResponseEntity<AjaxResult> response = controller.updateStatus("41", "8", statusRequest());

        assertEquals(400, response.getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST, response.getBody().get("code"));
    }

    @Test
    void comparingSameRequirementVersionUsesBadRequest()
    {
        setCurrentUser(23L);
        doThrow(new ServiceException("需求版本对比必须选择两个不同版本", HttpStatus.BAD_REQUEST))
            .when(requirementService).compareRequirementVersionsForUser(41L, 8L, 18L, 18L, 23L);

        ResponseEntity<?> response = controller.compareVersions("41", "8", "18", "18");

        assertEquals(400, response.getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST, ((AjaxResult) response.getBody()).get("code"));
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

    private RequirementStatusUpdateRequest statusRequest()
    {
        RequirementStatusUpdateRequest request = new RequirementStatusUpdateRequest();
        request.setStatus("doing");
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
