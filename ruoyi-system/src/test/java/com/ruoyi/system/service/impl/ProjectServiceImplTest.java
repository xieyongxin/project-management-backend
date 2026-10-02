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
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectMemberMapper;
import com.ruoyi.system.service.IProjectService;

class ProjectServiceImplTest
{
    private static final AnnotationConfigApplicationContext CONTEXT =
        new AnnotationConfigApplicationContext(TestConfiguration.class);
    private static final JdbcTemplate JDBC = CONTEXT.getBean(JdbcTemplate.class);
    private static final IProjectService SERVICE = CONTEXT.getBean(IProjectService.class);
    private static final TestProjectMemberMapper MEMBER_MAPPER = CONTEXT.getBean(TestProjectMemberMapper.class);

    @AfterAll
    static void closeContext()
    {
        CONTEXT.close();
    }

    @BeforeEach
    void cleanTables()
    {
        MEMBER_MAPPER.failNextInsert.set(false);
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
        IProjectService projectService(ProjectMapper projectMapper, ProjectMemberMapper projectMemberMapper)
        {
            return new ProjectServiceImpl(projectMapper, projectMemberMapper);
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
                + "project_id bigint not null, user_id bigint not null, is_project_admin integer not null, "
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
                + "(project_id, user_id, is_project_admin, create_time, update_time) values (?, ?, ?, ?, ?)",
                member.getProjectId(), member.getUserId(), member.getIsProjectAdmin(),
                new java.sql.Timestamp(member.getCreateTime().getTime()),
                new java.sql.Timestamp(member.getUpdateTime().getTime()));
        }
    }
}
