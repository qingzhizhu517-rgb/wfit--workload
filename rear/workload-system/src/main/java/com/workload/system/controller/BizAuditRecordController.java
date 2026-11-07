package com.workload.system.controller;

import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.core.page.TableDataInfo;
import com.workload.system.review.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/auditRecord")
public class BizAuditRecordController extends BaseController {
    private final ReviewQueryService query;
    private final ItemReviewService items;
    private final ReviewBatchCommandService batches;
    public BizAuditRecordController(ReviewQueryService query,ItemReviewService items,ReviewBatchCommandService batches){this.query=query;this.items=items;this.batches=batches;}
    @GetMapping("/list") @PreAuthorize("@ss.hasPermi('system:auditRecord:list')")
    public TableDataInfo list(@RequestParam Map<String,String> params){
        Set<String> allowed=Set.of("pageNum","pageSize","summaryId","userId","reviewType","result","batchNo");if(!allowed.containsAll(params.keySet()))throw ReviewCommands.bad("不支持的查询字段");
        Map<String,String> pagination=new HashMap<>(params);for(String key:List.of("summaryId","userId","reviewType","result","batchNo"))pagination.remove(key);
        return query.center(ReviewCommands.positiveId(params.get("summaryId")),params.get("batchNo"),ReviewCommands.positiveId(params.get("userId")),params.get("reviewType"),params.get("result"),ReviewCommands.page(pagination));
    }
    @GetMapping("/{id}") @PreAuthorize("@ss.hasPermi('system:auditRecord:query')")
    public AjaxResult get(@PathVariable("id") Long id){return success(query.review(id));}
    @PostMapping("/{id}/pass") @PreAuthorize("@ss.hasPermi('system:auditRecord:review')")
    public AjaxResult pass(@PathVariable("id") Long id,@RequestBody String body){return execute(id,"PASS",body);}
    @PostMapping("/{id}/reject") @PreAuthorize("@ss.hasPermi('system:auditRecord:review')")
    public AjaxResult reject(@PathVariable("id") Long id,@RequestBody String body){return execute(id,"REJECT",body);}
    @PostMapping("/{id}/waive") @PreAuthorize("@ss.hasPermi('system:auditRecord:waive')")
    public AjaxResult waive(@PathVariable("id") Long id,@RequestBody String body){return execute(id,"WAIVE",body);}
    @PostMapping("/batch/pass") @PreAuthorize("@ss.hasPermi('system:auditRecord:review')")
    public AjaxResult batchPass(@RequestBody String body){return batch("PASS",body);}
    @PostMapping("/batch/reject") @PreAuthorize("@ss.hasPermi('system:auditRecord:review')")
    public AjaxResult batchReject(@RequestBody String body){return batch("REJECT",body);}
    @PostMapping("/batch/waive") @PreAuthorize("@ss.hasPermi('system:auditRecord:waive')")
    public AjaxResult batchWaive(@RequestBody String body){return batch("WAIVE",body);}
    private AjaxResult execute(Long id,String action,String body){items.review(id,action,ReviewCommands.review(ReviewCommands.parse(body),action));return success();}
    private AjaxResult batch(String action,String body){return success(batches.review(ReviewCommands.batch(ReviewCommands.parse(body),action),action));}
}
