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
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.system.domain.RequirementOwner;
import com.ruoyi.system.domain.RequirementVersion;

class RequirementMapperXmlTest
{
    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception
    {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:pm0012-mapper;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        h2.setUser("sa");
        dataSource = h2;
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("drop all objects");
        jdbc.execute("create table pm_project (project_id bigint primary key, project_name varchar(255), "
            + "creator_id bigint, status varchar(16), create_time timestamp, update_time timestamp)");
        jdbc.execute("create table pm_project_member (project_id bigint, user_id bigint, "
            + "is_project_admin integer, create_time timestamp, update_time timestamp, "
            + "primary key(project_id, user_id))");
        jdbc.execute("create table sys_user (user_id bigint primary key, user_name varchar(30), "
            + "nick_name varchar(30), email varchar(50))");
        jdbc.execute("create table sys_dict_data (dict_code bigint primary key, dict_sort bigint, "
            + "dict_label varchar(100), dict_value varchar(100), dict_type varchar(100), css_class varchar(100), "
            + "list_class varchar(100), is_default varchar(1), status varchar(1), create_by varchar(64), "
            + "create_time timestamp, update_by varchar(64), update_time timestamp, remark varchar(500))");
        jdbc.execute("create table pm_requirement (requirement_id bigint auto_increment primary key, project_id bigint, "
            + "creator_id bigint, current_version_id bigint, status varchar(100), is_deleted integer, "
            + "create_time timestamp, update_time timestamp)");
        jdbc.execute("create table pm_requirement_version (version_id bigint auto_increment primary key, "
            + "requirement_id bigint, version_no integer, title varchar(255), content clob, "
            + "attachment_snapshot clob, created_by bigint, create_time timestamp)");
        jdbc.execute("create table pm_requirement_owner (requirement_id bigint, user_id bigint, "
            + "primary key(requirement_id, user_id))");

        Configuration configuration = new Configuration(
            new Environment("pm0012-test", new JdbcTransactionFactory(), dataSource));
        configuration.getTypeAliasRegistry().registerAlias("Requirement", Requirement.class);
        configuration.getTypeAliasRegistry().registerAlias("RequirementVersion", RequirementVersion.class);
        configuration.getTypeAliasRegistry().registerAlias("RequirementOwner", RequirementOwner.class);
        parseMapper(configuration, "mapper/system/RequirementMapper.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Test
    void mapperStoresVersionsOwnersAndRestrictsQueriesToProjectMembers()
    {
        Date now = new Date();
        jdbc.update("insert into pm_project values (41, 'Project', 21, 'ACTIVE', ?, ?)", now, now);
        jdbc.update("insert into pm_project_member values (41, 21, 1, ?, ?)", now, now);
        jdbc.update("insert into sys_user values (21, 'alice', 'Alice', 'alice@example.com')");
        jdbc.update("insert into sys_user values (23, 'carol', 'Carol', 'carol@example.com')");
        jdbc.update("insert into pm_project_member values (41, 23, 0, ?, ?)", now, now);
        jdbc.update("insert into sys_dict_data values (101, 1, '待处理', 'todo', 'pm_requirement_status', '', '', 'Y', '0', 'admin', ?, null, null, '')", now);
        jdbc.update("insert into sys_dict_data values (102, 2, '停用', 'disabled', 'pm_requirement_status', '', '', 'N', '1', 'admin', ?, null, null, '')", now);

        try (SqlSession session = sqlSessionFactory.openSession(false))
        {
            RequirementMapper mapper = session.getMapper(RequirementMapper.class);
            Requirement requirement = new Requirement();
            requirement.setProjectId(41L);
            requirement.setCreatorId(21L);
            requirement.setStatus("todo");
            requirement.setIsDeleted(0);
            requirement.setCreateTime(now);
            requirement.setUpdateTime(now);
            assertEquals(1, mapper.insertRequirement(requirement));

            RequirementVersion version = new RequirementVersion();
            version.setRequirementId(requirement.getRequirementId());
            version.setVersionNo(1);
            version.setTitle("登录");
            version.setContent("正文");
            version.setAttachmentSnapshot("[]");
            version.setCreatedBy(21L);
            version.setCreateTime(now);
            assertEquals(1, mapper.insertRequirementVersion(version));
            assertEquals(1, mapper.updateCurrentVersion(requirement.getRequirementId(), version.getVersionId()));
            RequirementVersion second = new RequirementVersion();
            second.setRequirementId(requirement.getRequirementId());
            second.setVersionNo(2);
            second.setTitle("登录 2");
            second.setContent("正文 2");
            second.setAttachmentSnapshot("[]");
            second.setCreatedBy(21L);
            second.setCreateTime(now);
            assertEquals(1, mapper.insertRequirementVersion(second));
            assertEquals(1, mapper.insertRequirementOwner(requirement.getRequirementId(), 21L));
            assertEquals(1, mapper.insertRequirementOwner(requirement.getRequirementId(), 23L));
            session.commit();

            Requirement selected = mapper.selectRequirementForUser(41L, requirement.getRequirementId(), 21L);
            assertEquals("登录", selected.getTitle());
            assertEquals(1, selected.getCurrentVersionNo());
            assertEquals(List.of(1, 2), mapper.selectRequirementVersionsForUser(41L,
                requirement.getRequirementId(), 21L).stream().map(RequirementVersion::getVersionNo).toList());
            assertEquals(0, mapper.selectRequirementVersionsForUser(41L, requirement.getRequirementId(), 99L).size());
            List<RequirementOwner> owners = mapper.selectRequirementOwners(requirement.getRequirementId());
            assertEquals(2, owners.size());
            assertEquals(21L, owners.get(0).getUserId());
            assertEquals("alice", owners.get(0).getUserName());
            assertEquals("Alice", owners.get(0).getNickName());
            assertEquals("alice@example.com", owners.get(0).getEmail());
            assertEquals(2, mapper.selectProjectMembersByIds(41L, List.of(21L, 23L)).size());
            assertEquals(1, mapper.selectRequirementsForUser(41L, 21L).size());
            assertEquals(0, mapper.selectRequirementsForUser(41L, 99L).size());
            assertEquals("待处理", mapper.selectActiveRequirementStatus("todo").getDictLabel());
            assertNull(mapper.selectActiveRequirementStatus("disabled"));
            assertEquals(1, mapper.selectActiveRequirementStatuses().size());
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
}
