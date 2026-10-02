package com.ruoyi.web.controller.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
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
import com.ruoyi.framework.web.service.PermissionService;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.service.IProjectService;
import com.ruoyi.web.domain.project.ProjectCreateRequest;

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
