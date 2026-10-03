package com.ruoyi.web.controller.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.service.ITaskService;
import com.ruoyi.web.domain.project.TaskCreateRequest;

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
        assertNotNull(list);
        assertNotNull(options);
        assertNotNull(create);
        assertEquals("@ss.hasPermi('project:task:list')", list.value());
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
    void memberCanReadOptionsAndCreateTask()
    {
        setCurrentUser(23L);
        SysDictData status = new SysDictData();
        status.setDictValue("todo");
        SysDictData category = new SysDictData();
        category.setDictValue("dev");
        when(taskService.selectActiveStatuses(41L, 23L)).thenReturn(List.of(status));
        when(taskService.selectActiveCategories(41L, 23L)).thenReturn(List.of(category));
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
        verify(taskService).createTask(41L, 23L, 7L, "登录", "实现登录", "todo", List.of("dev"), List.of(23L));
    }

    @Test
    void deniedOptionsAndMalformedIdReturn404()
    {
        setCurrentUser(23L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(taskService.selectActiveStatuses(41L, 23L)).thenReturn(null);
        when(taskService.selectActiveCategories(41L, 23L)).thenReturn(null);

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

    private void setCurrentUser(Long userId)
    {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setPermissions(Set.of("project:task:add", "project:task:list"));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, "", List.of()));
    }
}
