package com.workload.system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.workload.system.domain.dto.importing.TeacherRosterPreviewResponse;
import com.workload.system.service.IBizTeacherProfileService;
import com.workload.system.service.ITeacherProfileImportService;

/**
 * 教师档案 Controller 花名册预检端点测试。
 *
 * <p>只验证 {@code /importPreview} 把上传文件透传给服务的 {@code previewRoster}；
 * 本任务不提供 confirm 写入端点。</p>
 */
@ExtendWith(MockitoExtension.class)
class BizTeacherProfileControllerTest
{
    @Mock
    private IBizTeacherProfileService bizTeacherProfileService;

    @Mock
    private ITeacherProfileImportService teacherProfileImportService;

    @InjectMocks
    private BizTeacherProfileController controller;

    @Test
    void importPreviewDelegatesToRosterPreview() throws Exception
    {
        ReflectionTestUtils.setField(controller, "importMaxSizeMb", 10L);
        MockMultipartFile file = new MockMultipartFile("file", "roster.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        TeacherRosterPreviewResponse preview = new TeacherRosterPreviewResponse(
                1, 1, 0, 1, List.of(), Map.of(), Map.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());
        when(teacherProfileImportService.previewRoster(any(), eq("roster.xlsx"))).thenReturn(preview);

        controller.importPreview(file);

        verify(teacherProfileImportService).previewRoster(any(), eq("roster.xlsx"));
    }
}
