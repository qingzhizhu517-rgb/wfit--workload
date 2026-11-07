package com.workload.system.review;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ReviewControllerContractTest {
    @Test void legacyAuditCommandRejectsUnrecognizedClientIdentityParameters(){
        var request=new org.springframework.mock.web.MockHttpServletRequest();request.addParameter("id","1");request.addParameter("reviewerId","88");
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(request));
        try{var c=new com.workload.system.controller.BizAuditController();org.springframework.test.util.ReflectionTestUtils.setField(c,"bizAuditService",org.mockito.Mockito.mock(com.workload.system.service.BizAuditService.class));assertThatThrownBy(()->c.submit(1L)).isInstanceOf(com.workload.common.exception.ServiceException.class).extracting("code").isEqualTo(400);}
        finally{org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();}
    }
    @Test void controllersExistForSeparateReviewAndAppendOnlyNotePermissions(){
        for(String name:new String[]{"BizAuditRecordController","BizReviewDetailController","BizOfflineAdjustmentNoteController"})
            assertThatCode(()->Class.forName("com.workload.system.controller."+name)).doesNotThrowAnyException();
    }
}
