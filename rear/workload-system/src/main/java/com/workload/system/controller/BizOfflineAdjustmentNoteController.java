package com.workload.system.controller;

import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.core.page.TableDataInfo;
import com.workload.system.review.*;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/offlineAdjustmentNote")
public class BizOfflineAdjustmentNoteController extends BaseController {
    private final OfflineAdjustmentNoteService service;
    private final ReviewQueryService query;
    public BizOfflineAdjustmentNoteController(OfflineAdjustmentNoteService service,ReviewQueryService query){this.service=service;this.query=query;}
    @PostMapping("/{summaryId}") @PreAuthorize("@ss.hasPermi('system:offlineAdjustmentNote:add')")
    public AjaxResult add(@PathVariable("summaryId") Long summaryId,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody String body){return success(service.append(summaryId,ReviewCommands.note(ReviewCommands.parse(body),key)));}
    @GetMapping("/{summaryId}") @PreAuthorize("@ss.hasPermi('system:offlineAdjustmentNote:query')")
    public TableDataInfo list(@PathVariable("summaryId") Long summaryId,@RequestParam Map<String,String> params){return query.notes(summaryId,ReviewCommands.page(params));}
}
