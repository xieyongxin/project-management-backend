package com.ruoyi.system.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.domain.ProjectOperationLog;
import com.ruoyi.common.core.domain.entity.SysRole;

class ProjectMapperXmlTest
{
    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception
    {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:pm0002-mapper;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        h2.setUser("sa");
        dataSource = h2;
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("drop all objects");
        jdbc.execute("create table pm_project ("
            + "project_id bigint auto_increment primary key, project_name varchar(255) not null, "
            + "project_name_key varchar(255) not null unique, creator_id bigint not null, "
            + "status varchar(16) not null default 'ACTIVE', create_time timestamp not null, update_time timestamp not null)");
        jdbc.execute("create table pm_project_member ("
            + "project_id bigint not null, user_id bigint not null, role_id bigint null, is_project_admin integer not null, "
            + "create_time timestamp not null, update_time timestamp not null, primary key(project_id, user_id))");
        jdbc.execute("create table sys_user (user_id bigint primary key, user_name varchar(30), nick_name varchar(30), email varchar(50))");
        jdbc.execute("create table sys_role (role_id bigint primary key, role_name varchar(30), role_key varchar(100), "
            + "role_sort integer, status varchar(1), del_flag varchar(1))");
        jdbc.execute("create table pm_project_operation_log ("
            + "log_id bigint auto_increment primary key, project_id bigint not null, operator_id bigint not null, "
            + "target_user_id bigint, previous_role_id bigint, new_role_id bigint, operation_type varchar(64) not null, "
            + "detail varchar(1000), create_time timestamp not null)");

        Configuration configuration = new Configuration(
            new Environment("pm0002-test", new JdbcTransactionFactory(), dataSource));
        configuration.getTypeAliasRegistry().registerAlias("Project", Project.class);
        configuration.getTypeAliasRegistry().registerAlias("ProjectMember", ProjectMember.class);
        configuration.getTypeAliasRegistry().registerAlias("SysRole", SysRole.class);
        configuration.getTypeAliasRegistry().registerAlias("ProjectOperationLog", ProjectOperationLog.class);
        parseMapper(configuration, "mapper/system/ProjectMapper.xml");
        parseMapper(configuration, "mapper/system/ProjectMemberMapper.xml");
        parseMapper(configuration, "mapper/system/ProjectOperationLogMapper.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Test
    void mapperQueriesEnforceMembershipForListAndDetail()
    {
        Date now = new Date();
        try (SqlSession session = sqlSessionFactory.openSession(false))
        {
            ProjectMapper projectMapper = session.getMapper(ProjectMapper.class);
            ProjectMemberMapper memberMapper = session.getMapper(ProjectMemberMapper.class);
            Project first = insertProject(projectMapper, "Alpha project", "alpha project", 21L, now);
            Project second = insertProject(projectMapper, "Beta project", "beta project", 22L, now);
            memberMapper.insertProjectMember(member(first, 21L, now));
            memberMapper.insertProjectMember(member(second, 21L, now));
            jdbc.update("insert into sys_user (user_id, user_name, nick_name, email) values (21, 'alice', 'Alice', 'alice@example.com')");
            jdbc.update("insert into sys_user (user_id, user_name, nick_name, email) values (22, 'bob', 'Bob', 'bob@example.com')");
            jdbc.update("insert into sys_role (role_id, role_name, role_key, role_sort, status, del_flag) "
                + "values (7, '项目成员', 'project_member', 1, '0', '0')");
            ProjectMember secondMember = member(second, 22L, now);
            secondMember.setRoleId(7L);
            memberMapper.insertProjectMember(secondMember);
            session.commit();

            assertEquals(List.of(first.getProjectId()), projectMapper.selectProjectListForUser(21L, "Alpha")
                .stream().map(Project::getProjectId).toList());
            assertEquals(List.of(second.getProjectId(), first.getProjectId()),
                projectMapper.selectProjectListForUser(21L, null).stream().map(Project::getProjectId).toList());
            assertEquals(0, projectMapper.selectProjectListForUser(1L, null).size());
            assertNull(projectMapper.selectProjectForUser(first.getProjectId(), 22L));
            assertNull(projectMapper.selectProjectForUser(first.getProjectId(), 1L));
            assertEquals(first.getProjectId(), projectMapper.selectProjectForUser(first.getProjectId(), 21L)
                .getProjectId());
            assertEquals(Project.STATUS_ACTIVE, projectMapper.selectProjectForUser(first.getProjectId(), 21L).getStatus());
            List<ProjectMember> members = memberMapper.selectProjectMembersForUser(second.getProjectId(), 21L);
            assertEquals(2, members.size());
            assertEquals("bob", members.get(1).getUserName());
            assertEquals("项目成员", members.get(1).getRoleName());
            assertEquals(0, memberMapper.selectProjectMembersForUser(second.getProjectId(), 1L).size());
            assertEquals("bob", memberMapper.selectProjectMember(second.getProjectId(), 22L).getUserName());
            assertEquals("项目成员", memberMapper.selectActiveProjectRole(7L).getRoleName());
            assertEquals(1, memberMapper.selectActiveProjectRoles().size());
            assertEquals(1, memberMapper.updateProjectMemberRole(second.getProjectId(), 21L, 7L));
            assertEquals(second.getProjectId(), projectMapper.lockProjectForUpdate(second.getProjectId()));
            assertEquals(2, memberMapper.countProjectAdmins(second.getProjectId()));
            assertEquals(1, memberMapper.updateProjectMemberAdmin(second.getProjectId(), 22L, 1));
            assertEquals(1, projectMapper.updateProjectName(second.getProjectId(), "Renamed project", "renamed project"));
            assertEquals("Renamed project", projectMapper.selectProjectForUser(second.getProjectId(), 21L).getProjectName());
            assertEquals(1, projectMapper.updateProjectStatus(second.getProjectId(), Project.STATUS_ARCHIVED));
            assertEquals(Project.STATUS_ARCHIVED, projectMapper.selectProjectForUser(second.getProjectId(), 21L).getStatus());
            assertEquals(1, projectMapper.updateProjectStatus(second.getProjectId(), Project.STATUS_ACTIVE));
            assertEquals(Project.STATUS_ACTIVE, projectMapper.selectProjectForUser(second.getProjectId(), 21L).getStatus());

            ProjectOperationLog log = new ProjectOperationLog();
            log.setProjectId(second.getProjectId());
            log.setOperatorId(21L);
            log.setTargetUserId(22L);
            log.setPreviousRoleId(null);
            log.setNewRoleId(7L);
            log.setOperationType("MEMBER_ROLE_UPDATE");
            log.setDetail("更新项目成员全局角色");
            log.setCreateTime(now);
            assertEquals(1, session.getMapper(ProjectOperationLogMapper.class).insertProjectOperationLog(log));
            List<ProjectOperationLog> logs = session.getMapper(ProjectOperationLogMapper.class)
                .selectProjectOperationLogsForUser(second.getProjectId(), 21L);
            assertEquals(1, logs.size());
            assertEquals("alice", logs.get(0).getOperatorName());
            assertEquals("bob", logs.get(0).getTargetUserName());
            assertEquals(0, session.getMapper(ProjectOperationLogMapper.class)
                .selectProjectOperationLogsForUser(second.getProjectId(), 1L).size());
            assertEquals(1, memberMapper.deleteProjectMember(second.getProjectId(), 22L));
            assertNull(memberMapper.selectProjectMember(second.getProjectId(), 22L));
        }
    }

    private void parseMapper(Configuration configuration, String resource) throws Exception
    {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource))
        {
            if (input == null)
            {
                throw new IllegalStateException("Missing mapper resource: " + resource);
            }
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private Project insertProject(ProjectMapper mapper, String name, String key, Long creatorId, Date now)
    {
        Project project = new Project();
        project.setProjectName(name);
        project.setProjectNameKey(key);
        project.setCreatorId(creatorId);
        project.setCreateTime(now);
        project.setUpdateTime(now);
        mapper.insertProject(project);
        return project;
    }

    private ProjectMember member(Project project, Long userId, Date now)
    {
        ProjectMember member = new ProjectMember();
        member.setProjectId(project.getProjectId());
        member.setUserId(userId);
        member.setIsProjectAdmin(1);
        member.setCreateTime(now);
        member.setUpdateTime(now);
        return member;
    }
}
