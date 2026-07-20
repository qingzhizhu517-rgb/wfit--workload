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
import com.workload.system.domain.BizWlCourseDesign;
import com.workload.system.service.IBizWlCourseDesignService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * G4课程设计明细Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/wlCourseDesign")
public class BizWlCourseDesignController extends BaseController
{
    @Autowired
    private IBizWlCourseDesignService bizWlCourseDesignService;

    /**
     * 查询G4课程设计明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlCourseDesign:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWlCourseDesign bizWlCourseDesign)
    {
        startPage();
        List<BizWlCourseDesign> list = bizWlCourseDesignService.selectBizWlCourseDesignList(bizWlCourseDesign);
        return getDataTable(list);
    }

    /**
     * 导出G4课程设计明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlCourseDesign:export')")
    @Log(title = "G4课程设计明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWlCourseDesign bizWlCourseDesign)
    {
        List<BizWlCourseDesign> list = bizWlCourseDesignService.selectBizWlCourseDesignList(bizWlCourseDesign);
        ExcelUtil<BizWlCourseDesign> util = new ExcelUtil<BizWlCourseDesign>(BizWlCourseDesign.class);
        util.exportExcel(response, list, "G4课程设计明细数据");
    }

    /**
     * 获取G4课程设计明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:wlCourseDesign:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId)
    {
        return success(bizWlCourseDesignService.selectBizWlCourseDesignByItemId(itemId));
    }

    /**
     * 新增G4课程设计明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlCourseDesign:add')")
    @Log(title = "G4课程设计明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWlCourseDesign bizWlCourseDesign)
    {
        return toAjax(bizWlCourseDesignService.insertBizWlCourseDesign(bizWlCourseDesign));
    }

    /**
     * 修改G4课程设计明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlCourseDesign:edit')")
    @Log(title = "G4课程设计明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWlCourseDesign bizWlCourseDesign)
    {
        return toAjax(bizWlCourseDesignService.updateBizWlCourseDesign(bizWlCourseDesign));
    }

    /**
     * 删除G4课程设计明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlCourseDesign:remove')")
    @Log(title = "G4课程设计明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(bizWlCourseDesignService.deleteBizWlCourseDesignByItemIds(itemIds));
    }
}
