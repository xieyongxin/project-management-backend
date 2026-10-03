package com.ruoyi.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.domain.ProjectOperationLog;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectMemberMapper;
import com.ruoyi.system.mapper.ProjectOperationLogMapper;
import com.ruoyi.system.service.IProjectService;

class ProjectServiceImplTest
{
    private static final AnnotationConfigApplicationContext CONTEXT =
        new AnnotationConfigApplicationContext(TestConfiguration.class);
    private static final JdbcTemplate JDBC = CONTEXT.getBean(JdbcTemplate.class);
    private static final IProjectService SERVICE = CONTEXT.getBean(IProjectService.class);
    private static final TestProjectMemberMapper MEMBER_MAPPER = CONTEXT.getBean(TestProjectMemberMapper.class);
    private static final TestProjectOperationLogMapper LOG_MAPPER = CONTEXT.getBean(TestProjectOperationLogMapper.class);

    @AfterAll
    static void closeContext()
    {
        CONTEXT.close();
    }

    @BeforeEach
    void cleanTables()
    {
        MEMBER_MAPPER.failNextInsert.set(false);
        MEMBER_MAPPER.roles.clear();
        MEMBER_MAPPER.inactiveRoles.clear();
        LOG_MAPPER.logs.clear();
        LOG_MAPPER.last = null;
        LOG_MAPPER.failNextInsert.set(false);
        JDBC.update("delete from pm_requirement_owner");
        JDBC.update("delete from pm_task_owner");
        JDBC.update("delete from pm_task_version");
        JDBC.update("delete from pm_task_category");
        JDBC.update("delete from pm_task");
        JDBC.update("delete from pm_requirement");
        JDBC.update("delete from pm_project_member");
        JDBC.update("delete from pm_project");
        JDBC.update("delete from sys_user");
    }

    @Test
    void createTrimsNameAndMakesCreatorProjectAdmin()
    {
        Project project = SERVICE.createProject("  Project Alpha  ", 21L);

        assertEquals("Project Alpha", project.getProjectName());
        assertEquals("project alpha", project.getProjectNameKey());
        assertEquals(1, count("pm_project"));
        assertEquals(1, count("pm_project_member"));
        assertEquals(1, JDBC.queryForObject(
            "select is_project_admin from pm_project_member where project_id = ? and user_id = ?",
            Integer.class, project.getProjectId(), 21L));
    }

    @Test
    void blankNameIsRejectedWithoutWrites()
    {
        ServiceException error = assertThrows(ServiceException.class, () -> SERVICE.createProject("  ", 21L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
        assertEquals(0, count("pm_project"));
        assertEquals(0, count("pm_project_member"));
    }

    @Test
    void overlongNameIsRejectedWithoutWrites()
    {
        ServiceException error = assertThrows(ServiceException.class,
            () -> SERVICE.createProject("A".repeat(256), 21L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
        assertEquals(0, count("pm_project"));
        assertEquals(0, count("pm_project_member"));
    }

    @Test
    void duplicateNormalizedNameIsRejectedByUniqueConstraint()
    {
        SERVICE.createProject("Project Alpha", 21L);

        ServiceException error = assertThrows(ServiceException.class,
            () -> SERVICE.createProject(" project alpha ", 22L));

        assertEquals(HttpStatus.CONFLICT, error.getCode());
        assertEquals(1, count("pm_project"));
        assertEquals(1, count("pm_project_member"));
    }

    @Test
    void projectAdminCanRenameProjectAndOperationIsLogged()
    {
        Project project = SERVICE.createProject("Original project", 21L);

        Project updated = SERVICE.updateProjectName(project.getProjectId(), 21L, "  Renamed project  ");

        assertEquals("Renamed project", updated.getProjectName());
        assertEquals("renamed project", updated.getProjectNameKey());
        assertEquals("Renamed project", SERVICE.selectProjectForUser(project.getProjectId(), 21L).getProjectName());
        assertEquals("PROJECT_NAME_UPDATE", LOG_MAPPER.last.getOperationType());
        assertEquals("项目名称由“Original project”修改为“Renamed project”", LOG_MAPPER.last.getDetail());
    }

    @Test
    void projectRenameRejectsDuplicateAndPreservesOriginalName()
    {
        Project first = SERVICE.createProject("First project", 21L);
        SERVICE.createProject("Second project", 22L);

        ServiceException error = assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectName(first.getProjectId(), 21L, " second project "));

        assertEquals(HttpStatus.CONFLICT, error.getCode());
        assertEquals("First project", SERVICE.selectProjectForUser(first.getProjectId(), 21L).getProjectName());
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void projectRenameRequiresAdminAndValidName()
    {
        Project project = SERVICE.createProject("Rename boundary project", 21L);
        addMember(project, 22L, 7L, 0);

        ServiceException nonAdmin = assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectName(project.getProjectId(), 22L, "New name"));
        ServiceException blank = assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectName(project.getProjectId(), 21L, "  "));

        assertEquals(HttpStatus.FORBIDDEN, nonAdmin.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, blank.getCode());
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void projectRenameRollsBackWhenOperationLogCannotBeWritten()
    {
        Project project = SERVICE.createProject("Rename transaction project", 21L);
        LOG_MAPPER.failNextInsert.set(true);

        assertThrows(IllegalStateException.class,
            () -> SERVICE.updateProjectName(project.getProjectId(), 21L, "Updated name"));

        assertEquals("Rename transaction project", SERVICE.selectProjectForUser(project.getProjectId(), 21L)
            .getProjectName());
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void projectAdminCanArchiveAndReenableProject()
    {
        Project project = SERVICE.createProject("Archive project", 21L);

        Project archived = SERVICE.updateProjectStatus(project.getProjectId(), 21L, true);

        assertEquals(Project.STATUS_ARCHIVED, archived.getStatus());
        assertEquals(Project.STATUS_ARCHIVED,
            SERVICE.selectProjectForUser(project.getProjectId(), 21L).getStatus());
        ServiceException renameError = assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectName(project.getProjectId(), 21L, "Renamed while archived"));
        assertEquals(HttpStatus.BAD_REQUEST, renameError.getCode());

        Project enabled = SERVICE.updateProjectStatus(project.getProjectId(), 21L, false);

        assertEquals(Project.STATUS_ACTIVE, enabled.getStatus());
        assertEquals(Project.STATUS_ACTIVE,
            SERVICE.selectProjectForUser(project.getProjectId(), 21L).getStatus());
    }

    @Test
    void projectStatusUpdateRequiresAdministratorAndMembership()
    {
        Project project = SERVICE.createProject("Archive permission project", 21L);
        addMember(project, 22L, 7L, 0);

        ServiceException nonAdmin = assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectStatus(project.getProjectId(), 22L, true));
        ServiceException nonMember = assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectStatus(project.getProjectId(), 1L, true));

        assertEquals(HttpStatus.FORBIDDEN, nonAdmin.getCode());
        assertEquals(HttpStatus.NOT_FOUND, nonMember.getCode());
        assertEquals(Project.STATUS_ACTIVE,
            SERVICE.selectProjectForUser(project.getProjectId(), 21L).getStatus());
    }

    @Test
    void memberInsertFailureRollsBackProjectInsert()
    {
        MEMBER_MAPPER.failNextInsert.set(true);

        assertThrows(IllegalStateException.class, () -> SERVICE.createProject("Rollback project", 21L));

        assertEquals(0, count("pm_project"));
        assertEquals(0, count("pm_project_member"));
    }

    @Test
    void listAndDetailRequireMembershipEvenForSuperAdminId()
    {
        Project first = SERVICE.createProject("First project", 21L);
        Project second = SERVICE.createProject("Second project", 22L);

        assertEquals(List.of(first.getProjectId()), SERVICE.selectProjectsForUser(21L, null).stream()
            .map(Project::getProjectId).toList());
        assertEquals(List.of(second.getProjectId()), SERVICE.selectProjectsForUser(22L, null).stream()
            .map(Project::getProjectId).toList());
        assertEquals(0, SERVICE.selectProjectsForUser(1L, null).size());
        assertNotNull(SERVICE.selectProjectForUser(first.getProjectId(), 21L));
        assertNull(SERVICE.selectProjectForUser(first.getProjectId(), 22L));
        assertNull(SERVICE.selectProjectForUser(first.getProjectId(), 1L));
    }

    @Test
    void projectAdminCanAssignActiveRoleAndOperationIsLogged()
    {
        Project project = SERVICE.createProject("Role project", 21L);
        ProjectMember member = new ProjectMember();
        member.setProjectId(project.getProjectId());
        member.setUserId(22L);
        member.setIsProjectAdmin(0);
        member.setCreateTime(new java.util.Date());
        member.setUpdateTime(new java.util.Date());
        MEMBER_MAPPER.insertProjectMember(member);
        MEMBER_MAPPER.roles.put(7L, "项目成员");

        ProjectMember updated = SERVICE.updateProjectMemberRole(project.getProjectId(), 21L, 22L, 7L);

        assertEquals(7L, updated.getRoleId());
        assertEquals(1, LOG_MAPPER.logs.size());
        assertEquals("MEMBER_ROLE_UPDATE", LOG_MAPPER.last.getOperationType());
        assertNull(LOG_MAPPER.last.getPreviousRoleId());
        assertEquals(7L, LOG_MAPPER.last.getNewRoleId());
    }

    @Test
    void projectAdminCanAddActiveUserWithActiveRoleAndOperationIsLogged()
    {
        Project project = SERVICE.createProject("Member add project", 21L);
        insertUser(22L, "bob", "Bob", "bob@example.com", "0", "0");
        MEMBER_MAPPER.roles.put(7L, "项目成员");

        ProjectMember added = SERVICE.addProjectMember(project.getProjectId(), 21L, 22L, 7L);

        assertEquals(22L, added.getUserId());
        assertEquals(7L, added.getRoleId());
        assertEquals(0, added.getIsProjectAdmin());
        assertEquals(1, count("pm_project_member") - 1);
        assertEquals("MEMBER_ADD", LOG_MAPPER.last.getOperationType());
        assertEquals(22L, LOG_MAPPER.last.getTargetUserId());
        assertEquals(7L, LOG_MAPPER.last.getNewRoleId());
    }

    @Test
    void addingMemberRequiresAdministratorAndProjectMembership()
    {
        Project project = SERVICE.createProject("Member add permission project", 21L);
        insertUser(22L, "bob", "Bob", "bob@example.com", "0", "0");
        MEMBER_MAPPER.roles.put(7L, "项目成员");
        addMember(project, 23L, 7L, 0);

        ServiceException nonAdmin = assertThrows(ServiceException.class,
            () -> SERVICE.addProjectMember(project.getProjectId(), 23L, 22L, 7L));
        ServiceException nonMember = assertThrows(ServiceException.class,
            () -> SERVICE.addProjectMember(project.getProjectId(), 99L, 22L, 7L));

        assertEquals(HttpStatus.FORBIDDEN, nonAdmin.getCode());
        assertEquals(HttpStatus.NOT_FOUND, nonMember.getCode());
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void addingMemberRejectsInvalidUserRoleAndDuplicate()
    {
        Project project = SERVICE.createProject("Member add validation project", 21L);
        MEMBER_MAPPER.roles.put(7L, "项目成员");
        MEMBER_MAPPER.roles.put(8L, "已停用");
        MEMBER_MAPPER.inactiveRoles.add(8L);
        insertUser(22L, "bob", "Bob", "bob@example.com", "0", "0");
        insertUser(23L, "disabled", "Disabled", "disabled@example.com", "1", "0");
        insertUser(24L, "deleted", "Deleted", "deleted@example.com", "0", "2");

        ServiceException missingUser = assertThrows(ServiceException.class,
            () -> SERVICE.addProjectMember(project.getProjectId(), 21L, 99L, 7L));
        ServiceException disabledUser = assertThrows(ServiceException.class,
            () -> SERVICE.addProjectMember(project.getProjectId(), 21L, 23L, 7L));
        ServiceException deletedUser = assertThrows(ServiceException.class,
            () -> SERVICE.addProjectMember(project.getProjectId(), 21L, 24L, 7L));
        ServiceException inactiveRole = assertThrows(ServiceException.class,
            () -> SERVICE.addProjectMember(project.getProjectId(), 21L, 22L, 8L));
        ServiceException missingRole = assertThrows(ServiceException.class,
            () -> SERVICE.addProjectMember(project.getProjectId(), 21L, 22L, 99L));

        assertEquals(HttpStatus.BAD_REQUEST, missingUser.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, disabledUser.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, deletedUser.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, inactiveRole.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, missingRole.getCode());

        SERVICE.addProjectMember(project.getProjectId(), 21L, 22L, 7L);
        ServiceException duplicate = assertThrows(ServiceException.class,
            () -> SERVICE.addProjectMember(project.getProjectId(), 21L, 22L, 7L));
        assertEquals(HttpStatus.CONFLICT, duplicate.getCode());
        assertEquals(1, LOG_MAPPER.logs.size());
    }

    @Test
    void addingMemberRollsBackWhenOperationLogCannotBeWritten()
    {
        Project project = SERVICE.createProject("Member add transaction project", 21L);
        insertUser(22L, "bob", "Bob", "bob@example.com", "0", "0");
        MEMBER_MAPPER.roles.put(7L, "项目成员");
        LOG_MAPPER.failNextInsert.set(true);

        assertThrows(IllegalStateException.class,
            () -> SERVICE.addProjectMember(project.getProjectId(), 21L, 22L, 7L));

        assertNull(MEMBER_MAPPER.selectProjectMember(project.getProjectId(), 22L));
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void projectAdminCanAddMemberToArchivedProject()
    {
        Project project = SERVICE.createProject("Archived member add project", 21L);
        insertUser(22L, "bob", "Bob", "bob@example.com", "0", "0");
        MEMBER_MAPPER.roles.put(7L, "项目成员");
        SERVICE.updateProjectStatus(project.getProjectId(), 21L, true);

        ProjectMember added = SERVICE.addProjectMember(project.getProjectId(), 21L, 22L, 7L);

        assertEquals(22L, added.getUserId());
        assertEquals(Project.STATUS_ARCHIVED,
            SERVICE.selectProjectForUser(project.getProjectId(), 21L).getStatus());
    }

    @Test
    void nonAdminCannotAssignRoleAndInactiveRoleIsRejected()
    {
        Project project = SERVICE.createProject("Role boundary project", 21L);
        ProjectMember member = new ProjectMember();
        member.setProjectId(project.getProjectId());
        member.setUserId(22L);
        member.setIsProjectAdmin(0);
        member.setCreateTime(new java.util.Date());
        member.setUpdateTime(new java.util.Date());
        MEMBER_MAPPER.insertProjectMember(member);
        MEMBER_MAPPER.roles.put(7L, "项目成员");
        MEMBER_MAPPER.roles.put(8L, "已停用");
        MEMBER_MAPPER.inactiveRoles.add(8L);

        assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectMemberRole(project.getProjectId(), 22L, 22L, 7L));
        assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectMemberRole(project.getProjectId(), 21L, 22L, 8L));
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void roleUpdateRollsBackWhenOperationLogCannotBeWritten()
    {
        Project project = SERVICE.createProject("Role transaction project", 21L);
        ProjectMember member = new ProjectMember();
        member.setProjectId(project.getProjectId());
        member.setUserId(22L);
        member.setIsProjectAdmin(0);
        member.setCreateTime(new java.util.Date());
        member.setUpdateTime(new java.util.Date());
        MEMBER_MAPPER.insertProjectMember(member);
        MEMBER_MAPPER.roles.put(7L, "项目成员");
        LOG_MAPPER.failNextInsert.set(true);

        assertThrows(IllegalStateException.class,
            () -> SERVICE.updateProjectMemberRole(project.getProjectId(), 21L, 22L, 7L));

        assertNull(MEMBER_MAPPER.selectProjectMember(project.getProjectId(), 22L).getRoleId());
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void projectAdminCanGrantAndRevokeAdminWhenMemberHasActiveRole()
    {
        Project project = SERVICE.createProject("Admin qualification project", 21L);
        ProjectMember member = addMember(project, 22L, 7L, 0);
        MEMBER_MAPPER.roles.put(7L, "项目成员");

        ProjectMember granted = SERVICE.updateProjectMemberAdmin(project.getProjectId(), 21L, 22L, true);

        assertEquals(1, granted.getIsProjectAdmin());
        assertEquals("MEMBER_ADMIN_UPDATE", LOG_MAPPER.last.getOperationType());
        assertEquals("授予项目管理员资格", LOG_MAPPER.last.getDetail());

        ProjectMember revoked = SERVICE.updateProjectMemberAdmin(project.getProjectId(), 21L, 22L, false);

        assertEquals(0, revoked.getIsProjectAdmin());
        assertEquals(2, LOG_MAPPER.logs.size());
        assertEquals("撤销项目管理员资格", LOG_MAPPER.last.getDetail());
    }

    @Test
    void adminQualificationRequiresRoleAndRetainsOneAdmin()
    {
        Project project = SERVICE.createProject("Admin qualification boundary project", 21L);
        addMember(project, 22L, null, 0);

        ServiceException noRole = assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectMemberAdmin(project.getProjectId(), 21L, 22L, true));
        assertEquals(HttpStatus.BAD_REQUEST, noRole.getCode());

        ServiceException lastAdmin = assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectMemberAdmin(project.getProjectId(), 21L, 21L, false));
        assertEquals(HttpStatus.BAD_REQUEST, lastAdmin.getCode());
        assertEquals(1, MEMBER_MAPPER.selectProjectMember(project.getProjectId(), 21L).getIsProjectAdmin());
    }

    @Test
    void nonAdminCannotChangeAdminQualification()
    {
        Project project = SERVICE.createProject("Admin permission boundary project", 21L);
        addMember(project, 22L, 7L, 0);
        MEMBER_MAPPER.roles.put(7L, "项目成员");

        ServiceException error = assertThrows(ServiceException.class,
            () -> SERVICE.updateProjectMemberAdmin(project.getProjectId(), 22L, 21L, false));

        assertEquals(HttpStatus.FORBIDDEN, error.getCode());
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void adminQualificationRollsBackWhenOperationLogCannotBeWritten()
    {
        Project project = SERVICE.createProject("Admin qualification transaction project", 21L);
        addMember(project, 22L, 7L, 0);
        MEMBER_MAPPER.roles.put(7L, "项目成员");
        LOG_MAPPER.failNextInsert.set(true);

        assertThrows(IllegalStateException.class,
            () -> SERVICE.updateProjectMemberAdmin(project.getProjectId(), 21L, 22L, true));

        assertEquals(0, MEMBER_MAPPER.selectProjectMember(project.getProjectId(), 22L).getIsProjectAdmin());
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void projectAdminCanRemoveMemberAndOperationIsLogged()
    {
        Project project = SERVICE.createProject("Member removal project", 21L);
        addMember(project, 22L, 7L, 0);
        MEMBER_MAPPER.roles.put(7L, "项目成员");

        SERVICE.removeProjectMember(project.getProjectId(), 21L, 22L);

        assertNull(MEMBER_MAPPER.selectProjectMember(project.getProjectId(), 22L));
        assertEquals("MEMBER_REMOVE", LOG_MAPPER.last.getOperationType());
        assertEquals(22L, LOG_MAPPER.last.getTargetUserId());
        assertEquals("移除项目成员", LOG_MAPPER.last.getDetail());
    }

    @Test
    void projectMemberRemovalRequiresAdminAndRetainsLastAdmin()
    {
        Project project = SERVICE.createProject("Member removal boundary project", 21L);
        addMember(project, 22L, 7L, 0);

        ServiceException nonAdmin = assertThrows(ServiceException.class,
            () -> SERVICE.removeProjectMember(project.getProjectId(), 22L, 21L));
        ServiceException lastAdmin = assertThrows(ServiceException.class,
            () -> SERVICE.removeProjectMember(project.getProjectId(), 21L, 21L));

        assertEquals(HttpStatus.FORBIDDEN, nonAdmin.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, lastAdmin.getCode());
        assertNotNull(MEMBER_MAPPER.selectProjectMember(project.getProjectId(), 21L));
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void projectMemberRemovalRejectsNonMemberAndRollsBackWhenLogFails()
    {
        Project project = SERVICE.createProject("Member removal transaction project", 21L);
        addMember(project, 22L, 7L, 0);

        ServiceException missing = assertThrows(ServiceException.class,
            () -> SERVICE.removeProjectMember(project.getProjectId(), 21L, 99L));
        assertEquals(HttpStatus.NOT_FOUND, missing.getCode());

        LOG_MAPPER.failNextInsert.set(true);
        assertThrows(IllegalStateException.class,
            () -> SERVICE.removeProjectMember(project.getProjectId(), 21L, 22L));

        assertNotNull(MEMBER_MAPPER.selectProjectMember(project.getProjectId(), 22L));
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void projectMemberRemovalRejectsCurrentRequirementOwner()
    {
        Project project = SERVICE.createProject("Requirement owner removal project", 21L);
        addMember(project, 22L, 7L, 0);
        JDBC.update("insert into pm_requirement (project_id, creator_id, status, is_deleted, create_time, update_time) "
            + "values (?, ?, 'todo', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", project.getProjectId(), 21L);
        Long requirementId = JDBC.queryForObject("select max(requirement_id) from pm_requirement", Long.class);
        JDBC.update("insert into pm_requirement_owner (requirement_id, user_id) values (?, ?)", requirementId, 22L);

        ServiceException error = assertThrows(ServiceException.class,
            () -> SERVICE.removeProjectMember(project.getProjectId(), 21L, 22L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
        assertNotNull(MEMBER_MAPPER.selectProjectMember(project.getProjectId(), 22L));
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void projectAdminCanRemoveMemberFromArchivedProject()
    {
        Project project = SERVICE.createProject("Archived member removal project", 21L);
        addMember(project, 22L, 7L, 0);
        SERVICE.updateProjectStatus(project.getProjectId(), 21L, true);

        SERVICE.removeProjectMember(project.getProjectId(), 21L, 22L);

        assertNull(MEMBER_MAPPER.selectProjectMember(project.getProjectId(), 22L));
    }

    @Test
    void projectMemberRemovalRejectsCurrentTaskOwner()
    {
        Project project = SERVICE.createProject("Task owner removal project", 21L);
        addMember(project, 22L, 7L, 0);
        JDBC.update("insert into pm_task (project_id, is_deleted) values (?, 0)", project.getProjectId());
        Long taskId = JDBC.queryForObject("select max(task_id) from pm_task", Long.class);
        JDBC.update("insert into pm_task_owner (task_id, user_id) values (?, ?)", taskId, 22L);

        ServiceException error = assertThrows(ServiceException.class,
            () -> SERVICE.removeProjectMember(project.getProjectId(), 21L, 22L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
        assertNotNull(MEMBER_MAPPER.selectProjectMember(project.getProjectId(), 22L));
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void projectMemberCanQueryOnlyTheirProjectLogs()
    {
        Project project = SERVICE.createProject("Log query project", 21L);
        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(project.getProjectId());
        log.setOperatorId(21L);
        log.setOperationType("PROJECT_NAME_UPDATE");
        LOG_MAPPER.insertProjectOperationLog(log);

        assertEquals(1, SERVICE.selectProjectOperationLogsForUser(project.getProjectId(), 21L).size());
        assertNull(SERVICE.selectProjectOperationLogsForUser(project.getProjectId(), 22L));
    }

    private ProjectMember addMember(Project project, Long userId, Long roleId, int isProjectAdmin)
    {
        ProjectMember member = new ProjectMember();
        member.setProjectId(project.getProjectId());
        member.setUserId(userId);
        member.setRoleId(roleId);
        member.setIsProjectAdmin(isProjectAdmin);
        member.setCreateTime(new java.util.Date());
        member.setUpdateTime(new java.util.Date());
        MEMBER_MAPPER.insertProjectMember(member);
        return member;
    }

    private void insertUser(Long userId, String userName, String nickName, String email, String status, String delFlag)
    {
        JDBC.update("insert into sys_user (user_id, user_name, nick_name, email, status, del_flag) "
            + "values (?, ?, ?, ?, ?, ?)", userId, userName, nickName, email, status, delFlag);
    }

    private int count(String table)
    {
        return JDBC.queryForObject("select count(*) from " + table, Integer.class);
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfiguration
    {
        @Bean
        DataSource dataSource()
        {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:pm0002;MODE=MySQL;DB_CLOSE_DELAY=-1");
            dataSource.setUser("sa");
            return dataSource;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource)
        {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource)
        {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        ProjectMapper projectMapper(JdbcTemplate jdbcTemplate)
        {
            return new TestProjectMapper(jdbcTemplate);
        }

        @Bean
        TestProjectMemberMapper projectMemberMapper(JdbcTemplate jdbcTemplate)
        {
            return new TestProjectMemberMapper(jdbcTemplate);
        }

        @Bean
        TestProjectOperationLogMapper projectOperationLogMapper()
        {
            return new TestProjectOperationLogMapper();
        }

        @Bean
        IProjectService projectService(ProjectMapper projectMapper, ProjectMemberMapper projectMemberMapper,
            ProjectOperationLogMapper projectOperationLogMapper)
        {
            return new ProjectServiceImpl(projectMapper, projectMemberMapper, projectOperationLogMapper);
        }

        @Bean
        Object initializeSchema(JdbcTemplate jdbcTemplate)
        {
            jdbcTemplate.execute("create table pm_project ("
                + "project_id bigint auto_increment primary key, "
                + "project_name varchar(255) not null, "
                + "project_name_key varchar(255) not null unique, "
                + "creator_id bigint not null, status varchar(16) not null default 'ACTIVE', "
                + "create_time timestamp not null, update_time timestamp not null)");
            jdbcTemplate.execute("create table pm_project_member ("
                + "project_id bigint not null, user_id bigint not null, role_id bigint null, is_project_admin integer not null, "
                + "create_time timestamp not null, update_time timestamp not null, "
                + "primary key (project_id, user_id))");
            jdbcTemplate.execute("create table sys_user ("
                + "user_id bigint primary key, user_name varchar(30), nick_name varchar(30), email varchar(50), "
                + "status varchar(1) not null default '0', del_flag varchar(1) not null default '0')");
            jdbcTemplate.execute("create table pm_requirement ("
                + "requirement_id bigint auto_increment primary key, project_id bigint not null, "
                + "creator_id bigint not null, current_version_id bigint null, status varchar(100) not null, "
                + "is_deleted integer not null default 0, create_time timestamp not null, update_time timestamp not null)");
            jdbcTemplate.execute("create table pm_requirement_owner ("
                + "requirement_id bigint not null, user_id bigint not null, "
                + "primary key(requirement_id, user_id))");
            jdbcTemplate.execute("create table pm_task ("
                + "task_id bigint auto_increment primary key, project_id bigint not null, "
                + "is_deleted integer not null default 0)");
            jdbcTemplate.execute("create table pm_task_owner ("
                + "task_id bigint not null, user_id bigint not null, primary key(task_id, user_id))");
            jdbcTemplate.execute("create table pm_task_version ("
                + "version_id bigint auto_increment primary key, task_id bigint not null)");
            jdbcTemplate.execute("create table pm_task_category ("
                + "task_id bigint not null, category_value varchar(100) not null, "
                + "primary key(task_id, category_value))");
            return new Object();
        }
    }

    static class TestProjectMapper implements ProjectMapper
    {
        private final JdbcTemplate jdbc;

        TestProjectMapper(JdbcTemplate jdbc)
        {
            this.jdbc = jdbc;
        }

        @Override
        public int insertProject(Project project)
        {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int rows = jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    "insert into pm_project (project_name, project_name_key, creator_id, create_time, update_time) "
                        + "values (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, project.getProjectName());
                statement.setString(2, project.getProjectNameKey());
                statement.setLong(3, project.getCreatorId());
                statement.setTimestamp(4, new java.sql.Timestamp(project.getCreateTime().getTime()));
                statement.setTimestamp(5, new java.sql.Timestamp(project.getUpdateTime().getTime()));
                return statement;
            }, keyHolder);
            project.setProjectId(keyHolder.getKey().longValue());
            return rows;
        }

        @Override
        public List<Project> selectProjectListForUser(Long userId, String projectName)
        {
            String sql = "select p.* from pm_project p inner join pm_project_member m "
                + "on m.project_id = p.project_id where m.user_id = ?";
            if (projectName == null)
            {
                return jdbc.query(sql, projectRowMapper(), userId);
            }
            return jdbc.query(sql + " and p.project_name like concat('%', ?, '%')", projectRowMapper(), userId,
                projectName);
        }

        @Override
        public Project selectProjectForUser(Long projectId, Long userId)
        {
            List<Project> projects = jdbc.query("select p.* from pm_project p inner join pm_project_member m "
                + "on m.project_id = p.project_id where p.project_id = ? and m.user_id = ?", projectRowMapper(),
                projectId, userId);
            return projects.isEmpty() ? null : projects.get(0);
        }

        @Override
        public Long lockProjectForUpdate(Long projectId)
        {
            return jdbc.query("select project_id from pm_project where project_id = ?",
                resultSet -> resultSet.next() ? resultSet.getLong(1) : null, projectId);
        }

        @Override
        public int updateProjectName(Long projectId, String projectName, String projectNameKey)
        {
            return jdbc.update("update pm_project set project_name = ?, project_name_key = ?, update_time = CURRENT_TIMESTAMP "
                + "where project_id = ?", projectName, projectNameKey, projectId);
        }

        @Override
        public int updateProjectStatus(Long projectId, String status)
        {
            return jdbc.update("update pm_project set status = ?, update_time = CURRENT_TIMESTAMP where project_id = ?",
                status, projectId);
        }

        private RowMapper<Project> projectRowMapper()
        {
            return (resultSet, rowNum) -> {
                Project project = new Project();
                project.setProjectId(resultSet.getLong("project_id"));
                project.setProjectName(resultSet.getString("project_name"));
                project.setProjectNameKey(resultSet.getString("project_name_key"));
                project.setCreatorId(resultSet.getLong("creator_id"));
                project.setStatus(resultSet.getString("status"));
                project.setCreateTime(resultSet.getTimestamp("create_time"));
                project.setUpdateTime(resultSet.getTimestamp("update_time"));
                return project;
            };
        }
    }

    static class TestProjectMemberMapper implements ProjectMemberMapper
    {
        private final JdbcTemplate jdbc;
        private final AtomicBoolean failNextInsert = new AtomicBoolean();
        private final java.util.Map<Long, String> roles = new java.util.HashMap<>();
        private final java.util.Set<Long> inactiveRoles = new java.util.HashSet<>();

        TestProjectMemberMapper(JdbcTemplate jdbc)
        {
            this.jdbc = jdbc;
        }

        @Override
        public int insertProjectMember(ProjectMember member)
        {
            if (failNextInsert.compareAndSet(true, false))
            {
                throw new IllegalStateException("Injected member write failure");
            }
            return jdbc.update("insert into pm_project_member "
                + "(project_id, user_id, role_id, is_project_admin, create_time, update_time) values (?, ?, ?, ?, ?, ?)",
                member.getProjectId(), member.getUserId(), member.getRoleId(), member.getIsProjectAdmin(),
                new java.sql.Timestamp(member.getCreateTime().getTime()),
                new java.sql.Timestamp(member.getUpdateTime().getTime()));
        }

        @Override
        public List<ProjectMember> selectProjectMembersForUser(Long projectId, Long userId)
        {
            return jdbc.query("select * from pm_project_member where project_id = ? and exists "
                + "(select 1 from pm_project_member where project_id = ? and user_id = ?)",
                (resultSet, rowNum) -> {
                    ProjectMember member = new ProjectMember();
                    member.setProjectId(resultSet.getLong("project_id"));
                    member.setUserId(resultSet.getLong("user_id"));
                    member.setRoleId((Long) resultSet.getObject("role_id"));
                    member.setIsProjectAdmin(resultSet.getInt("is_project_admin"));
                    return member;
                }, projectId, projectId, userId);
        }

        @Override
        public ProjectMember selectProjectMember(Long projectId, Long userId)
        {
            List<ProjectMember> members = jdbc.query(
                "select * from pm_project_member where project_id = ? and user_id = ?",
                (resultSet, rowNum) -> {
                    ProjectMember member = new ProjectMember();
                    member.setProjectId(resultSet.getLong("project_id"));
                    member.setUserId(resultSet.getLong("user_id"));
                    member.setRoleId((Long) resultSet.getObject("role_id"));
                    member.setIsProjectAdmin(resultSet.getInt("is_project_admin"));
                    member.setRoleName(roles.get(member.getRoleId()));
                    return member;
                }, projectId, userId);
            return members.isEmpty() ? null : members.get(0);
        }

        @Override
        public ProjectMember selectActiveProjectMemberUser(Long userId)
        {
            List<ProjectMember> users = jdbc.query(
                "select user_id, user_name, nick_name, email from sys_user "
                    + "where user_id = ? and status = '0' and del_flag = '0'",
                (resultSet, rowNum) -> {
                    ProjectMember user = new ProjectMember();
                    user.setUserId(resultSet.getLong("user_id"));
                    user.setUserName(resultSet.getString("user_name"));
                    user.setNickName(resultSet.getString("nick_name"));
                    user.setEmail(resultSet.getString("email"));
                    return user;
                }, userId);
            return users.isEmpty() ? null : users.get(0);
        }

        @Override
        public int updateProjectMemberRole(Long projectId, Long userId, Long roleId)
        {
            return jdbc.update("update pm_project_member set role_id = ? where project_id = ? and user_id = ?",
                roleId, projectId, userId);
        }

        @Override
        public int updateProjectMemberAdmin(Long projectId, Long userId, Integer isProjectAdmin)
        {
            return jdbc.update("update pm_project_member set is_project_admin = ? where project_id = ? and user_id = ?",
                isProjectAdmin, projectId, userId);
        }

        @Override
        public int deleteProjectMember(Long projectId, Long userId)
        {
            return jdbc.update("delete from pm_project_member where project_id = ? and user_id = ?",
                projectId, userId);
        }

        @Override
        public int countProjectAdmins(Long projectId)
        {
            return jdbc.queryForObject("select count(*) from pm_project_member "
                + "where project_id = ? and is_project_admin = 1", Integer.class, projectId);
        }

        @Override
        public int countRequirementOwnerReferences(Long projectId, Long userId)
        {
            return jdbc.queryForObject("select count(*) from pm_requirement_owner o "
                + "inner join pm_requirement r on r.requirement_id = o.requirement_id "
                + "where r.project_id = ? and r.is_deleted = 0 and o.user_id = ?", Integer.class,
                projectId, userId);
        }

        @Override
        public int countTaskOwnerReferences(Long projectId, Long userId)
        {
            return jdbc.queryForObject("select count(*) from pm_task_owner o "
                + "inner join pm_task t on t.task_id = o.task_id "
                + "where t.project_id = ? and t.is_deleted = 0 and o.user_id = ?", Integer.class,
                projectId, userId);
        }

        @Override
        public com.ruoyi.common.core.domain.entity.SysRole selectActiveProjectRole(Long roleId)
        {
            if (inactiveRoles.contains(roleId) || (!roles.isEmpty() && !roles.containsKey(roleId)))
            {
                return null;
            }
            com.ruoyi.common.core.domain.entity.SysRole role = new com.ruoyi.common.core.domain.entity.SysRole();
            role.setRoleId(roleId);
            role.setRoleName(roles.getOrDefault(roleId, "Role " + roleId));
            return role;
        }

        @Override
        public List<com.ruoyi.common.core.domain.entity.SysRole> selectActiveProjectRoles()
        {
            return roles.entrySet().stream().map(entry -> {
                com.ruoyi.common.core.domain.entity.SysRole role = new com.ruoyi.common.core.domain.entity.SysRole();
                role.setRoleId(entry.getKey());
                role.setRoleName(entry.getValue());
                return role;
            }).toList();
        }
    }

    static class TestProjectOperationLogMapper implements ProjectOperationLogMapper
    {
        private final List<ProjectOperationLog> logs = new java.util.ArrayList<>();
        private final AtomicBoolean failNextInsert = new AtomicBoolean();
        private ProjectOperationLog last;

        @Override
        public int insertProjectOperationLog(ProjectOperationLog log)
        {
            if (failNextInsert.compareAndSet(true, false))
            {
                throw new IllegalStateException("Injected project log write failure");
            }
            last = log;
            logs.add(log);
            return 1;
        }

        @Override
        public List<ProjectOperationLog> selectProjectOperationLogsForUser(Long projectId, Long userId)
        {
            return logs.stream().filter(log -> projectId.equals(log.getProjectId())).toList();
        }
    }
}
