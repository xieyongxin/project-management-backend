package com.ruoyi.web.controller.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.service.PermissionService;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.domain.ProjectOperationLog;
import com.ruoyi.system.service.IProjectService;
import com.ruoyi.web.domain.project.ProjectCreateRequest;
import com.ruoyi.web.domain.project.ProjectMemberRoleRequest;
import com.ruoyi.web.domain.project.ProjectMemberAdminRequest;
import com.ruoyi.web.domain.project.ProjectMemberAddRequest;
import com.ruoyi.web.domain.project.ProjectNameUpdateRequest;
import com.ruoyi.web.domain.project.ProjectMemberView;
import com.ruoyi.web.domain.project.ProjectArchiveRequest;

class ProjectControllerTest
{
    private final IProjectService projectService = org.mockito.Mockito.mock(IProjectService.class);
    private final ProjectController controller = new ProjectController(projectService);

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createEndpointDeclaresProjectCreatePermission() throws NoSuchMethodException
    {
        PreAuthorize annotation = ProjectController.class
            .getMethod("create", ProjectCreateRequest.class)
            .getAnnotation(PreAuthorize.class);

        assertNotNull(annotation);
        assertEquals("@ss.hasPermi('project:create')", annotation.value());
    }

    @Test
    void missingOrNonMemberProjectUsesHttp404()
    {
        setCurrentUser(23L);
        when(projectService.selectProjectForUser(41L, 23L)).thenReturn(null);

        ResponseEntity<?> response = controller.getInfo("41");

        assertEquals(404, response.getStatusCode().value());
        assertEquals(HttpStatus.NOT_FOUND, ((java.util.Map<?, ?>) response.getBody()).get("code"));
    }

    @Test
    void malformedProjectIdUsesHttp404WithoutQueryingService()
    {
        setCurrentUser(23L);

        ResponseEntity<?> response = controller.getInfo("not-a-number");

        assertEquals(404, response.getStatusCode().value());
        verifyNoInteractions(projectService);
    }

    @Test
    void projectMemberListUsesCurrentUserAndReturnsMembers()
    {
        setCurrentUser(23L);
        ProjectMember member = new ProjectMember();
        member.setUserId(23L);
        member.setUserName("alice");
        member.setNickName("Alice");
        member.setEmail("alice@example.com");
        member.setIsProjectAdmin(1);
        when(projectService.selectProjectMembersForUser(41L, 23L)).thenReturn(List.of(member));

        ResponseEntity<AjaxResult> response = controller.members("41");

        assertEquals(200, response.getStatusCode().value());
        verify(projectService).selectProjectMembersForUser(41L, 23L);
        assertEquals(1, ((List<?>) response.getBody().get("data")).size());
    }

    @Test
    void nonMemberCannotReadProjectMemberList()
    {
        setCurrentUser(23L);
        when(projectService.selectProjectMembersForUser(41L, 23L)).thenReturn(null);

        ResponseEntity<AjaxResult> response = controller.members("41");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void malformedMemberListProjectIdUsesHttp404WithoutQueryingService()
    {
        setCurrentUser(23L);

        ResponseEntity<AjaxResult> response = controller.members("not-a-number");

        assertEquals(404, response.getStatusCode().value());
        verifyNoInteractions(projectService);
    }

    @Test
    void projectAdminCanReadRoleOptions()
    {
        setCurrentUser(23L);
        SysRole role = new SysRole(7L);
        role.setRoleName("项目成员");
        when(projectService.selectProjectRoleOptionsForAdmin(41L, 23L)).thenReturn(List.of(role));

        ResponseEntity<AjaxResult> response = controller.memberRoleOptions("41");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, ((List<?>) response.getBody().get("data")).size());
    }

    @Test
    void roleOptionsPropagateProjectAccessStatus()
    {
        setCurrentUser(23L);
        when(projectService.selectProjectRoleOptionsForAdmin(41L, 23L))
            .thenThrow(new ServiceException("只有项目管理员可以调整成员角色", HttpStatus.FORBIDDEN));

        ResponseEntity<AjaxResult> response = controller.memberRoleOptions("41");

        assertEquals(403, response.getStatusCode().value());
        assertEquals(HttpStatus.FORBIDDEN, response.getBody().get("code"));
    }

    @Test
    void projectAdminCanUpdateMemberRole()
    {
        setCurrentUser(23L);
        ProjectMember member = new ProjectMember();
        member.setUserId(24L);
        member.setRoleId(7L);
        member.setRoleName("项目成员");
        when(projectService.updateProjectMemberRole(41L, 23L, 24L, 7L)).thenReturn(member);
        ProjectMemberRoleRequest request = new ProjectMemberRoleRequest();
        request.setRoleId(7L);

        ResponseEntity<AjaxResult> response = controller.updateMemberRole("41", "24", request);

        assertEquals(200, response.getStatusCode().value());
        verify(projectService).updateProjectMemberRole(41L, 23L, 24L, 7L);
        assertEquals(7L, ((ProjectMemberView) response.getBody().get("data")).getRoleId());
    }

    @Test
    void projectAdminCanAddProjectMemberWithRole()
    {
        setCurrentUser(23L);
        ProjectMember member = new ProjectMember();
        member.setUserId(24L);
        member.setRoleId(7L);
        member.setIsProjectAdmin(0);
        when(projectService.addProjectMember(41L, 23L, 24L, 7L)).thenReturn(member);
        ProjectMemberAddRequest request = new ProjectMemberAddRequest();
        request.setUserId(24L);
        request.setRoleId(7L);

        ResponseEntity<AjaxResult> response = controller.addMember("41", request);

        assertEquals(200, response.getStatusCode().value());
        verify(projectService).addProjectMember(41L, 23L, 24L, 7L);
        assertEquals(24L, ((ProjectMemberView) response.getBody().get("data")).getUserId());
        assertEquals(7L, ((ProjectMemberView) response.getBody().get("data")).getRoleId());
    }

    @Test
    void malformedMemberAddProjectIdUsesHttp404WithoutQueryingService()
    {
        setCurrentUser(23L);
        ProjectMemberAddRequest request = new ProjectMemberAddRequest();
        request.setUserId(24L);
        request.setRoleId(7L);

        ResponseEntity<AjaxResult> response = controller.addMember("not-a-number", request);

        assertEquals(404, response.getStatusCode().value());
        verifyNoInteractions(projectService);
    }

    @Test
    void memberAddPropagatesProjectAccessStatus()
    {
        setCurrentUser(23L);
        doThrow(new ServiceException("只有项目管理员可以管理项目", HttpStatus.FORBIDDEN))
            .when(projectService).addProjectMember(41L, 23L, 24L, 7L);
        ProjectMemberAddRequest request = new ProjectMemberAddRequest();
        request.setUserId(24L);
        request.setRoleId(7L);

        ResponseEntity<AjaxResult> response = controller.addMember("41", request);

        assertEquals(403, response.getStatusCode().value());
        assertEquals(HttpStatus.FORBIDDEN, response.getBody().get("code"));
    }

    @Test
    void projectAdminCanUpdateMemberAdminQualification()
    {
        setCurrentUser(23L);
        ProjectMember member = new ProjectMember();
        member.setUserId(24L);
        member.setIsProjectAdmin(1);
        when(projectService.updateProjectMemberAdmin(41L, 23L, 24L, true)).thenReturn(member);
        ProjectMemberAdminRequest request = new ProjectMemberAdminRequest();
        request.setProjectAdmin(true);

        ResponseEntity<AjaxResult> response = controller.updateMemberAdmin("41", "24", request);

        assertEquals(200, response.getStatusCode().value());
        verify(projectService).updateProjectMemberAdmin(41L, 23L, 24L, true);
        assertEquals(1, ((ProjectMemberView) response.getBody().get("data")).getIsProjectAdmin());
    }

    @Test
    void projectAdminCanUpdateProjectName()
    {
        setCurrentUser(23L);
        Project project = new Project();
        project.setProjectId(41L);
        project.setProjectName("Renamed project");
        when(projectService.updateProjectName(41L, 23L, "Renamed project")).thenReturn(project);
        ProjectNameUpdateRequest request = new ProjectNameUpdateRequest();
        request.setProjectName("Renamed project");

        ResponseEntity<AjaxResult> response = controller.update("41", request);

        assertEquals(200, response.getStatusCode().value());
        verify(projectService).updateProjectName(41L, 23L, "Renamed project");
        assertEquals("Renamed project", ((com.ruoyi.web.domain.project.ProjectView) response.getBody().get("data"))
            .getProjectName());
    }

    @Test
    void projectAdminCanRemoveProjectMember()
    {
        setCurrentUser(23L);

        ResponseEntity<AjaxResult> response = controller.removeMember("41", "24");

        assertEquals(200, response.getStatusCode().value());
        verify(projectService).removeProjectMember(41L, 23L, 24L);
    }

    @Test
    void malformedMemberRemovalIdsUseHttp404WithoutQueryingService()
    {
        setCurrentUser(23L);

        ResponseEntity<AjaxResult> response = controller.removeMember("not-a-number", "24");

        assertEquals(404, response.getStatusCode().value());
        verifyNoInteractions(projectService);
    }

    @Test
    void memberRemovalPropagatesProjectAccessStatus()
    {
        setCurrentUser(23L);
        doThrow(new ServiceException("只有项目管理员可以管理项目", HttpStatus.FORBIDDEN))
            .when(projectService).removeProjectMember(41L, 23L, 24L);

        ResponseEntity<AjaxResult> response = controller.removeMember("41", "24");

        assertEquals(403, response.getStatusCode().value());
        assertEquals(HttpStatus.FORBIDDEN, response.getBody().get("code"));
    }

    @Test
    void projectAdminCanUpdateProjectArchiveStatus()
    {
        setCurrentUser(23L);
        Project project = new Project();
        project.setProjectId(41L);
        project.setStatus(Project.STATUS_ARCHIVED);
        when(projectService.updateProjectStatus(41L, 23L, true)).thenReturn(project);
        ProjectArchiveRequest request = new ProjectArchiveRequest();
        request.setArchived(true);

        ResponseEntity<AjaxResult> response = controller.updateStatus("41", request);

        assertEquals(200, response.getStatusCode().value());
        verify(projectService).updateProjectStatus(41L, 23L, true);
        assertEquals(Project.STATUS_ARCHIVED,
            ((com.ruoyi.web.domain.project.ProjectView) response.getBody().get("data")).getStatus());
    }

    @Test
    void malformedProjectArchiveStatusIdUsesHttp404WithoutQueryingService()
    {
        setCurrentUser(23L);
        ProjectArchiveRequest request = new ProjectArchiveRequest();
        request.setArchived(true);

        ResponseEntity<AjaxResult> response = controller.updateStatus("not-a-number", request);

        assertEquals(404, response.getStatusCode().value());
        verifyNoInteractions(projectService);
    }

    @Test
    void projectLogListUsesCurrentUserAndMapsRows()
    {
        setCurrentUser(23L, Set.of("project:log:list"));
        ProjectOperationLog log = new ProjectOperationLog();
        log.setLogId(8L);
        log.setProjectId(41L);
        log.setOperatorName("alice");
        log.setTargetUserName("bob");
        log.setOperationType("MEMBER_ROLE_UPDATE");
        when(projectService.selectProjectOperationLogsForUser(41L, 23L)).thenReturn(List.of(log));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        ResponseEntity<?> response = controller.operationLogs("41");

        assertEquals(200, response.getStatusCode().value());
        verify(projectService).selectProjectOperationLogsForUser(41L, 23L);
        TableDataInfo result = (TableDataInfo) response.getBody();
        assertEquals(1, result.getTotal());
    }

    @Test
    void projectLogListRequiresPermission()
    {
        try (AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(SecurityTestConfiguration.class))
        {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
            ProjectController securedController = context.getBean(ProjectController.class);
            setCurrentUser(23L, Set.of());

            assertThrows(AccessDeniedException.class, () -> securedController.operationLogs("41"));
        }
    }

    @Test
    void projectListUsesCurrentUserInsteadOfRequestedUserId()
    {
        setCurrentUser(23L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "1");
        request.addParameter("projectName", "Alpha");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        Project project = new Project();
        project.setProjectId(41L);
        project.setProjectName("Alpha project");
        when(projectService.selectProjectsForUser(23L, "Alpha")).thenReturn(List.of(project));

        TableDataInfo response = controller.list("Alpha");

        verify(projectService).selectProjectsForUser(23L, "Alpha");
        assertEquals(1, response.getRows().size());
        assertEquals(1, response.getTotal());
    }

    @Test
    void projectNameValidationMatchesDatabaseColumnLimit()
    {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory())
        {
            var validator = factory.getValidator();
            assertEquals(0, validator.validate(request("A".repeat(255))).size());
            assertEquals(1, validator.validate(request("A".repeat(256))).size());
            assertEquals(1, validator.validate(request("   ")).size());
        }
    }

    @Test
    void memberAddRequestRequiresUserAndRole()
    {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory())
        {
            var validator = factory.getValidator();
            ProjectMemberAddRequest request = new ProjectMemberAddRequest();
            assertEquals(2, validator.validate(request).size());
            request.setUserId(24L);
            assertEquals(1, validator.validate(request).size());
            request.setRoleId(7L);
            assertEquals(0, validator.validate(request).size());
        }
    }

    @Test
    void createIsDeniedWithoutProjectCreatePermission()
    {
        try (AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(SecurityTestConfiguration.class))
        {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
            IProjectService securedService = context.getBean(IProjectService.class);
            ProjectController securedController = context.getBean(ProjectController.class);
            setCurrentUser(23L, Set.of());

            assertThrows(AccessDeniedException.class,
                () -> securedController.create(request("Project")));
            verifyNoInteractions(securedService);

            setCurrentUser(23L, Set.of("project:create"));
            Project project = new Project();
            project.setProjectId(101L);
            project.setProjectName("Project");
            when(securedService.createProject("Project", 23L)).thenReturn(project);

            AjaxResult response = securedController.create(request("Project"));

            assertEquals(HttpStatus.SUCCESS, response.get("code"));
        }
    }

    private ProjectCreateRequest request(String projectName)
    {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setProjectName(projectName);
        return request;
    }

    private void setCurrentUser(Long userId)
    {
        setCurrentUser(userId, Set.of());
    }

    private void setCurrentUser(Long userId, Set<String> permissions)
    {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setPermissions(permissions);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, "", List.of()));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class SecurityTestConfiguration
    {
        @Bean("ss")
        PermissionService permissionService()
        {
            return new PermissionService();
        }

        @Bean
        IProjectService projectService()
        {
            return org.mockito.Mockito.mock(IProjectService.class);
        }

        @Bean
        ProjectController projectController(IProjectService projectService)
        {
            return new ProjectController(projectService);
        }
    }
}
