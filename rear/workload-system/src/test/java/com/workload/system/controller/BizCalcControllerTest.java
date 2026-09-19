package com.workload.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.core.domain.AjaxResult;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.calc.WorkloadCalcService;
import com.workload.system.domain.dto.CalculationRunRequest;
import com.workload.system.domain.vo.CalculationRunResult;

@ExtendWith(MockitoExtension.class)
class BizCalcControllerTest
{
    private static final Long USER = 1L;
    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks private BizCalcController controller;
    @Mock private WorkloadCalcService workloadCalcService;

    @Test
    void runDelegatesToServiceWithResolvedUser()
    {
        CalculationRunResult result = new CalculationRunResult();
        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            dataScope.when(() -> DataScopeUtil.resolveUserId(USER)).thenReturn(USER);
            when(workloadCalcService.run(org.mockito.ArgumentMatchers.argThat(
                    r -> USER.equals(r.getUserId()) && SEMESTER.equals(r.getSemester()) && r.isIncludeG11())))
                    .thenReturn(result);

            CalculationRunRequest request = new CalculationRunRequest(USER, SEMESTER, true);
            AjaxResult ajax = controller.run(request);

            assertThat(ajax.get(AjaxResult.CODE_TAG)).isEqualTo(200);
            assertThat(ajax.get(AjaxResult.DATA_TAG)).isSameAs(result);
        }
    }

    @Test
    void runBatchDelegatesUserIdsAndSemester()
    {
        List<Long> userIds = Arrays.asList(USER, 2L);
        java.util.Map<String, Object> batch = new java.util.HashMap<>();
        batch.put("successCount", 2);
        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            // 管理角色：resolveUserId(null) 返回 null，保持全量/指定列表语义
            dataScope.when(() -> DataScopeUtil.resolveUserId(null)).thenReturn(null);
            when(workloadCalcService.runBatch(userIds, SEMESTER, true)).thenReturn(batch);

            BizCalcController.RunBatchBody body = new BizCalcController.RunBatchBody();
            body.setUserIds(userIds);
            body.setIncludeG11(true);
            AjaxResult ajax = controller.runBatch(SEMESTER, body);

            assertThat(ajax.get(AjaxResult.CODE_TAG)).isEqualTo(200);
            verify(workloadCalcService).runBatch(userIds, SEMESTER, true);
        }
    }

    @Test
    void runBatchTeacherIsConfinedToSelf()
    {
        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            // 教师角色：resolveUserId(null) 收敛为本人，无论 body 传谁
            dataScope.when(() -> DataScopeUtil.resolveUserId(null)).thenReturn(USER);
            when(workloadCalcService.runBatch(eq(java.util.Collections.singletonList(USER)), eq(SEMESTER),
                    eq(false))).thenReturn(new java.util.HashMap<>());

            BizCalcController.RunBatchBody body = new BizCalcController.RunBatchBody();
            body.setUserIds(Arrays.asList(2L, 3L));
            body.setIncludeG11(false);
            controller.runBatch(SEMESTER, body);

            verify(workloadCalcService).runBatch(java.util.Collections.singletonList(USER), SEMESTER, false);
        }
    }
}
