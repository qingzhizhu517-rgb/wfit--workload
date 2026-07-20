package com.workload.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.enums.BusinessType;
import com.workload.system.domain.BizWlInternshipTraining;
import com.workload.system.service.IBizWlInternshipTrainingService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * G3教学实习实训明细Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/wlInternshipTraining")
public class BizWlInternshipTrainingController extends BaseController
{
    @Autowired
    private IBizWlInternshipTrainingService bizWlInternshipTrainingService;

    /**
     * 查询G3教学实习实训明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlInternshipTraining:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWlInternshipTraining bizWlInternshipTraining)
    {
        startPage();
        List<BizWlInternshipTraining> list = bizWlInternshipTrainingService.selectBizWlInternshipTrainingList(bizWlInternshipTraining);
        return getDataTable(list);
    }

    /**
     * 导出G3教学实习实训明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlInternshipTraining:export')")
    @Log(title = "G3教学实习实训明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWlInternshipTraining bizWlInternshipTraining)
    {
        List<BizWlInternshipTraining> list = bizWlInternshipTrainingService.selectBizWlInternshipTrainingList(bizWlInternshipTraining);
        ExcelUtil<BizWlInternshipTraining> util = new ExcelUtil<BizWlInternshipTraining>(BizWlInternshipTraining.class);
        util.exportExcel(response, list, "G3教学实习实训明细数据");
    }

    /**
     * 获取G3教学实习实训明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:wlInternshipTraining:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId)
    {
        return success(bizWlInternshipTrainingService.selectBizWlInternshipTrainingByItemId(itemId));
    }

    /**
     * 新增G3教学实习实训明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlInternshipTraining:add')")
    @Log(title = "G3教学实习实训明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWlInternshipTraining bizWlInternshipTraining)
    {
        return toAjax(bizWlInternshipTrainingService.insertBizWlInternshipTraining(bizWlInternshipTraining));
    }

    /**
     * 修改G3教学实习实训明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlInternshipTraining:edit')")
    @Log(title = "G3教学实习实训明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWlInternshipTraining bizWlInternshipTraining)
    {
        return toAjax(bizWlInternshipTrainingService.updateBizWlInternshipTraining(bizWlInternshipTraining));
    }

    /**
     * 删除G3教学实习实训明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlInternshipTraining:remove')")
    @Log(title = "G3教学实习实训明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(bizWlInternshipTrainingService.deleteBizWlInternshipTrainingByItemIds(itemIds));
    }
}
