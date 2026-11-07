package com.workload.system.review;

import com.workload.common.core.domain.entity.SysRole;
import com.workload.common.core.domain.entity.SysUser;
import com.workload.common.core.domain.model.LoginUser;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.mapper.BizAuditLogMapper;
import com.workload.system.mapper.BizOfflineAdjustmentNoteMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewQueryDataScopeTest {
    private final ReviewQueryService queries = new ReviewQueryService();
    private final BizWorkloadSummaryMapper summaries = mock(BizWorkloadSummaryMapper.class);
    private final BizAuditLogMapper logs = mock(BizAuditLogMapper.class);
    private final BizOfflineAdjustmentNoteMapper notes = mock(BizOfflineAdjustmentNoteMapper.class);
    private final ReviewCommands.Page page = new ReviewCommands.Page(1, 10);

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(queries, "summaries", summaries);
        ReflectionTestUtils.setField(queries, "logs", logs);
        ReflectionTestUtils.setField(queries, "notes", notes);
        var summary = new BizWorkloadSummary();
        summary.setId(101L);
        summary.setUserId(1003L);
        summary.setStatus(0);
        when(summaries.selectBizWorkloadSummaryById(101L)).thenReturn(summary);
        when(logs.selectPage(101L, 0, 10)).thenReturn(List.of());
        when(notes.selectPage(101L, 0, 10)).thenReturn(List.of());
    }

    @AfterEach void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"detail", "reviews", "logs", "notes"})
    void teacherReadingOthersSummaryReceivesForbiddenCode(String query) {
        authenticate(2003L, "teacher");
        assertThatThrownBy(() -> read(query)).isInstanceOf(ServiceException.class)
                .hasMessage("无权访问他人数据").extracting("code").isEqualTo(403);
        verifyNoInteractions(logs, notes);
    }

    @ParameterizedTest
    @CsvSource({"1003,teacher", "1,admin", "1004,biz_admin"})
    void ownTeacherAndAdministratorsKeepAllReadAccess(long userId, String role) {
        authenticate(userId, role);
        for (String query : List.of("detail", "reviews", "logs", "notes")) {
            assertThatCode(() -> read(query)).doesNotThrowAnyException();
        }
    }

    @Test void teacherCannotReadDataWithoutOwner() {
        authenticate(1003L, "teacher");
        assertThatThrownBy(() -> DataScopeUtil.assertOwnOrAdmin(null))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(403);
    }

    @Test void absentSummaryKeepsNotFoundCode() {
        authenticate(1003L, "teacher");
        assertThatThrownBy(() -> queries.requireSummary(999L))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(404);
    }

    private void read(String query) {
        switch (query) {
            case "detail" -> queries.detail(101L);
            case "reviews" -> queries.reviews(101L, null, page);
            case "logs" -> queries.logs(101L, page);
            case "notes" -> queries.notes(101L, page);
            default -> throw new IllegalArgumentException(query);
        }
    }

    private void authenticate(long userId, String roleKey) {
        var role = new SysRole();
        role.setRoleKey(roleKey);
        var user = new SysUser();
        user.setUserId(userId);
        user.setUserName("test-user");
        user.setRoles(List.of(role));
        var login = new LoginUser(userId, 1L, user, Set.of());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
        SecurityContextHolder.setContext(context);
    }
}
