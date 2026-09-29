package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.system.domain.dto.Attachment1RowDTO;
import com.workload.system.domain.dto.Attachment1TeacherDTO;
import com.workload.system.mapper.BizExportMapper;

/**
 * 批量附件1导出服务单元测试。
 * <p>
 * 本任务（Task 2）只锁定「一次批量查询 + 按 userId 分组」这一层：
 * 批量准备必须恰好各触发一次批量行/教师查询，绝不回落到单人查询，
 * 且请求 ID 顺序与分组归属都正确。XLSX/ZIP 写出与失败清单属 Task 3。
 */
@ExtendWith(MockitoExtension.class)
class Attachment1ExportServiceImplTest
{
    private static final String SEMESTER = "2026-2027-1";

    @Mock
    private BizExportMapper mapper;

    @InjectMocks
    private Attachment1ExportServiceImpl service;

    @Test
    void batchLoadsRowsAndTeachersInTwoQueries() throws Exception
    {
        when(mapper.selectAttachment1RowsBatch(List.of(2L, 1L), SEMESTER))
                .thenReturn(List.of(row(1L, "课程A"), row(2L, "课程B")));
        when(mapper.selectAttachment1TeachersBatch(List.of(2L, 1L), SEMESTER))
                .thenReturn(List.of(teacher(1L, "甲"), teacher(2L, "乙")));

        LinkedHashMap<Long, Attachment1ExportServiceImpl.TeacherExportData> prepared =
                service.prepareBatch(SEMESTER, List.of(2L, 1L));

        // 恰好两条批量查询，绝不回落到单人查询（否则退化为 N×2）
        verify(mapper, times(1)).selectAttachment1RowsBatch(anyList(), eq(SEMESTER));
        verify(mapper, times(1)).selectAttachment1TeachersBatch(anyList(), eq(SEMESTER));
        verify(mapper, never()).selectAttachment1Rows(anyLong(), anyString());

        // 按请求 ID 顺序构造，每位教师恰好归入自己的数据组
        assertThat(prepared.keySet()).containsExactly(2L, 1L);
        assertThat(prepared.get(1L).teacher().getTeacherName()).isEqualTo("甲");
        assertThat(prepared.get(1L).rows()).extracting(Attachment1RowDTO::getCourseName)
                .containsExactly("课程A");
        assertThat(prepared.get(2L).teacher().getTeacherName()).isEqualTo("乙");
        assertThat(prepared.get(2L).rows()).extracting(Attachment1RowDTO::getCourseName)
                .containsExactly("课程B");
    }

    private static Attachment1RowDTO row(Long userId, String courseName)
    {
        Attachment1RowDTO row = new Attachment1RowDTO();
        row.setUserId(userId);
        row.setCourseName(courseName);
        return row;
    }

    private static Attachment1TeacherDTO teacher(Long userId, String name)
    {
        Attachment1TeacherDTO teacher = new Attachment1TeacherDTO();
        teacher.setUserId(userId);
        teacher.setTeacherName(name);
        return teacher;
    }
}
