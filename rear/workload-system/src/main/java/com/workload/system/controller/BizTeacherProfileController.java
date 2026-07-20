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
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.service.IBizTeacherProfileService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * 教师业务档案Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/teacherProfile")
public class BizTeacherProfileController extends BaseController
{
    @Autowired
    private IBizTeacherProfileService bizTeacherProfileService;

    /**
     * 查询教师业务档案列表
     */
    @PreAuthorize("@ss.hasPermi('system:teacherProfile:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizTeacherProfile bizTeacherProfile)
    {
        startPage();
        List<BizTeacherProfile> list = bizTeacherProfileService.selectBizTeacherProfileList(bizTeacherProfile);
        return getDataTable(list);
    }

    /**
     * 导出教师业务档案列表
     */
    @PreAuthorize("@ss.hasPermi('system:teacherProfile:export')")
    @Log(title = "教师业务档案", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizTeacherProfile bizTeacherProfile)
    {
        List<BizTeacherProfile> list = bizTeacherProfileService.selectBizTeacherProfileList(bizTeacherProfile);
        ExcelUtil<BizTeacherProfile> util = new ExcelUtil<BizTeacherProfile>(BizTeacherProfile.class);
        util.exportExcel(response, list, "教师业务档案数据");
    }

    /**
     * 获取教师业务档案详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:teacherProfile:query')")
    @GetMapping(value = "/{userId}")
    public AjaxResult getInfo(@PathVariable("userId") Long userId)
    {
        return success(bizTeacherProfileService.selectBizTeacherProfileByUserId(userId));
    }

    /**
     * 新增教师业务档案
     */
    @PreAuthorize("@ss.hasPermi('system:teacherProfile:add')")
    @Log(title = "教师业务档案", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizTeacherProfile bizTeacherProfile)
    {
        return toAjax(bizTeacherProfileService.insertBizTeacherProfile(bizTeacherProfile));
    }

    /**
     * 修改教师业务档案
     */
    @PreAuthorize("@ss.hasPermi('system:teacherProfile:edit')")
    @Log(title = "教师业务档案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizTeacherProfile bizTeacherProfile)
    {
        return toAjax(bizTeacherProfileService.updateBizTeacherProfile(bizTeacherProfile));
    }

    /**
     * 删除教师业务档案
     */
    @PreAuthorize("@ss.hasPermi('system:teacherProfile:remove')")
    @Log(title = "教师业务档案", businessType = BusinessType.DELETE)
	@DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        return toAjax(bizTeacherProfileService.deleteBizTeacherProfileByUserIds(userIds));
    }
}
