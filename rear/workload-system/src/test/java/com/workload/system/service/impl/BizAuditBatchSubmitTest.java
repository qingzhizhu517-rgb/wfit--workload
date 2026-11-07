package com.workload.system.service.impl;

import com.workload.common.exception.ServiceException;
import com.workload.system.service.BizAuditService;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.AopContext;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BizAuditBatchSubmitTest {
    private final BizAuditServiceImpl service = new BizAuditServiceImpl();
    private final BizAuditService proxy = mock(BizAuditService.class);

    static Stream<Arguments> invalidIds() {
        return Stream.<Long[]>of(null, new Long[0], ids(101), new Long[]{1L, 1L},
                new Long[]{1L, null}, new Long[]{1L, 0L}, new Long[]{1L, -1L})
                .map(ids -> Arguments.of((Object) ids));
    }

    @ParameterizedTest
    @MethodSource("invalidIds")
    void invalidBatchIsRejectedBeforeAnyProxyCallOrWrite(Long[] ids) {
        try (var aop = mockStatic(AopContext.class)) {
            aop.when(AopContext::currentProxy).thenReturn(proxy);
            assertThatThrownBy(() -> service.batchSubmit(ids)).isInstanceOf(ServiceException.class)
                    .extracting("code").isEqualTo(400);
            aop.verifyNoInteractions();
            verifyNoInteractions(proxy);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void validBoundaryBatchUsesProxyForEverySubmission(int size) {
        try (var aop = mockStatic(AopContext.class)) {
            aop.when(AopContext::currentProxy).thenReturn(proxy);
            var receipt = service.batchSubmit(ids(size));
            assertThat(receipt).containsEntry("successCount", size).containsEntry("failCount", 0)
                    .containsEntry("failDetails", List.of());
            for (long id = 1; id <= size; id++) verify(proxy).submit(id);
            verifyNoMoreInteractions(proxy);
        }
    }

    @Test void individualFailureKeepsSuccessfulItemsAndContinuesThroughProxy() {
        doThrow(new ServiceException("状态冲突", 409)).when(proxy).submit(2L);
        try (var aop = mockStatic(AopContext.class)) {
            aop.when(AopContext::currentProxy).thenReturn(proxy);
            var receipt = service.batchSubmit(ids(3));
            assertThat(receipt).containsEntry("successCount", 2).containsEntry("failCount", 1)
                    .containsEntry("failDetails", List.of(Map.of("id", 2L, "reason", "状态冲突")));
            var order = inOrder(proxy);
            order.verify(proxy).submit(1L);
            order.verify(proxy).submit(2L);
            order.verify(proxy).submit(3L);
            verifyNoMoreInteractions(proxy);
        }
    }

    private static Long[] ids(int count) {
        return LongStream.rangeClosed(1, count).boxed().toArray(Long[]::new);
    }
}
