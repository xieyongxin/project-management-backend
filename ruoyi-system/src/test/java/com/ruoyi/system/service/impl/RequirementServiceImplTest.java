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
import java.util.List;
import java.util.Map;
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
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectOperationLogMapper;
import com.ruoyi.system.mapper.RequirementMapper;
import com.ruoyi.system.service.IRequirementService;

class RequirementServiceImplTest
{
    private static final AnnotationConfigApplicationContext CONTEXT =
        new AnnotationConfigApplicationContext(TestConfiguration.class);
    private static final JdbcTemplate JDBC = CONTEXT.getBean(JdbcTemplate.class);
    private static final IRequirementService SERVICE = CONTEXT.getBean(IRequirementService.class);
    private static final ProjectMapper PROJECT_MAPPER = CONTEXT.getBean(ProjectMapper.class);
    private static final TestRequirementMapper REQUIREMENT_MAPPER = CONTEXT.getBean(TestRequirementMapper.class);
    private static final TestLogMapper LOG_MAPPER = CONTEXT.getBean(TestLogMapper.class);

    @AfterAll
    static void closeContext()
    {
        CONTEXT.close();
    }

    @BeforeEach
    void clean()
    {
        JDBC.update("delete from pm_requirement_owner");
        JDBC.update("delete from pm_requirement_version");
        JDBC.update("delete from pm_requirement");
        JDBC.update("delete from pm_project_member");
        JDBC.update("delete from pm_project");
        JDBC.update("delete from sys_user");
        REQUIREMENT_MAPPER.statuses.clear();
        REQUIREMENT_MAPPER.inactiveStatuses.clear();
        LOG_MAPPER.logs.clear();
        LOG_MAPPER.failNextInsert.set(false);
        Project project = project(Project.STATUS_ACTIVE);
        when(PROJECT_MAPPER.selectProjectForUser(41L, 21L)).thenReturn(project);
        when(PROJECT_MAPPER.selectProjectForUser(41L, 22L)).thenReturn(null);
        when(PROJECT_MAPPER.selectProjectForUser(41L, 99L)).thenReturn(project(Project.STATUS_ARCHIVED));
        insertUser(21L, "alice", "Alice", "alice@example.com");
        insertUser(22L, "bob", "Bob", "bob@example.com");
        insertUser(23L, "carol", "Carol", "carol@example.com");
        insertProjectMember(41L, 21L);
        insertProjectMember(41L, 23L);
        REQUIREMENT_MAPPER.addStatus("todo", "待处理");
    }

    @Test
    void memberCanCreateRequirementWithInitialVersionAndOwners()
    {
        Requirement requirement = SERVICE.createRequirement(41L, 21L, "  登录需求  ", "  <p>正文</p>  ",
            "todo", List.of(21L, 23L));

        assertEquals("登录需求", requirement.getTitle());
        assertEquals("<p>正文</p>", requirement.getContent());
        assertEquals("todo", requirement.getStatus());
        assertEquals("待处理", requirement.getStatusLabel());
        assertEquals(1, requirement.getCurrentVersionNo());
        assertEquals(List.of(21L, 23L), requirement.getOwners().stream()
            .map(RequirementOwner::getUserId).toList());
        assertEquals(1, count("pm_requirement"));
        assertEquals(1, count("pm_requirement_version"));
        assertEquals(2, count("pm_requirement_owner"));
        assertEquals("REQUIREMENT_CREATE", LOG_MAPPER.logs.get(0).getOperationType());
    }

    @Test
    void memberCanReadRequirementDetailAndVersionHistoryWithProjectIsolation()
    {
        Requirement requirement = SERVICE.createRequirement(41L, 21L, "登录需求", "正文 1",
            "todo", List.of(21L));
        RequirementVersion second = new RequirementVersion();
        second.setRequirementId(requirement.getRequirementId());
        second.setVersionNo(2);
        second.setTitle("登录需求 2");
        second.setContent("正文 2");
        second.setAttachmentSnapshot("[]");
        second.setCreatedBy(21L);
        second.setCreateTime(new Date());
        assertEquals(1, REQUIREMENT_MAPPER.insertRequirementVersion(second));

        Requirement selected = SERVICE.selectRequirementForUser(41L, requirement.getRequirementId(), 21L);
        List<RequirementVersion> versions = SERVICE.selectRequirementVersionsForUser(41L,
            requirement.getRequirementId(), 21L);

        assertEquals("登录需求", selected.getTitle());
        assertEquals(2, versions.size());
        assertEquals(List.of(1, 2), versions.stream().map(RequirementVersion::getVersionNo).toList());
        assertNull(SERVICE.selectRequirementForUser(41L, requirement.getRequirementId(), 22L));
        assertNull(SERVICE.selectRequirementVersionsForUser(41L, requirement.getRequirementId(), 22L));
        assertNull(SERVICE.selectRequirementVersionsForUser(41L, 999L, 21L));
    }

    @Test
    void memberCanCompareTwoRequirementVersionsAndVersionsMustBelongToRequirement()
    {
        Requirement created = SERVICE.createRequirement(41L, 21L, "旧标题", "旧正文", "todo", List.of(21L));
        RequirementVersion second = new RequirementVersion();
        second.setRequirementId(created.getRequirementId());
        second.setVersionNo(2);
        second.setTitle("新标题");
        second.setContent("新正文");
        second.setAttachmentSnapshot("[{\"name\":\"new.txt\"}]");
        second.setCreatedBy(21L);
        second.setCreateTime(new Date());
        REQUIREMENT_MAPPER.insertRequirementVersion(second);

        List<RequirementVersion> comparison = SERVICE.compareRequirementVersionsForUser(41L,
            created.getRequirementId(), created.getCurrentVersionId(), second.getVersionId(), 21L);

        assertEquals(List.of("旧标题", "新标题"), comparison.stream().map(RequirementVersion::getTitle).toList());
        assertEquals("[{\"name\":\"new.txt\"}]", comparison.get(1).getAttachmentSnapshot());
        assertNull(SERVICE.compareRequirementVersionsForUser(41L, created.getRequirementId(),
            501L, second.getVersionId(), 21L));
        assertNull(SERVICE.compareRequirementVersionsForUser(41L, created.getRequirementId(),
            created.getCurrentVersionId(), 999L, 21L));
        assertNull(SERVICE.compareRequirementVersionsForUser(41L, created.getRequirementId(),
            created.getCurrentVersionId(), second.getVersionId(), 22L));
    }

    @Test
    void comparingSameRequirementVersionIsRejected()
    {
        Requirement created = SERVICE.createRequirement(41L, 21L, "标题", "正文", "todo", List.of(21L));
        ServiceException error = assertThrows(ServiceException.class,
            () -> SERVICE.compareRequirementVersionsForUser(41L, created.getRequirementId(),
                created.getCurrentVersionId(), created.getCurrentVersionId(), 21L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
        assertNull(SERVICE.compareRequirementVersionsForUser(41L, 999L, 18L, 18L, 21L));
    }

    @Test
    void requirementCreationValidatesMembershipStatusAndArchive()
    {
        ServiceException nonMember = assertThrows(ServiceException.class,
            () -> SERVICE.createRequirement(41L, 22L, "Title", "Body", "todo", List.of(21L)));
        ServiceException archived = assertThrows(ServiceException.class,
            () -> SERVICE.createRequirement(41L, 99L, "Title", "Body", "todo", List.of(21L)));
        ServiceException invalidStatus = assertThrows(ServiceException.class,
            () -> SERVICE.createRequirement(41L, 21L, "Title", "Body", "missing", List.of(21L)));
        ServiceException invalidOwner = assertThrows(ServiceException.class,
            () -> SERVICE.createRequirement(41L, 21L, "Title", "Body", "todo", List.of(22L)));
        ServiceException noOwner = assertThrows(ServiceException.class,
            () -> SERVICE.createRequirement(41L, 21L, "Title", "Body", "todo", List.of()));

        assertEquals(HttpStatus.NOT_FOUND, nonMember.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, archived.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, invalidStatus.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, invalidOwner.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, noOwner.getCode());
        assertEquals(0, count("pm_requirement"));
    }

    @Test
    void requirementCreationRejectsBlankContentAndDuplicateOwners()
    {
        ServiceException blankTitle = assertThrows(ServiceException.class,
            () -> SERVICE.createRequirement(41L, 21L, "  ", "Body", "todo", List.of(21L)));
        ServiceException blankContent = assertThrows(ServiceException.class,
            () -> SERVICE.createRequirement(41L, 21L, "Title", " <p></p> ", "todo", List.of(21L)));

        assertEquals(HttpStatus.BAD_REQUEST, blankTitle.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, blankContent.getCode());
        Requirement requirement = SERVICE.createRequirement(41L, 21L, "Title", "Body", "todo", List.of(21L, 21L));
        assertEquals(1, requirement.getOwners().size());
    }

    @Test
    void requirementAndOwnersRollBackWhenLogCannotBeWritten()
    {
        LOG_MAPPER.failNextInsert.set(true);

        assertThrows(IllegalStateException.class,
            () -> SERVICE.createRequirement(41L, 21L, "Title", "Body", "todo", List.of(21L)));

        assertEquals(0, count("pm_requirement"));
        assertEquals(0, count("pm_requirement_version"));
        assertEquals(0, count("pm_requirement_owner"));
        assertEquals(0, LOG_MAPPER.logs.size());
    }

    @Test
    void listAndStatusesRequireProjectMembership()
    {
        SERVICE.createRequirement(41L, 21L, "Title", "Body", "todo", List.of(21L));

        assertEquals(1, SERVICE.selectRequirementsForUser(41L, 21L).size());
        assertNull(SERVICE.selectRequirementsForUser(41L, 22L));
        assertEquals(1, SERVICE.selectActiveStatuses(41L, 21L).size());
        assertNull(SERVICE.selectActiveStatuses(41L, 22L));
    }

    @Test
    void memberCanChangeRequirementStatusWithoutCreatingVersion()
    {
        REQUIREMENT_MAPPER.addStatus("doing", "进行中");
        REQUIREMENT_MAPPER.addStatus("done", "已完成");
        Requirement requirement = SERVICE.createRequirement(41L, 21L, "Title", "Body", "todo", List.of(21L));
        int versionsBefore = count("pm_requirement_version");

        Requirement doing = SERVICE.updateRequirementStatus(41L, requirement.getRequirementId(), 21L, " doing ");
        Requirement done = SERVICE.updateRequirementStatus(41L, requirement.getRequirementId(), 21L, "done");

        assertEquals("doing", doing.getStatus());
        assertEquals("进行中", doing.getStatusLabel());
        assertEquals("done", done.getStatus());
        assertEquals("REQUIREMENT_STATUS_UPDATE", LOG_MAPPER.logs.get(1).getOperationType());
        assertEquals("REQUIREMENT_STATUS_UPDATE", LOG_MAPPER.logs.get(2).getOperationType());
        assertEquals("更新需求状态：待处理 -> 进行中", LOG_MAPPER.logs.get(1).getDetail());
        assertEquals("更新需求状态：进行中 -> 已完成", LOG_MAPPER.logs.get(2).getDetail());
        assertEquals(versionsBefore, count("pm_requirement_version"));
    }

    @Test
    void requirementStatusLogKeepsLabelForPreviouslyDisabledStatus()
    {
        Requirement created = SERVICE.createRequirement(41L, 21L, "Title", "Body", "todo", List.of(21L));
        REQUIREMENT_MAPPER.addInactiveStatus("legacy", "旧需求状态");
        REQUIREMENT_MAPPER.addStatus("done", "已完成");
        JDBC.update("update pm_requirement set status = ? where requirement_id = ?", "legacy",
            created.getRequirementId());

        SERVICE.updateRequirementStatus(41L, created.getRequirementId(), 21L, "done");

        assertEquals("更新需求状态：旧需求状态 -> 已完成", LOG_MAPPER.logs.get(1).getDetail());
    }

    @Test
    void unchangedRequirementStatusDoesNotWriteLogOrVersion()
    {
        Requirement requirement = SERVICE.createRequirement(41L, 21L, "Title", "Body", "todo", List.of(21L));
        int versionsBefore = count("pm_requirement_version");
        int logsBefore = LOG_MAPPER.logs.size();

        Requirement unchanged = SERVICE.updateRequirementStatus(41L, requirement.getRequirementId(), 21L, "todo");

        assertEquals("todo", unchanged.getStatus());
        assertEquals(logsBefore, LOG_MAPPER.logs.size());
        assertEquals(versionsBefore, count("pm_requirement_version"));
    }

    @Test
    void requirementStatusUpdateEnforcesMembershipRequirementArchiveAndActiveStatus()
    {
        REQUIREMENT_MAPPER.addInactiveStatus("paused", "已停用");
        Requirement requirement = SERVICE.createRequirement(41L, 21L, "Title", "Body", "todo", List.of(21L));

        ServiceException nonMember = assertThrows(ServiceException.class,
            () -> SERVICE.updateRequirementStatus(41L, requirement.getRequirementId(), 22L, "todo"));
        ServiceException missingRequirement = assertThrows(ServiceException.class,
            () -> SERVICE.updateRequirementStatus(41L, 999L, 21L, "todo"));
        ServiceException archived = assertThrows(ServiceException.class,
            () -> SERVICE.updateRequirementStatus(41L, requirement.getRequirementId(), 99L, "todo"));
        ServiceException invalid = assertThrows(ServiceException.class,
            () -> SERVICE.updateRequirementStatus(41L, requirement.getRequirementId(), 21L, "missing"));
        ServiceException inactive = assertThrows(ServiceException.class,
            () -> SERVICE.updateRequirementStatus(41L, requirement.getRequirementId(), 21L, "paused"));

        assertEquals(HttpStatus.NOT_FOUND, nonMember.getCode());
        assertEquals(HttpStatus.NOT_FOUND, missingRequirement.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, archived.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, invalid.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, inactive.getCode());
    }

    @Test
    void requirementStatusAndLogRollBackTogetherWhenLogFails()
    {
        REQUIREMENT_MAPPER.addStatus("doing", "进行中");
        Requirement requirement = SERVICE.createRequirement(41L, 21L, "Title", "Body", "todo", List.of(21L));
        LOG_MAPPER.failNextInsert.set(true);

        assertThrows(IllegalStateException.class,
            () -> SERVICE.updateRequirementStatus(41L, requirement.getRequirementId(), 21L, "doing"));

        assertEquals("todo", JDBC.queryForObject("select status from pm_requirement where requirement_id = ?",
            String.class, requirement.getRequirementId()));
        assertEquals(1, LOG_MAPPER.logs.size());
    }

    private Project project(String status)
    {
        Project project = new Project();
        project.setProjectId(41L);
        project.setProjectName("Project");
        project.setStatus(status);
        return project;
    }

    private void insertUser(Long userId, String userName, String nickName, String email)
    {
        JDBC.update("insert into sys_user (user_id, user_name, nick_name, email) values (?, ?, ?, ?)",
            userId, userName, nickName, email);
    }

    private void insertProjectMember(Long projectId, Long userId)
    {
        JDBC.update("insert into pm_project_member (project_id, user_id, is_project_admin, create_time, update_time) "
            + "values (?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", projectId, userId);
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
            dataSource.setURL("jdbc:h2:mem:pm0012;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
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
        TestRequirementMapper requirementMapper(JdbcTemplate jdbcTemplate)
        {
            return new TestRequirementMapper(jdbcTemplate);
        }

        @Bean
        TestLogMapper projectOperationLogMapper()
        {
            return new TestLogMapper();
        }

        @Bean
        IRequirementService requirementService(ProjectMapper projectMapper, RequirementMapper requirementMapper,
            ProjectOperationLogMapper projectOperationLogMapper)
        {
            return new RequirementServiceImpl(projectMapper, requirementMapper, projectOperationLogMapper);
        }

        @Bean
        Object initializeSchema(JdbcTemplate jdbcTemplate)
        {
            jdbcTemplate.execute("create table pm_project (project_id bigint primary key, project_name varchar(255), "
                + "creator_id bigint, status varchar(16), create_time timestamp, update_time timestamp)");
            jdbcTemplate.execute("create table pm_project_member (project_id bigint, user_id bigint, "
                + "is_project_admin integer, create_time timestamp, update_time timestamp, "
                + "primary key(project_id, user_id))");
            jdbcTemplate.execute("create table sys_user (user_id bigint primary key, user_name varchar(30), "
                + "nick_name varchar(30), email varchar(50))");
            jdbcTemplate.execute("create table pm_requirement (requirement_id bigint auto_increment primary key, "
                + "project_id bigint, creator_id bigint, current_version_id bigint, status varchar(100), "
                + "is_deleted integer, create_time timestamp, update_time timestamp)");
            jdbcTemplate.execute("create table pm_requirement_version (version_id bigint auto_increment primary key, "
                + "requirement_id bigint, version_no integer, title varchar(255), content clob, "
                + "attachment_snapshot clob, created_by bigint, create_time timestamp)");
            jdbcTemplate.execute("create table pm_requirement_owner (requirement_id bigint, user_id bigint, "
                + "primary key(requirement_id, user_id))");
            return new Object();
        }
    }

    static class TestRequirementMapper implements RequirementMapper
    {
        private final JdbcTemplate jdbc;
        private final Map<String, String> statuses = new HashMap<>();
        private final java.util.Set<String> inactiveStatuses = new java.util.HashSet<>();

        TestRequirementMapper(JdbcTemplate jdbc)
        {
            this.jdbc = jdbc;
        }

        void addStatus(String value, String label)
        {
            statuses.put(value, label);
            inactiveStatuses.remove(value);
        }

        void addInactiveStatus(String value, String label)
        {
            statuses.put(value, label);
            inactiveStatuses.add(value);
        }

        @Override
        public int updateRequirementStatus(Long projectId, Long requirementId, String status)
        {
            return jdbc.update("update pm_requirement set status = ?, update_time = CURRENT_TIMESTAMP "
                + "where project_id = ? and requirement_id = ? and is_deleted = 0", status, projectId, requirementId);
        }

        @Override
        public int insertRequirement(Requirement requirement)
        {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int rows = jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    "insert into pm_requirement (project_id, creator_id, current_version_id, status, is_deleted, "
                        + "create_time, update_time) values (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, requirement.getProjectId());
                statement.setLong(2, requirement.getCreatorId());
                statement.setObject(3, requirement.getCurrentVersionId());
                statement.setString(4, requirement.getStatus());
                statement.setInt(5, requirement.getIsDeleted());
                statement.setTimestamp(6, new java.sql.Timestamp(requirement.getCreateTime().getTime()));
                statement.setTimestamp(7, new java.sql.Timestamp(requirement.getUpdateTime().getTime()));
                return statement;
            }, keyHolder);
            requirement.setRequirementId(keyHolder.getKey().longValue());
            return rows;
        }

        @Override
        public int insertRequirementVersion(RequirementVersion version)
        {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int rows = jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    "insert into pm_requirement_version (requirement_id, version_no, title, content, "
                        + "attachment_snapshot, created_by, create_time) values (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, version.getRequirementId());
                statement.setInt(2, version.getVersionNo());
                statement.setString(3, version.getTitle());
                statement.setString(4, version.getContent());
                statement.setString(5, version.getAttachmentSnapshot());
                statement.setLong(6, version.getCreatedBy());
                statement.setTimestamp(7, new java.sql.Timestamp(version.getCreateTime().getTime()));
                return statement;
            }, keyHolder);
            version.setVersionId(keyHolder.getKey().longValue());
            return rows;
        }

        @Override
        public int updateCurrentVersion(Long requirementId, Long versionId)
        {
            return jdbc.update("update pm_requirement set current_version_id = ? where requirement_id = ?",
                versionId, requirementId);
        }

        @Override
        public int insertRequirementOwner(Long requirementId, Long userId)
        {
            return jdbc.update("insert into pm_requirement_owner (requirement_id, user_id) values (?, ?)",
                requirementId, userId);
        }

        @Override
        public Requirement selectRequirementForUser(Long projectId, Long requirementId, Long userId)
        {
            List<Requirement> requirements = jdbc.query("select r.*, v.version_no, v.title, v.content "
                + "from pm_requirement r inner join pm_requirement_version v on v.version_id = r.current_version_id "
                + "inner join pm_project_member m on m.project_id = r.project_id and m.user_id = ? "
                + "where r.project_id = ? and r.requirement_id = ? and r.is_deleted = 0",
                (rs, rowNum) -> requirement(rs), userId, projectId, requirementId);
            if (requirements.isEmpty()) return null;
            Requirement result = requirements.get(0);
            result.setOwners(selectRequirementOwners(requirementId));
            return result;
        }

        @Override
        public List<RequirementVersion> selectRequirementVersionsForUser(Long projectId, Long requirementId,
            Long userId)
        {
            return jdbc.query("select v.version_id, v.requirement_id, v.version_no, v.title, v.content, "
                + "v.attachment_snapshot, v.created_by, v.create_time "
                + "from pm_requirement_version v inner join pm_requirement r "
                + "on r.requirement_id = v.requirement_id "
                + "inner join pm_project_member m on m.project_id = r.project_id and m.user_id = ? "
                + "where r.project_id = ? and r.requirement_id = ? and r.is_deleted = 0 "
                + "order by v.version_no asc, v.version_id asc", (rs, rowNum) -> {
                    RequirementVersion version = new RequirementVersion();
                    version.setVersionId(rs.getLong("version_id"));
                    version.setRequirementId(rs.getLong("requirement_id"));
                    version.setVersionNo(rs.getInt("version_no"));
                    version.setTitle(rs.getString("title"));
                    version.setContent(rs.getString("content"));
                    version.setAttachmentSnapshot(rs.getString("attachment_snapshot"));
                    version.setCreatedBy(rs.getLong("created_by"));
                    version.setCreateTime(rs.getTimestamp("create_time"));
                    return version;
                }, userId, projectId, requirementId);
        }

        @Override
        public List<Requirement> selectRequirementsForUser(Long projectId, Long userId)
        {
            return jdbc.query("select distinct r.*, v.version_no, v.title, v.content "
                + "from pm_requirement r inner join pm_requirement_version v on v.version_id = r.current_version_id "
                + "inner join pm_project_member m on m.project_id = r.project_id and m.user_id = ? "
                + "where r.project_id = ? and r.is_deleted = 0 order by r.create_time desc, r.requirement_id desc",
                (rs, rowNum) -> requirement(rs), userId, projectId);
        }

        @Override
        public List<RequirementOwner> selectRequirementOwners(Long requirementId)
        {
            return jdbc.query("select o.requirement_id, o.user_id, u.user_name, u.nick_name, u.email "
                + "from pm_requirement_owner o inner join sys_user u on u.user_id = o.user_id "
                + "where o.requirement_id = ? order by o.user_id", (rs, rowNum) -> {
                    RequirementOwner owner = new RequirementOwner();
                    owner.setRequirementId(rs.getLong("requirement_id"));
                    owner.setUserId(rs.getLong("user_id"));
                    owner.setUserName(rs.getString("user_name"));
                    owner.setNickName(rs.getString("nick_name"));
                    owner.setEmail(rs.getString("email"));
                    return owner;
                }, requirementId);
        }

        @Override
        public List<RequirementOwner> selectProjectMembersByIds(Long projectId, List<Long> userIds)
        {
            if (userIds == null || userIds.isEmpty()) return List.of();
            String placeholders = String.join(",", userIds.stream().map(value -> "?").toList());
            List<Object> args = new ArrayList<>();
            args.add(projectId);
            args.addAll(userIds);
            return jdbc.query("select m.user_id, u.user_name, u.nick_name, u.email from pm_project_member m "
                + "inner join sys_user u on u.user_id = m.user_id where m.project_id = ? and m.user_id in ("
                + placeholders + ") order by m.user_id", (rs, rowNum) -> {
                    RequirementOwner owner = new RequirementOwner();
                    owner.setUserId(rs.getLong("user_id"));
                    owner.setUserName(rs.getString("user_name"));
                    owner.setNickName(rs.getString("nick_name"));
                    owner.setEmail(rs.getString("email"));
                    return owner;
                }, args.toArray());
        }

        @Override
        public SysDictData selectActiveRequirementStatus(String status)
        {
            if (!statuses.containsKey(status) || inactiveStatuses.contains(status)) return null;
            SysDictData data = new SysDictData();
            data.setDictValue(status);
            data.setDictLabel(statuses.get(status));
            data.setDictType("pm_requirement_status");
            data.setStatus("0");
            return data;
        }

        @Override
        public List<SysDictData> selectActiveRequirementStatuses()
        {
            return statuses.entrySet().stream().map(entry -> selectActiveRequirementStatus(entry.getKey()))
                .filter(java.util.Objects::nonNull).toList();
        }

        private Requirement requirement(java.sql.ResultSet rs) throws java.sql.SQLException
        {
            Requirement requirement = new Requirement();
            requirement.setRequirementId(rs.getLong("requirement_id"));
            requirement.setProjectId(rs.getLong("project_id"));
            requirement.setCreatorId(rs.getLong("creator_id"));
            requirement.setCurrentVersionId((Long) rs.getObject("current_version_id"));
            requirement.setCurrentVersionNo(rs.getInt("version_no"));
            requirement.setTitle(rs.getString("title"));
            requirement.setContent(rs.getString("content"));
            requirement.setStatus(rs.getString("status"));
            requirement.setStatusLabel(statuses.get(requirement.getStatus()));
            requirement.setIsDeleted(rs.getInt("is_deleted"));
            requirement.setCreateTime(rs.getTimestamp("create_time"));
            requirement.setUpdateTime(rs.getTimestamp("update_time"));
            return requirement;
        }
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
