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
        JDBC.update("delete from pm_project_member");
        JDBC.update("delete from pm_project");
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
                + "creator_id bigint not null, create_time timestamp not null, update_time timestamp not null)");
            jdbcTemplate.execute("create table pm_project_member ("
                + "project_id bigint not null, user_id bigint not null, role_id bigint null, is_project_admin integer not null, "
                + "create_time timestamp not null, update_time timestamp not null, "
                + "primary key (project_id, user_id))");
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

        private RowMapper<Project> projectRowMapper()
        {
            return (resultSet, rowNum) -> {
                Project project = new Project();
                project.setProjectId(resultSet.getLong("project_id"));
                project.setProjectName(resultSet.getString("project_name"));
                project.setProjectNameKey(resultSet.getString("project_name_key"));
                project.setCreatorId(resultSet.getLong("creator_id"));
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
        public int updateProjectMemberRole(Long projectId, Long userId, Long roleId)
        {
            return jdbc.update("update pm_project_member set role_id = ? where project_id = ? and user_id = ?",
                roleId, projectId, userId);
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
    }
}
