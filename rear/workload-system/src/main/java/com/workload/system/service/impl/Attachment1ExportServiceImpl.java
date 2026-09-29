package com.workload.system.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.workload.system.domain.dto.Attachment1BatchRequest;
import com.workload.system.domain.dto.Attachment1RowDTO;
import com.workload.system.domain.dto.Attachment1TeacherDTO;
import com.workload.system.mapper.BizExportMapper;
import com.workload.system.service.Attachment1ExportService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 附件1（表一）导出服务实现。
 * <p>
 * <b>Task 2（本任务）</b>只落地「一次批量查询 + 按 userId 分组」：{@link #prepareBatch}
 * 用两条批量 Mapper 查询取齐全部教师的任务行与教师级信息，按请求 ID 顺序构造
 * {@link LinkedHashMap}，每位教师恰好归入自己的数据组，绝不回落到单人查询。
 * <p>
 * <b>Task 3 待实现</b>：{@link #writeSingle}（接管现有 GET）与 {@link #writeBatch}
 * （多 Sheet 工作簿 / 逐 entry ZIP、阈值、失败清单、文件名清洗）暂以占位实现抛出
 * {@link UnsupportedOperationException}，待 Task 3 补齐模板渲染与流式写出。
 *
 * @author wflg
 */
@Service
public class Attachment1ExportServiceImpl implements Attachment1ExportService
{
    private final BizExportMapper exportMapper;

    public Attachment1ExportServiceImpl(BizExportMapper exportMapper)
    {
        this.exportMapper = exportMapper;
    }

    /**
     * 批量准备：一次批量查行 + 一次批量查教师，按 userId 分组并以请求 ID 顺序归位。
     * <p>
     * 分组不依赖 SQL 排序，而是按传入 {@code userIds} 顺序构造 {@link LinkedHashMap}，
     * 保证输出容器内教师次序与请求一致；无任务行的教师得到空列表，无汇总的教师
     * {@link TeacherExportData#teacher()} 为 {@code null}（是否可导出的判定留待 Task 3）。
     *
     * @param semester 学年学期
     * @param userIds  去重、保序后的教师用户 ID 列表
     * @return 按请求 ID 顺序的教师导出数据组
     */
    public LinkedHashMap<Long, TeacherExportData> prepareBatch(String semester, List<Long> userIds)
    {
        List<Attachment1RowDTO> allRows = exportMapper.selectAttachment1RowsBatch(userIds, semester);
        List<Attachment1TeacherDTO> allTeachers = exportMapper.selectAttachment1TeachersBatch(userIds, semester);

        Map<Long, List<Attachment1RowDTO>> rowsByUser = allRows.stream()
                .collect(Collectors.groupingBy(Attachment1RowDTO::getUserId));
        Map<Long, Attachment1TeacherDTO> teacherByUser = allTeachers.stream()
                .collect(Collectors.toMap(Attachment1TeacherDTO::getUserId, t -> t, (a, b) -> a));

        LinkedHashMap<Long, TeacherExportData> prepared = new LinkedHashMap<>();
        for (Long userId : userIds)
        {
            prepared.put(userId, new TeacherExportData(userId,
                    teacherByUser.get(userId),
                    rowsByUser.getOrDefault(userId, List.of())));
        }
        return prepared;
    }

    @Override
    public void writeSingle(Long userId, String semester, HttpServletResponse response) throws Exception
    {
        // TODO(Task 3): 接管现有 GET 单人导出（无明细文本、无汇总抛「先核算汇总」、否则写 xlsx）。
        throw new UnsupportedOperationException("writeSingle 由 Task 3 实现");
    }

    @Override
    public void writeBatch(String semester, List<Long> userIds,
                           Attachment1BatchRequest.Format format,
                           HttpServletResponse response) throws Exception
    {
        // TODO(Task 3): 基于 prepareBatch 结果写多 Sheet 工作簿 / 逐 entry ZIP + 阈值 + 失败清单。
        throw new UnsupportedOperationException("writeBatch 由 Task 3 实现");
    }

    /**
     * 单教师批量导出数据组：定位字段 userId + 教师级信息 + 该教师任务行。
     *
     * @param userId  教师用户 ID
     * @param teacher 教师级信息（无汇总时为 null）
     * @param rows    该教师任务行（无任务时为空列表）
     */
    public record TeacherExportData(Long userId, Attachment1TeacherDTO teacher, List<Attachment1RowDTO> rows)
    {
    }
}
