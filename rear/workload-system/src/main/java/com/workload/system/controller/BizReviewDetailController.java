package com.workload.system.controller;

import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.core.page.TableDataInfo;
import com.workload.system.review.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/workloadSummary")
public class BizReviewDetailController extends BaseController {
    private final ReviewQueryService query;
    public BizReviewDetailController(ReviewQueryService query){this.query=query;}
    @GetMapping("/{id}/detail") @PreAuthorize("@ss.hasPermi('system:workloadSummary:query')")
    public AjaxResult detail(@PathVariable("id") Long id){return success(query.detail(id));}
    @GetMapping("/{id}/reviews") @PreAuthorize("@ss.hasPermi('system:workloadSummary:query')")
    public TableDataInfo reviews(@PathVariable("id") Long id,@RequestParam Map<String,String> params){var paging=new HashMap<>(params);String batch=paging.remove("batchNo");return query.reviews(id,batch,ReviewCommands.page(paging));}
    @GetMapping("/{id}/auditLogs") @PreAuthorize("@ss.hasPermi('system:workloadSummary:query')")
    public TableDataInfo logs(@PathVariable("id") Long id,@RequestParam Map<String,String> params){return query.logs(id,ReviewCommands.page(params));}
    @GetMapping("/{id}/offlineAdjustmentNotes") @PreAuthorize("@ss.hasPermi('system:workloadSummary:query')")
    public TableDataInfo notes(@PathVariable("id") Long id,@RequestParam Map<String,String> params){return query.notes(id,ReviewCommands.page(params));}
}
