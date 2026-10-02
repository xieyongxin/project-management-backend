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
            + "create_time timestamp not null, update_time timestamp not null)");
        jdbc.execute("create table pm_project_member ("
            + "project_id bigint not null, user_id bigint not null, is_project_admin integer not null, "
            + "create_time timestamp not null, update_time timestamp not null, primary key(project_id, user_id))");

        Configuration configuration = new Configuration(
            new Environment("pm0002-test", new JdbcTransactionFactory(), dataSource));
        configuration.getTypeAliasRegistry().registerAlias("Project", Project.class);
        configuration.getTypeAliasRegistry().registerAlias("ProjectMember", ProjectMember.class);
        parseMapper(configuration, "mapper/system/ProjectMapper.xml");
        parseMapper(configuration, "mapper/system/ProjectMemberMapper.xml");
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
            memberMapper.insertProjectMember(member(second, 22L, now));
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
