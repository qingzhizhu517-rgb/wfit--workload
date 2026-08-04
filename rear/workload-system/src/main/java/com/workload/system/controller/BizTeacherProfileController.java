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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.enums.BusinessType;
import com.workload.common.utils.excel.ExcelReadUtil;
import com.workload.common.utils.excel.ImportResult;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.dto.TeacherProfileImportDTO;
import com.workload.system.service.IBizTeacherProfileService;
import com.workload.system.service.ITeacherProfileImportService;
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

    @Autowired
    private ITeacherProfileImportService teacherProfileImportService;

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
     * 导入教师业务档案
     */
    @PreAuthorize("@ss.hasPermi('system:teacherProfile:import')")
    @Log(title = "教师业务档案导入", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, @RequestParam(defaultValue = "false") boolean updateSupport) throws Exception
    {
        ImportResult result = ExcelReadUtil.read(file.getInputStream(), TeacherProfileImportDTO.class,
                rows -> teacherProfileImportService.importTeacherProfiles(rows, file.getOriginalFilename(), updateSupport));
        return success(result);
    }

    /**
     * 下载导入模板
     */
    @PreAuthorize("@ss.hasPermi('system:teacherProfile:import')")
    @Log(title = "教师档案导入模板", businessType = BusinessType.EXPORT)
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<TeacherProfileImportDTO> util = new ExcelUtil<TeacherProfileImportDTO>(TeacherProfileImportDTO.class);
        // 添加示例数据行
        List<TeacherProfileImportDTO> example = new java.util.ArrayList<>();
        TeacherProfileImportDTO sample = new TeacherProfileImportDTO();
        sample.setUserCode("T2024001");
        sample.setNickName("张三");
        sample.setDeptName("计算机学院");
        sample.setTitle("讲师");
        sample.setTeacherNature("专任");
        sample.setPhonenumber("13800138000");
        sample.setEmail("zhangsan@example.com");
        example.add(sample);
        util.exportExcel(response, example, "教师档案导入模板");
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
