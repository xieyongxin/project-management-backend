package com.ruoyi.system.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import com.ruoyi.system.domain.Task;
import com.ruoyi.system.domain.TaskCategory;
import com.ruoyi.system.domain.TaskOwner;
import com.ruoyi.system.domain.TaskVersion;

class TaskMapperXmlTest
{
    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception
    {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:pm0013-mapper;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        h2.setUser("sa");
        dataSource = h2;
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("drop all objects");
        jdbc.execute("create table pm_project_member (project_id bigint, user_id bigint, primary key(project_id, user_id))");
        jdbc.execute("create table sys_user (user_id bigint primary key, user_name varchar(30), nick_name varchar(30), email varchar(50))");
        jdbc.execute("create table sys_dict_data (dict_code bigint primary key, dict_sort bigint, dict_label varchar(100), "
            + "dict_value varchar(100), dict_type varchar(100), css_class varchar(100), list_class varchar(100), "
            + "is_default varchar(1), status varchar(1), create_by varchar(64), create_time timestamp, update_by varchar(64), "
            + "update_time timestamp, remark varchar(500))");
        jdbc.execute("create table pm_requirement_version (version_id bigint primary key, requirement_id bigint, version_no integer)");
        jdbc.execute("create table pm_requirement (requirement_id bigint primary key, project_id bigint, current_version_id bigint, is_deleted integer)");
        jdbc.execute("create table pm_task (task_id bigint auto_increment primary key, project_id bigint, requirement_id bigint, "
            + "creator_id bigint, current_version_id bigint, status varchar(100), is_deleted integer, create_time timestamp, update_time timestamp)");
        jdbc.execute("create table pm_task_version (version_id bigint auto_increment primary key, task_id bigint, version_no integer, "
            + "title varchar(255), description clob, requirement_version_id bigint, created_by bigint, create_time timestamp)");
        jdbc.execute("create table pm_task_category (task_id bigint, category_value varchar(100), primary key(task_id, category_value))");
        jdbc.execute("create table pm_task_owner (task_id bigint, user_id bigint, primary key(task_id, user_id))");

        Configuration configuration = new Configuration(
            new Environment("pm0013-test", new JdbcTransactionFactory(), dataSource));
        configuration.getTypeAliasRegistry().registerAlias("Task", Task.class);
        configuration.getTypeAliasRegistry().registerAlias("TaskVersion", TaskVersion.class);
        configuration.getTypeAliasRegistry().registerAlias("TaskCategory", TaskCategory.class);
        configuration.getTypeAliasRegistry().registerAlias("TaskOwner", TaskOwner.class);
        parseMapper(configuration, "mapper/system/TaskMapper.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Test
    void mapperStoresTaskVersionCategoriesOwnersAndAppliesMemberIsolation()
        throws Exception
    {
        Date now = new Date();
        jdbc.update("insert into pm_project_member values (41, 21)");
        jdbc.update("insert into sys_user values (21, 'alice', 'Alice', 'alice@example.com')");
        jdbc.update("insert into sys_user values (23, 'carol', 'Carol', 'carol@example.com')");
        jdbc.update("insert into pm_project_member values (41, 23)");
        jdbc.update("insert into pm_requirement_version values (501, 7, 3)");
        jdbc.update("insert into pm_requirement values (7, 41, 501, 0)");
        jdbc.update("insert into sys_dict_data values (101, 1, '待处理', 'todo', 'pm_task_status', '', '', 'Y', '0', 'admin', ?, null, null, '')", now);
        jdbc.update("insert into sys_dict_data values (102, 1, '开发', 'dev', 'pm_task_category', '', '', 'Y', '0', 'admin', ?, null, null, '')", now);

        try (SqlSession session = sqlSessionFactory.openSession(false))
        {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            Task task = new Task();
            task.setProjectId(41L);
            task.setRequirementId(7L);
            task.setCreatorId(21L);
            task.setStatus("todo");
            task.setIsDeleted(0);
            task.setCreateTime(now);
            task.setUpdateTime(now);
            assertEquals(1, mapper.insertTask(task));

            TaskVersion version = new TaskVersion();
            version.setTaskId(task.getTaskId());
            version.setVersionNo(1);
            version.setTitle("登录");
            version.setDescription("实现登录");
            version.setRequirementVersionId(501L);
            version.setCreatedBy(21L);
            version.setCreateTime(now);
            assertEquals(1, mapper.insertTaskVersion(version));
            assertEquals(1, mapper.updateCurrentVersion(task.getTaskId(), version.getVersionId()));
            assertEquals(1, mapper.insertTaskCategory(task.getTaskId(), "dev"));
            assertEquals(1, mapper.insertTaskOwner(task.getTaskId(), 21L));
            assertEquals(1, mapper.insertTaskOwner(task.getTaskId(), 23L));
            session.commit();

            Task selected = mapper.selectTaskForUser(41L, task.getTaskId(), 21L);
            assertEquals("登录", selected.getTitle());
            assertEquals(1, selected.getCurrentVersionNo());
            assertEquals(3, selected.getRequirementVersionNo());
            assertEquals(0, selected.getRequirementVersionOutdated());
            List<TaskVersion> versions = mapper.selectTaskVersionsForUser(41L, task.getTaskId(), 21L);
            assertEquals(1, versions.size());
            assertEquals(1, versions.get(0).getVersionNo());
            assertEquals(3, versions.get(0).getRequirementVersionNo());
            assertEquals(1, mapper.selectTaskCategories(task.getTaskId()).size());
            assertEquals("dev", mapper.selectTaskCategories(task.getTaskId()).get(0).getCategoryValue());
            assertEquals(2, mapper.selectTaskOwners(task.getTaskId()).size());
            assertEquals(21L, mapper.selectTaskOwners(task.getTaskId()).get(0).getUserId());
            assertEquals(1, mapper.selectActiveTaskStatuses().size());
            assertEquals(1, mapper.selectActiveTaskCategories().size());
            assertEquals("todo", mapper.selectActiveTaskStatuses().get(0).getDictValue());
            assertEquals(0, mapper.selectTaskForUser(41L, task.getTaskId(), 99L) == null ? 0 : 1);
            assertEquals(0, mapper.selectTaskVersionsForUser(41L, task.getTaskId(), 99L).size());
            assertEquals(1, mapper.selectTasksForUser(41L, 21L).size());
            assertEquals(0, mapper.selectTasksForUser(41L, 99L).size());
            try (java.sql.PreparedStatement statement = session.getConnection().prepareStatement(
                "insert into pm_requirement_version (version_id, requirement_id, version_no) values (502, 7, 4)"))
            {
                statement.executeUpdate();
            }
            try (java.sql.PreparedStatement statement = session.getConnection().prepareStatement(
                "update pm_requirement set current_version_id = 502 where requirement_id = 7"))
            {
                statement.executeUpdate();
            }
            session.clearCache();
            assertEquals(1, mapper.selectTaskForUser(41L, task.getTaskId(), 21L).getRequirementVersionOutdated());
            assertEquals(1, mapper.updateTaskStatus(41L, task.getTaskId(), "done"));
            session.commit();
            assertEquals("done", jdbc.queryForObject("select status from pm_task where task_id = ?", String.class,
                task.getTaskId()));
            assertEquals(0, mapper.updateTaskStatus(42L, task.getTaskId(), "todo"));
            session.commit();
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
