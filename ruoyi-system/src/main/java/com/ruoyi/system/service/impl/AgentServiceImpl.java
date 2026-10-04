package com.ruoyi.system.service.impl;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.domain.AgentCall;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectOperationLog;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.system.domain.RequirementAttachment;
import com.ruoyi.system.mapper.AgentCallMapper;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectOperationLogMapper;
import com.ruoyi.system.service.IAgentService;
import com.ruoyi.system.service.IRequirementService;
import com.ruoyi.system.service.ISysConfigService;

@Service
public class AgentServiceImpl implements IAgentService
{
    private final ProjectMapper projectMapper;
    private final IRequirementService requirementService;
    private final AgentCallMapper agentCallMapper;
    private final ProjectOperationLogMapper projectOperationLogMapper;
    private final ISysConfigService configService;

    public AgentServiceImpl(ProjectMapper projectMapper, IRequirementService requirementService,
        AgentCallMapper agentCallMapper, ProjectOperationLogMapper projectOperationLogMapper,
        ISysConfigService configService)
    {
        this.projectMapper = projectMapper;
        this.requirementService = requirementService;
        this.agentCallMapper = agentCallMapper;
        this.projectOperationLogMapper = projectOperationLogMapper;
        this.configService = configService;
    }

    @Override
    public Requirement preview(Long projectId, Long requirementId, Long userId)
    {
        Requirement requirement = requirementService.selectRequirementForUser(projectId, requirementId, userId);
        if (requirement == null) throw new ServiceException("需求不存在或无权访问", HttpStatus.NOT_FOUND);
        if (Integer.valueOf(1).equals(requirement.getIsDeleted()))
            throw new ServiceException("已删除需求不能调用 Agent", HttpStatus.BAD_REQUEST);
        return requirement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentCall call(Long projectId, Long requirementId, Long userId, List<Long> attachmentIds,
        String idempotencyKey, boolean confirmed)
    {
        Project project = projectId == null || userId == null ? null : projectMapper.selectProjectForUser(projectId, userId);
        if (project == null) throw new ServiceException("项目不存在或无权访问", HttpStatus.NOT_FOUND);
        if (Project.STATUS_ARCHIVED.equals(project.getStatus()))
            throw new ServiceException("归档项目不能开展新的业务操作", HttpStatus.BAD_REQUEST);
        if (!confirmed) throw new ServiceException("发送 Agent 前必须确认拟发送内容", HttpStatus.BAD_REQUEST);
        String key = idempotencyKey == null ? "" : idempotencyKey.trim();
        if (key.isEmpty() || key.length() > 128) throw new ServiceException("幂等键不能为空且不能超过128个字符", HttpStatus.BAD_REQUEST);
        Requirement requirement = preview(projectId, requirementId, userId);
        AgentCall existing = agentCallMapper.selectByIdempotency(projectId, userId, key);
        if (existing != null)
        {
            if (!requirementId.equals(existing.getRequirementId()))
            {
                throw new ServiceException("幂等键已经用于其他需求", HttpStatus.CONFLICT);
            }
            return existing;
        }
        List<RequirementAttachment> available = requirement.getAttachments() == null ? List.of() : requirement.getAttachments();
        Set<Long> allowed = available.stream().map(RequirementAttachment::getAttachmentId).collect(java.util.stream.Collectors.toSet());
        Set<Long> selected = new LinkedHashSet<>(attachmentIds == null ? List.of() : attachmentIds);
        if (!allowed.containsAll(selected)) throw new ServiceException("所选附件不属于需求当前版本", HttpStatus.BAD_REQUEST);
        String provider = value("project.agent.provider", "local");
        String model = value("project.agent.model", "local-draft-v1");
        boolean external = Boolean.parseBoolean(value("project.agent.external.enabled", "false"));
        Date now = new Date();
        AgentCall call = new AgentCall();
        call.setProjectId(projectId); call.setRequirementId(requirementId); call.setRequirementVersionId(requirement.getCurrentVersionId());
        call.setInitiatorId(userId); call.setProvider(provider); call.setModel(model); call.setExternalEnabled(external ? 1 : 0);
        call.setStatus("SUCCESS"); call.setIdempotencyKey(key); call.setSelectedAttachmentSnapshot(JSON.toJSONString(selected));
        call.setInputContent(requirement.getContent());
        call.setParsedAttachmentContent(parseAttachmentContent(selected.stream()
            .map(id -> available.stream().filter(item -> id.equals(item.getAttachmentId())).findFirst().orElse(null))
            .filter(java.util.Objects::nonNull).toList()));
        call.setDraftTasks(JSON.toJSONString(List.of(java.util.Map.of("title", "拆分：" + requirement.getTitle(),
            "description", requirement.getContent(), "categoryValues", List.of()))));
        call.setCreateTime(now); call.setUpdateTime(now);
        if (agentCallMapper.insertAgentCall(call) != 1)
            throw new ServiceException("保存 Agent 调用记录失败", HttpStatus.ERROR);
        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId); log.setOperatorId(userId); log.setOperationType("AGENT_CALL");
        log.setDetail("调用 Agent 生成任务草稿，需求版本 v" + requirement.getCurrentVersionNo()); log.setCreateTime(now);
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
            throw new ServiceException("记录项目操作日志失败", HttpStatus.ERROR);
        return call;
    }

    @Override
    public List<AgentCall> listCalls(Long projectId, Long requirementId, Long userId)
    {
        if (projectId == null || requirementId == null || userId == null
            || projectMapper.selectProjectForUser(projectId, userId) == null
            || requirementService.selectRequirementForUser(projectId, requirementId, userId) == null) return null;
        return agentCallMapper.selectAgentCallsForUser(projectId, requirementId, userId);
    }

    private String value(String key, String fallback)
    {
        String value = configService.selectConfigByKey(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String parseAttachmentContent(List<RequirementAttachment> attachments)
    {
        boolean enabled = Boolean.parseBoolean(value("project.agent.file.parse.enabled", "true"));
        int configuredMaxChars;
        try { configuredMaxChars = Math.max(1000, Integer.parseInt(value("project.agent.file.parse.max-chars", "20000"))); }
        catch (NumberFormatException e) { configuredMaxChars = 20000; }
        final int maxChars = configuredMaxChars;
        List<Map<String, Object>> parsed = attachments.stream().map(attachment -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("attachmentId", attachment.getAttachmentId());
            item.put("originalName", attachment.getOriginalName());
            item.put("extension", attachment.getExtension());
            item.put("fileSize", attachment.getFileSize());
            if (!enabled)
            {
                item.put("status", "DISABLED");
                return item;
            }
            try
            {
                Path path = Paths.get(RuoYiConfig.getProfile(), FileUtils.stripPrefix(attachment.getStoragePath()));
                item.put("content", truncate(parseFile(path, attachment.getExtension()), maxChars));
                item.put("status", "PARSED");
            }
            catch (Exception e)
            {
                item.put("status", "UNPARSED");
                item.put("error", e.getMessage() == null ? "文件解析失败" : e.getMessage());
            }
            return item;
        }).collect(Collectors.toList());
        return JSON.toJSONString(parsed);
    }

    private String parseFile(Path path, String extension) throws Exception
    {
        if (!Files.isRegularFile(path)) throw new IllegalStateException("附件文件不存在");
        String ext = extension == null ? "" : extension.toLowerCase();
        if ("docx".equals(ext))
        {
            try (InputStream input = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(input))
            {
                return document.getParagraphs().stream().map(item -> item.getText())
                    .filter(item -> item != null && !item.isBlank()).collect(Collectors.joining("\n"));
            }
        }
        if ("xls".equals(ext) || "xlsx".equals(ext))
        {
            try (InputStream input = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(input))
            {
                DataFormatter formatter = new DataFormatter();
                StringBuilder output = new StringBuilder();
                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++)
                {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);
                    output.append('[').append(sheet.getSheetName()).append("]\n");
                    for (Row row : sheet)
                    {
                        boolean hasValue = false;
                        for (Cell cell : row)
                        {
                            String value = formatter.formatCellValue(cell);
                            if (!value.isBlank()) hasValue = true;
                            output.append(value).append('\t');
                        }
                        if (hasValue) output.append('\n');
                    }
                }
                return output.toString().trim();
            }
        }
        return "已选择附件：" + (path.getFileName() == null ? "" : path.getFileName());
    }

    private String truncate(String value, int maxChars)
    {
        if (value == null) return "";
        return value.length() <= maxChars ? value : value.substring(0, maxChars) + "…";
    }
}
