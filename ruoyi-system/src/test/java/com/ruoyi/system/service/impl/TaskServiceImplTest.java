package com.ruoyi.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectOperationLog;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.system.domain.RequirementOwner;
import com.ruoyi.system.domain.RequirementVersion;
import com.ruoyi.system.domain.Task;
import com.ruoyi.system.domain.TaskCategory;
import com.ruoyi.system.domain.TaskOwner;
import com.ruoyi.system.domain.TaskVersion;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectOperationLogMapper;
import com.ruoyi.system.mapper.RequirementMapper;
import com.ruoyi.system.mapper.TaskMapper;
import com.ruoyi.system.service.ITaskService;

class TaskServiceImplTest
{
    private static final AnnotationConfigApplicationContext CONTEXT =
        new AnnotationConfigApplicationContext(TestConfiguration.class);
    private static final JdbcTemplate JDBC = CONTEXT.getBean(JdbcTemplate.class);
    private static final ITaskService SERVICE = CONTEXT.getBean(ITaskService.class);
    private static final ProjectMapper PROJECT_MAPPER = CONTEXT.getBean(ProjectMapper.class);
    private static final TestRequirementMapper REQUIREMENT_MAPPER = CONTEXT.getBean(TestRequirementMapper.class);
    private static final TestTaskMapper TASK_MAPPER = CONTEXT.getBean(TestTaskMapper.class);
    private static final TestLogMapper LOG_MAPPER = CONTEXT.getBean(TestLogMapper.class);

    @AfterAll
    static void closeContext()
    {
        CONTEXT.close();
    }

    @BeforeEach
    void clean()
    {
        JDBC.update("delete from pm_task_owner");
        JDBC.update("delete from pm_task_category");
        JDBC.update("delete from pm_task_version");
        JDBC.update("delete from pm_task");
        TASK_MAPPER.statuses.clear();
        TASK_MAPPER.categories.clear();
        TASK_MAPPER.users.clear();
        REQUIREMENT_MAPPER.memberIds.clear();
        LOG_MAPPER.logs.clear();
        LOG_MAPPER.failNextInsert.set(false);

        Project active = project(Project.STATUS_ACTIVE);
        when(PROJECT_MAPPER.selectProjectForUser(41L, 21L)).thenReturn(active);
        when(PROJECT_MAPPER.selectProjectForUser(41L, 22L)).thenReturn(null);
        when(PROJECT_MAPPER.selectProjectForUser(41L, 99L)).thenReturn(project(Project.STATUS_ARCHIVED));

        Requirement requirement = new Requirement();
        requirement.setRequirementId(7L);
        requirement.setProjectId(41L);
        requirement.setCurrentVersionId(501L);
        REQUIREMENT_MAPPER.requirement = requirement;
        REQUIREMENT_MAPPER.memberIds.addAll(Set.of(21L, 23L));
        TASK_MAPPER.addStatus("todo", "待处理");
        TASK_MAPPER.addCategory("dev", "开发");
        TASK_MAPPER.users.put(21L, new String[] {"alice", "Alice", "alice@example.com"});
        TASK_MAPPER.users.put(23L, new String[] {"carol", "Carol", "carol@example.com"});
    }

    @Test
    void memberCanCreateTaskWithVersionCategoriesOwnersAndLog()
    {
        Task task = SERVICE.createTask(41L, 21L, 7L, "  登录接口  ", "  实现登录接口  ",
            "todo", List.of("dev"), List.of(21L, 23L));

        assertEquals("登录接口", task.getTitle());
        assertEquals("实现登录接口", task.getDescription());
        assertEquals(1, task.getCurrentVersionNo());
        assertEquals(501L, task.getRequirementVersionId());
        assertEquals(List.of("dev"), task.getCategories().stream()
            .map(TaskCategory::getCategoryValue).toList());
        assertEquals(List.of(21L, 23L), task.getOwners().stream()
            .map(TaskOwner::getUserId).toList());
        assertEquals(1, count("pm_task"));
        assertEquals(1, count("pm_task_version"));
        assertEquals(1, count("pm_task_category"));
        assertEquals(2, count("pm_task_owner"));
        assertEquals("TASK_CREATE", LOG_MAPPER.logs.get(0).getOperationType());
    }

    @Test
    void taskCreationValidatesRequirementMembershipArchiveDictionariesAndOwners()
    {
        ServiceException nonMember = assertThrows(ServiceException.class,
            () -> SERVICE.createTask(41L, 22L, 7L, "Title", "Description", "todo", List.of("dev"), List.of(21L)));
        ServiceException archived = assertThrows(ServiceException.class,
            () -> SERVICE.createTask(41L, 99L, 7L, "Title", "Description", "todo", List.of("dev"), List.of(21L)));
        ServiceException missingRequirement = assertThrows(ServiceException.class,
            () -> SERVICE.createTask(41L, 21L, 8L, "Title", "Description", "todo", List.of("dev"), List.of(21L)));
        ServiceException invalidStatus = assertThrows(ServiceException.class,
            () -> SERVICE.createTask(41L, 21L, 7L, "Title", "Description", "missing", List.of("dev"), List.of(21L)));
        ServiceException invalidCategory = assertThrows(ServiceException.class,
            () -> SERVICE.createTask(41L, 21L, 7L, "Title", "Description", "todo", List.of("missing"), List.of(21L)));
        ServiceException invalidOwner = assertThrows(ServiceException.class,
            () -> SERVICE.createTask(41L, 21L, 7L, "Title", "Description", "todo", List.of("dev"), List.of(22L)));
        ServiceException noCategory = assertThrows(ServiceException.class,
            () -> SERVICE.createTask(41L, 21L, 7L, "Title", "Description", "todo", List.of(), List.of(21L)));
        ServiceException noOwner = assertThrows(ServiceException.class,
            () -> SERVICE.createTask(41L, 21L, 7L, "Title", "Description", "todo", List.of("dev"), List.of()));

        assertEquals(HttpStatus.NOT_FOUND, nonMember.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, archived.getCode());
        assertEquals(HttpStatus.NOT_FOUND, missingRequirement.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, invalidStatus.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, invalidCategory.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, invalidOwner.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, noCategory.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, noOwner.getCode());
        assertEquals(0, count("pm_task"));
    }

    @Test
    void duplicateCategoriesAndOwnersAreStoredOnce()
    {
        Task task = SERVICE.createTask(41L, 21L, 7L, "Title", "Description", "todo",
            List.of("dev", "dev"), List.of(21L, 21L));

        assertEquals(1, task.getCategories().size());
        assertEquals(1, task.getOwners().size());
        assertEquals(1, count("pm_task_category"));
        assertEquals(1, count("pm_task_owner"));
    }

    @Test
    void taskAndRelationsRollBackWhenLogCannotBeWritten()
    {
        LOG_MAPPER.failNextInsert.set(true);

        assertThrows(IllegalStateException.class,
            () -> SERVICE.createTask(41L, 21L, 7L, "Title", "Description", "todo", List.of("dev"), List.of(21L)));

        assertEquals(0, count("pm_task"));
        assertEquals(0, count("pm_task_version"));
        assertEquals(0, count("pm_task_category"));
        assertEquals(0, count("pm_task_owner"));
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void optionsRequireProjectMembership()
    {
        assertEquals(1, SERVICE.selectActiveStatuses(41L, 21L).size());
        assertEquals(1, SERVICE.selectActiveCategories(41L, 21L).size());
        assertNull(SERVICE.selectActiveStatuses(41L, 22L));
        assertNull(SERVICE.selectActiveCategories(41L, 22L));
    }

    @Test
    void memberCanListTasksWithOwnersAndNonMemberIsDenied()
    {
        SERVICE.createTask(41L, 21L, 7L, "Title", "Description", "todo", List.of("dev"), List.of(21L));

        List<Task> tasks = SERVICE.selectTasksForUser(41L, 21L);

        assertEquals(1, tasks.size());
        assertEquals("Title", tasks.get(0).getTitle());
        assertEquals(List.of(21L), tasks.get(0).getOwners().stream().map(TaskOwner::getUserId).toList());
        assertNull(SERVICE.selectTasksForUser(41L, 22L));
    }

    private Project project(String status)
    {
        Project project = new Project();
        project.setProjectId(41L);
        project.setProjectName("Project");
        project.setStatus(status);
        return project;
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
            dataSource.setURL("jdbc:h2:mem:pm0013;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
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
        ProjectMapper projectMapper()
        {
            return mock(ProjectMapper.class);
        }

        @Bean
        TestRequirementMapper requirementMapper()
        {
            return new TestRequirementMapper();
        }

        @Bean
        TestTaskMapper taskMapper(JdbcTemplate jdbcTemplate)
        {
            return new TestTaskMapper(jdbcTemplate);
        }

        @Bean
        TestLogMapper projectOperationLogMapper()
        {
            return new TestLogMapper();
        }

        @Bean
        ITaskService taskService(ProjectMapper projectMapper, RequirementMapper requirementMapper,
            TaskMapper taskMapper, ProjectOperationLogMapper projectOperationLogMapper)
        {
            return new TaskServiceImpl(projectMapper, requirementMapper, taskMapper, projectOperationLogMapper);
        }

        @Bean
        Object initializeSchema(JdbcTemplate jdbcTemplate)
        {
            jdbcTemplate.execute("create table pm_task (task_id bigint auto_increment primary key, "
                + "project_id bigint, requirement_id bigint, creator_id bigint, current_version_id bigint, "
                + "status varchar(100), is_deleted integer, create_time timestamp, update_time timestamp)");
            jdbcTemplate.execute("create table pm_task_version (version_id bigint auto_increment primary key, "
                + "task_id bigint, version_no integer, title varchar(255), description clob, "
                + "requirement_version_id bigint, created_by bigint, create_time timestamp)");
            jdbcTemplate.execute("create table pm_task_category (task_id bigint, category_value varchar(100), "
                + "primary key(task_id, category_value))");
            jdbcTemplate.execute("create table pm_task_owner (task_id bigint, user_id bigint, "
                + "primary key(task_id, user_id))");
            return new Object();
        }
    }

    static class TestRequirementMapper implements RequirementMapper
    {
        private Requirement requirement;
        private final Set<Long> memberIds = new java.util.LinkedHashSet<>();

        @Override
        public Requirement selectRequirementForUser(Long projectId, Long requirementId, Long userId)
        {
            if (requirement == null || !projectId.equals(requirement.getProjectId())
                || !requirementId.equals(requirement.getRequirementId()) || !memberIds.contains(userId))
            {
                return null;
            }
            return requirement;
        }

        @Override
        public List<RequirementVersion> selectRequirementVersionsForUser(Long projectId, Long requirementId,
            Long userId)
        {
            return List.of();
        }

        @Override
        public List<RequirementOwner> selectProjectMembersByIds(Long projectId, List<Long> userIds)
        {
            if (userIds == null)
            {
                return List.of();
            }
            return userIds.stream().filter(memberIds::contains).map(userId -> {
                RequirementOwner owner = new RequirementOwner();
                owner.setUserId(userId);
                return owner;
            }).toList();
        }

        @Override public int insertRequirement(Requirement value) { return 0; }
        @Override public int insertRequirementVersion(com.ruoyi.system.domain.RequirementVersion value) { return 0; }
        @Override public int updateCurrentVersion(Long requirementId, Long versionId) { return 0; }
        @Override public int insertRequirementOwner(Long requirementId, Long userId) { return 0; }
        @Override public List<Requirement> selectRequirementsForUser(Long projectId, Long userId) { return List.of(); }
        @Override public List<RequirementOwner> selectRequirementOwners(Long requirementId) { return List.of(); }
        @Override public SysDictData selectActiveRequirementStatus(String status) { return null; }
        @Override public List<SysDictData> selectActiveRequirementStatuses() { return List.of(); }
    }

    static class TestTaskMapper implements TaskMapper
    {
        private final JdbcTemplate jdbc;
        private final Map<String, SysDictData> statuses = new LinkedHashMap<>();
        private final Map<String, SysDictData> categories = new LinkedHashMap<>();
        private final Map<Long, String[]> users = new HashMap<>();

        TestTaskMapper(JdbcTemplate jdbc)
        {
            this.jdbc = jdbc;
        }

        void addStatus(String value, String label)
        {
            statuses.put(value, dict("pm_task_status", value, label));
        }

        void addCategory(String value, String label)
        {
            categories.put(value, dict("pm_task_category", value, label));
        }

        private SysDictData dict(String type, String value, String label)
        {
            SysDictData data = new SysDictData();
            data.setDictType(type);
            data.setDictValue(value);
            data.setDictLabel(label);
            data.setStatus("0");
            return data;
        }

        @Override
        public int insertTask(Task task)
        {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int rows = jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    "insert into pm_task (project_id, requirement_id, creator_id, current_version_id, status, "
                        + "is_deleted, create_time, update_time) values (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, task.getProjectId());
                statement.setLong(2, task.getRequirementId());
                statement.setLong(3, task.getCreatorId());
                statement.setObject(4, task.getCurrentVersionId());
                statement.setString(5, task.getStatus());
                statement.setInt(6, task.getIsDeleted());
                statement.setTimestamp(7, new java.sql.Timestamp(task.getCreateTime().getTime()));
                statement.setTimestamp(8, new java.sql.Timestamp(task.getUpdateTime().getTime()));
                return statement;
            }, keyHolder);
            task.setTaskId(keyHolder.getKey().longValue());
            return rows;
        }

        @Override
        public int insertTaskVersion(TaskVersion version)
        {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int rows = jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    "insert into pm_task_version (task_id, version_no, title, description, requirement_version_id, "
                        + "created_by, create_time) values (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, version.getTaskId());
                statement.setInt(2, version.getVersionNo());
                statement.setString(3, version.getTitle());
                statement.setString(4, version.getDescription());
                statement.setLong(5, version.getRequirementVersionId());
                statement.setLong(6, version.getCreatedBy());
                statement.setTimestamp(7, new java.sql.Timestamp(version.getCreateTime().getTime()));
                return statement;
            }, keyHolder);
            version.setVersionId(keyHolder.getKey().longValue());
            return rows;
        }

        @Override public int updateCurrentVersion(Long taskId, Long versionId)
        { return jdbc.update("update pm_task set current_version_id = ? where task_id = ?", versionId, taskId); }
        @Override public int insertTaskCategory(Long taskId, String categoryValue)
        { return jdbc.update("insert into pm_task_category (task_id, category_value) values (?, ?)", taskId, categoryValue); }
        @Override public int insertTaskOwner(Long taskId, Long userId)
        { return jdbc.update("insert into pm_task_owner (task_id, user_id) values (?, ?)", taskId, userId); }

        @Override
        public Task selectTaskForUser(Long projectId, Long taskId, Long userId)
        {
            List<Task> tasks = jdbc.query("select t.*, v.version_no, v.title, v.description, v.requirement_version_id "
                + "from pm_task t inner join pm_task_version v on v.version_id = t.current_version_id "
                + "where t.project_id = ? and t.task_id = ? and t.is_deleted = 0",
                (rs, rowNum) -> {
                    Task task = new Task();
                    task.setTaskId(rs.getLong("task_id"));
                    task.setProjectId(rs.getLong("project_id"));
                    task.setRequirementId(rs.getLong("requirement_id"));
                    task.setCreatorId(rs.getLong("creator_id"));
                    task.setCurrentVersionId(rs.getLong("current_version_id"));
                    task.setCurrentVersionNo(rs.getInt("version_no"));
                    task.setRequirementVersionId(rs.getLong("requirement_version_id"));
                    task.setRequirementVersionNo(1);
                    task.setTitle(rs.getString("title"));
                    task.setDescription(rs.getString("description"));
                    task.setStatus(rs.getString("status"));
                    task.setStatusLabel(statuses.get(task.getStatus()).getDictLabel());
                    task.setIsDeleted(rs.getInt("is_deleted"));
                    task.setCreateTime(rs.getTimestamp("create_time"));
                    task.setUpdateTime(rs.getTimestamp("update_time"));
                    return task;
                }, projectId, taskId);
            if (tasks.isEmpty()) return null;
            return tasks.get(0);
        }

        @Override
        public List<Task> selectTasksForUser(Long projectId, Long userId)
        {
            return jdbc.query("select t.*, v.version_no, v.title, v.description, v.requirement_version_id "
                + "from pm_task t inner join pm_task_version v on v.version_id = t.current_version_id "
                + "where t.project_id = ? and t.is_deleted = 0 order by t.create_time desc, t.task_id desc",
                (rs, rowNum) -> {
                    Task task = new Task();
                    task.setTaskId(rs.getLong("task_id"));
                    task.setProjectId(rs.getLong("project_id"));
                    task.setRequirementId(rs.getLong("requirement_id"));
                    task.setCreatorId(rs.getLong("creator_id"));
                    task.setCurrentVersionId(rs.getLong("current_version_id"));
                    task.setCurrentVersionNo(rs.getInt("version_no"));
                    task.setRequirementVersionId(rs.getLong("requirement_version_id"));
                    task.setRequirementVersionNo(1);
                    task.setTitle(rs.getString("title"));
                    task.setDescription(rs.getString("description"));
                    task.setStatus(rs.getString("status"));
                    task.setStatusLabel(statuses.get(task.getStatus()).getDictLabel());
                    task.setIsDeleted(rs.getInt("is_deleted"));
                    task.setCreateTime(rs.getTimestamp("create_time"));
                    task.setUpdateTime(rs.getTimestamp("update_time"));
                    return task;
                }, projectId);
        }

        @Override
        public List<TaskCategory> selectTaskCategories(Long taskId)
        {
            return jdbc.query("select task_id, category_value from pm_task_category where task_id = ? "
                + "order by category_value", (rs, rowNum) -> {
                    TaskCategory category = new TaskCategory();
                    category.setTaskId(rs.getLong("task_id"));
                    category.setCategoryValue(rs.getString("category_value"));
                    category.setCategoryLabel(categories.get(category.getCategoryValue()).getDictLabel());
                    return category;
                }, taskId);
        }

        @Override
        public List<TaskOwner> selectTaskOwners(Long taskId)
        {
            return jdbc.query("select task_id, user_id from pm_task_owner where task_id = ? order by user_id",
                (rs, rowNum) -> {
                    TaskOwner owner = new TaskOwner();
                    owner.setTaskId(rs.getLong("task_id"));
                    owner.setUserId(rs.getLong("user_id"));
                    String[] user = users.get(owner.getUserId());
                    if (user != null) { owner.setUserName(user[0]); owner.setNickName(user[1]); owner.setEmail(user[2]); }
                    return owner;
                }, taskId);
        }

        @Override public List<SysDictData> selectActiveTaskStatuses() { return new ArrayList<>(statuses.values()); }
        @Override public List<SysDictData> selectActiveTaskCategories() { return new ArrayList<>(categories.values()); }
    }

    static class TestLogMapper implements ProjectOperationLogMapper
    {
        private final List<ProjectOperationLog> logs = new ArrayList<>();
        private final AtomicBoolean failNextInsert = new AtomicBoolean();

        @Override
        public int insertProjectOperationLog(ProjectOperationLog log)
        {
            if (failNextInsert.compareAndSet(true, false))
            {
                throw new IllegalStateException("Injected log failure");
            }
            logs.add(log);
            return 1;
        }

        @Override
        public List<ProjectOperationLog> selectProjectOperationLogsForUser(Long projectId, Long userId)
        {
            return logs;
        }
    }
}
