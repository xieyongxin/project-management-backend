package com.ruoyi.web.controller.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.domain.Task;
import com.ruoyi.system.domain.TaskVersion;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.service.ITaskService;
import com.ruoyi.web.domain.project.TaskCreateRequest;
import com.ruoyi.web.domain.project.TaskStatusUpdateRequest;
import com.ruoyi.web.domain.project.TaskVersionUpdateRequest;
import com.ruoyi.web.domain.project.TaskVersionView;

class TaskControllerTest
{
    private final ITaskService taskService = org.mockito.Mockito.mock(ITaskService.class);
    private final TaskController controller = new TaskController(taskService);

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void taskEndpointsDeclarePermission() throws NoSuchMethodException
    {
        org.springframework.security.access.prepost.PreAuthorize list = TaskController.class
            .getMethod("list", String.class).getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.springframework.security.access.prepost.PreAuthorize options = TaskController.class
            .getMethod("options", String.class).getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.springframework.security.access.prepost.PreAuthorize create = TaskController.class
            .getMethod("create", String.class, TaskCreateRequest.class)
            .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.springframework.security.access.prepost.PreAuthorize detail = TaskController.class
            .getMethod("detail", String.class, String.class)
            .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.springframework.security.access.prepost.PreAuthorize versions = TaskController.class
            .getMethod("versions", String.class, String.class)
            .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.springframework.security.access.prepost.PreAuthorize compareVersions = TaskController.class
            .getMethod("compareVersions", String.class, String.class, String.class, String.class)
            .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.springframework.security.access.prepost.PreAuthorize updateStatus = TaskController.class
            .getMethod("updateStatus", String.class, String.class, TaskStatusUpdateRequest.class)
            .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.springframework.security.access.prepost.PreAuthorize updateLatestVersion = TaskController.class
            .getMethod("updateLatestVersion", String.class, String.class, TaskVersionUpdateRequest.class)
            .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.springframework.security.access.prepost.PreAuthorize delete = TaskController.class
            .getMethod("delete", String.class, String.class)
            .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertNotNull(list);
        assertNotNull(options);
        assertNotNull(create);
        assertNotNull(detail);
        assertNotNull(versions);
        assertNotNull(compareVersions);
        assertNotNull(updateStatus);
        assertNotNull(updateLatestVersion);
        assertNotNull(delete);
        assertEquals("@ss.hasPermi('project:task:list')", list.value());
        assertEquals("@ss.hasPermi('project:task:list')", detail.value());
        assertEquals("@ss.hasPermi('project:task:list')", versions.value());
        assertEquals("@ss.hasPermi('project:task:list')", compareVersions.value());
        assertEquals("@ss.hasPermi('project:task:status')", updateStatus.value());
        assertEquals("@ss.hasPermi('project:task:edit')", updateLatestVersion.value());
        assertEquals("@ss.hasPermi('project:task:delete')", delete.value());
        assertEquals("@ss.hasPermi('project:task:add')", options.value());
        assertEquals("@ss.hasPermi('project:task:add')", create.value());
    }

    @Test
    void memberCanListTasksAndNonMemberGets404()
    {
        setCurrentUser(23L);
        Task task = new Task();
        task.setTaskId(8L);
        task.setTitle("登录");
        when(taskService.selectTasksForUser(41L, 23L)).thenReturn(List.of(task));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        ResponseEntity<?> response = controller.list("41");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, ((TableDataInfo) response.getBody()).getTotal());
        verify(taskService).selectTasksForUser(41L, 23L);

        when(taskService.selectTasksForUser(41L, 23L)).thenReturn(null);
        assertEquals(404, controller.list("41").getStatusCode().value());
        assertEquals(404, controller.list("bad").getStatusCode().value());
    }

    @Test
    void memberCanReadTaskDetailAndVersionsAndNonMemberGets404()
    {
        setCurrentUser(23L);
        Task task = new Task();
        task.setTaskId(8L);
        task.setTitle("登录");
        TaskVersion version = new TaskVersion();
        version.setVersionId(18L);
        version.setTaskId(8L);
        version.setVersionNo(1);
        version.setTitle("登录");
        version.setDescription("正文");
        version.setRequirementVersionId(28L);
        version.setRequirementVersionNo(2);
        when(taskService.selectTaskForUser(41L, 8L, 23L)).thenReturn(task);
        when(taskService.selectTaskVersionsForUser(41L, 8L, 23L)).thenReturn(List.of(version));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        ResponseEntity<?> detail = controller.detail("41", "8");
        ResponseEntity<?> versions = controller.versions("41", "8");

        assertEquals(200, detail.getStatusCode().value());
        assertEquals(8L, ((com.ruoyi.web.domain.project.TaskView) ((java.util.Map<?, ?>) detail.getBody())
            .get("data")).getTaskId());
        assertEquals(200, versions.getStatusCode().value());
        TaskVersionView versionView = (TaskVersionView) ((List<?>) ((java.util.Map<?, ?>) versions.getBody())
            .get("data")).get(0);
        assertEquals(2, versionView.getRequirementVersionNo());

        when(taskService.selectTaskForUser(41L, 8L, 23L)).thenReturn(null);
        when(taskService.selectTaskVersionsForUser(41L, 8L, 23L)).thenReturn(null);
        assertEquals(404, controller.detail("41", "8").getStatusCode().value());
        assertEquals(404, controller.versions("41", "8").getStatusCode().value());
        verify(taskService, org.mockito.Mockito.times(2)).selectTaskForUser(41L, 8L, 23L);
        verify(taskService, org.mockito.Mockito.times(2)).selectTaskVersionsForUser(41L, 8L, 23L);
    }

    @Test
    void memberCanCompareTwoTaskVersionsAndNonMemberGets404()
    {
        setCurrentUser(23L);
        TaskVersion left = new TaskVersion();
        left.setVersionId(18L);
        left.setTaskId(8L);
        left.setVersionNo(1);
        left.setTitle("旧标题");
        TaskVersion right = new TaskVersion();
        right.setVersionId(19L);
        right.setTaskId(8L);
        right.setVersionNo(2);
        right.setTitle("新标题");
        when(taskService.compareTaskVersionsForUser(41L, 8L, 18L, 19L, 23L))
            .thenReturn(List.of(left, right));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        ResponseEntity<?> response = controller.compareVersions("41", "8", "18", "19");

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> data = (Map<?, ?>) ((AjaxResult) response.getBody()).get("data");
        assertEquals("旧标题", ((TaskVersionView) data.get("left")).getTitle());
        assertEquals("新标题", ((TaskVersionView) data.get("right")).getTitle());

        when(taskService.compareTaskVersionsForUser(41L, 8L, 18L, 19L, 23L)).thenReturn(null);
        assertEquals(404, controller.compareVersions("41", "8", "18", "19").getStatusCode().value());
        verify(taskService, org.mockito.Mockito.times(2))
            .compareTaskVersionsForUser(41L, 8L, 18L, 19L, 23L);
    }

    @Test
    void malformedTaskIdsDoNotQueryService()
    {
        setCurrentUser(23L);
        assertEquals(404, controller.detail("bad", "8").getStatusCode().value());
        assertEquals(404, controller.versions("41", "bad").getStatusCode().value());
        assertEquals(404, controller.compareVersions("41", "8", "bad", "19").getStatusCode().value());
        assertEquals(404, controller.updateStatus("41", "bad", statusRequest()).getStatusCode().value());
        assertEquals(404, controller.updateLatestVersion("41", "bad", versionUpdateRequest()).getStatusCode().value());
        org.mockito.Mockito.verifyNoInteractions(taskService);
    }

    @Test
    void memberCanUpdateTaskStatus()
    {
        setCurrentUser(23L);
        Task task = new Task();
        task.setTaskId(8L);
        task.setStatus("doing");
        when(taskService.updateTaskStatus(41L, 8L, 23L, "doing")).thenReturn(task);

        ResponseEntity<AjaxResult> response = controller.updateStatus("41", "8", statusRequest());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(8L, ((com.ruoyi.web.domain.project.TaskView) response.getBody().get("data")).getTaskId());
        verify(taskService).updateTaskStatus(41L, 8L, 23L, "doing");
    }

    @Test
    void memberCanUpdateTaskLatestVersion()
    {
        setCurrentUser(23L);
        Task task = new Task();
        task.setTaskId(8L);
        task.setTitle("新标题");
        when(taskService.updateTaskToLatestRequirement(41L, 8L, 23L, "新标题", "新说明"))
            .thenReturn(task);

        ResponseEntity<AjaxResult> response = controller.updateLatestVersion("41", "8", versionUpdateRequest());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(8L, ((com.ruoyi.web.domain.project.TaskView) response.getBody().get("data")).getTaskId());
        verify(taskService).updateTaskToLatestRequirement(41L, 8L, 23L, "新标题", "新说明");
    }

    @Test
    void memberCanDeleteTaskAndServiceErrorsUseBusinessStatus()
    {
        setCurrentUser(23L);
        Task task = new Task();
        task.setTaskId(8L);
        task.setIsDeleted(1);
        when(taskService.deleteTask(41L, 8L, 23L)).thenReturn(task);

        ResponseEntity<AjaxResult> response = controller.delete("41", "8");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, ((com.ruoyi.web.domain.project.TaskView) response.getBody().get("data")).getIsDeleted());
        verify(taskService).deleteTask(41L, 8L, 23L);

        doThrow(new ServiceException("归档项目不能开展新的业务操作", HttpStatus.BAD_REQUEST))
            .when(taskService).deleteTask(41L, 8L, 23L);
        response = controller.delete("41", "8");
        assertEquals(400, response.getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST, response.getBody().get("code"));
    }

    @Test
    void malformedDeleteIdsDoNotQueryService()
    {
        setCurrentUser(23L);
        assertEquals(404, controller.delete("bad", "8").getStatusCode().value());
        assertEquals(404, controller.delete("41", "bad").getStatusCode().value());
        org.mockito.Mockito.verifyNoInteractions(taskService);
    }

    @Test
    void statusUpdateServiceErrorsUseBusinessStatus()
    {
        setCurrentUser(23L);
        doThrow(new ServiceException("归档项目不能开展新的业务操作", HttpStatus.BAD_REQUEST))
            .when(taskService).updateTaskStatus(41L, 8L, 23L, "doing");

        ResponseEntity<AjaxResult> response = controller.updateStatus("41", "8", statusRequest());

        assertEquals(400, response.getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST, response.getBody().get("code"));
    }

    @Test
    void comparingSameTaskVersionUsesBadRequest()
    {
        setCurrentUser(23L);
        doThrow(new ServiceException("任务版本对比必须选择两个不同版本", HttpStatus.BAD_REQUEST))
            .when(taskService).compareTaskVersionsForUser(41L, 8L, 18L, 18L, 23L);

        ResponseEntity<?> response = controller.compareVersions("41", "8", "18", "18");

        assertEquals(400, response.getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST, ((AjaxResult) response.getBody()).get("code"));
    }

    @Test
    void memberCanReadOptionsAndCreateTask()
    {
        setCurrentUser(23L);
        SysDictData status = new SysDictData();
        status.setDictValue("todo");
        SysDictData category = new SysDictData();
        category.setDictValue("dev");
        Requirement requirement = new Requirement();
        requirement.setRequirementId(7L);
        requirement.setTitle("登录需求");
        when(taskService.selectActiveStatuses(41L, 23L)).thenReturn(List.of(status));
        when(taskService.selectActiveCategories(41L, 23L)).thenReturn(List.of(category));
        when(taskService.selectAvailableRequirements(41L, 23L)).thenReturn(List.of(requirement));
        Task task = new Task();
        task.setTaskId(8L);
        when(taskService.createTask(41L, 23L, 7L, "登录", "实现登录", "todo", List.of("dev"), List.of(23L)))
            .thenReturn(task);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        ResponseEntity<?> options = controller.options("41");
        ResponseEntity<AjaxResult> create = controller.create("41", request());

        assertEquals(200, options.getStatusCode().value());
        assertEquals(200, create.getStatusCode().value());
        assertEquals(8L, ((com.ruoyi.web.domain.project.TaskView) create.getBody().get("data")).getTaskId());
        verify(taskService).selectActiveStatuses(41L, 23L);
        verify(taskService).selectActiveCategories(41L, 23L);
        verify(taskService).selectAvailableRequirements(41L, 23L);
        verify(taskService).createTask(41L, 23L, 7L, "登录", "实现登录", "todo", List.of("dev"), List.of(23L));
    }

    @Test
    void deniedOptionsAndMalformedIdReturn404()
    {
        setCurrentUser(23L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(taskService.selectActiveStatuses(41L, 23L)).thenReturn(null);
        when(taskService.selectActiveCategories(41L, 23L)).thenReturn(null);
        when(taskService.selectAvailableRequirements(41L, 23L)).thenReturn(null);

        ResponseEntity<?> denied = controller.options("41");
        ResponseEntity<?> malformed = controller.options("bad");

        assertEquals(404, denied.getStatusCode().value());
        assertEquals(404, malformed.getStatusCode().value());
    }

    @Test
    void serviceErrorsAreReturnedWithBusinessStatus()
    {
        setCurrentUser(23L);
        doThrow(new ServiceException("归档项目不能开展新的业务操作", HttpStatus.BAD_REQUEST))
            .when(taskService).createTask(41L, 23L, 7L, "登录", "实现登录", "todo", List.of("dev"), List.of(23L));

        ResponseEntity<AjaxResult> response = controller.create("41", request());

        assertEquals(400, response.getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST, response.getBody().get("code"));
    }

    @Test
    void taskRequestRequiresAllCreationFields()
    {
        try (jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory())
        {
            var validator = factory.getValidator();
            TaskCreateRequest request = new TaskCreateRequest();
            assertEquals(6, validator.validate(request).size());
            request.setRequirementId(7L);
            request.setTitle("Title");
            request.setDescription("Description");
            request.setStatus("todo");
            request.setCategoryValues(List.of("dev"));
            request.setOwnerIds(List.of(23L));
            assertEquals(0, validator.validate(request).size());
        }
    }

    private TaskCreateRequest request()
    {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setRequirementId(7L);
        request.setTitle("登录");
        request.setDescription("实现登录");
        request.setStatus("todo");
        request.setCategoryValues(List.of("dev"));
        request.setOwnerIds(List.of(23L));
        return request;
    }

    private TaskStatusUpdateRequest statusRequest()
    {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus("doing");
        return request;
    }

    private TaskVersionUpdateRequest versionUpdateRequest()
    {
        TaskVersionUpdateRequest request = new TaskVersionUpdateRequest();
        request.setTitle("新标题");
        request.setDescription("新说明");
        return request;
    }

    private void setCurrentUser(Long userId)
    {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setPermissions(Set.of("project:task:add", "project:task:list"));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, "", List.of()));
    }
}
