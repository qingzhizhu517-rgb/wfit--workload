package com.workload.system.controller;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.excel.EasyExcel;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.entity.SysUser;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DataScopeUtil;
import com.workload.common.utils.StringUtils;
import com.workload.system.domain.dto.PaySummaryExportDTO;
import com.workload.system.domain.dto.PersonalWorkloadDetailExportDTO;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.mapper.BizExportMapper;
import com.workload.system.service.ISysUserService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 工作量报表导出 Controller
 *
 * @author wflg
 */
@RestController
@RequestMapping("/system/export")
public class BizExportController extends BaseController
{
    @Autowired
    private BizExportMapper exportMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private RuleParamService ruleParamService;

    /**
     * 导出个人工作量明细表（附件1格式）
     * <p>
     * 每行除核算结果外还带出各系数原值与「系数说明」（第几次开课、哪个班、是否封顶），
     * 用于事后核对「这条为什么只算 0.8」，不必再回查 biz_wl_* 明细表。
     */
    @PreAuthorize("@ss.hasPermi('system:export:personal')")
    @GetMapping("/personalWorkload")
    public void exportPersonalWorkload(
            @RequestParam("userId") Long userId,
            @RequestParam("semester") String semester,
            HttpServletResponse response) throws Exception
    {
        if (userId == null)
        {
            throw new ServiceException("userId 不能为空");
        }
        if (StringUtils.isEmpty(semester))
        {
            throw new ServiceException("semester 不能为空");
        }
        // 教师角色强制导出本人数据，忽略传入的他人 userId（修复越权导出 P2-01）
        userId = DataScopeUtil.resolveUserId(userId);

        List<PersonalWorkloadDetailExportDTO> exportData = exportMapper.selectPersonalWorkloadExport(userId, semester);
        if (exportData.isEmpty())
        {
            // 没有明细即没有可导出的表体；不写空 Excel，避免用户拿到空文件误以为「本学期没工作量」
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("未找到该教师该学期的工作量明细");
            return;
        }
        for (PersonalWorkloadDetailExportDTO dto : exportData)
        {
            dto.setCoefRemark(buildCoefRemark(dto));
        }

        writeExcel(response, "工作量明细_" + userLabel(userId) + "_" + semester + ".xlsx",
                "工作量明细", PersonalWorkloadDetailExportDTO.class, exportData);
    }

    /**
     * 导出绩效酬金统计表（附件2格式）
     * <p>
     * 绩效/其他/总金额三列取 biz_pay_record 落库值，与「酬金记录」页同源；
     * 未核算酬金的教师照样出现，酬金列为空表示「待核算」。
     */
    @PreAuthorize("@ss.hasPermi('system:export:paySummary')")
    @GetMapping("/paySummary")
    public void exportPaySummary(
            @RequestParam("semester") String semester,
            HttpServletResponse response) throws Exception
    {
        if (StringUtils.isEmpty(semester))
        {
            throw new ServiceException("semester 不能为空");
        }
        // 教师角色强制只导出本人数据；管理角色下返回 null，即全学期
        List<PaySummaryExportDTO> exportData =
                exportMapper.selectPaySummaryExport(semester, DataScopeUtil.resolveUserId(null));
        if (exportData.isEmpty())
        {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("未找到该学期的汇总数据");
            return;
        }
        // 封顶线取规则表当前值，勿在 SQL 里写死 540：政策调整只改 biz_workload_rule
        BigDecimal cap200 = ruleParamService.get("CAP_200PCT", new BigDecimal("540"));
        for (PaySummaryExportDTO dto : exportData)
        {
            fillPayExplain(dto, cap200);
        }

        writeExcel(response, "绩效酬金统计_" + semester + ".xlsx",
                "绩效酬金统计", PaySummaryExportDTO.class, exportData);
    }

    /**
     * 补齐附件2 的三列解释信息：是否触顶、计酬超额工作量、备注。
     * <p>
     * 与 {@code SummaryCalcServiceImpl} 的绩效口径严格对齐：
     * 计酬基数 = min(总工作量, CAP_200PCT) − 额定，下限 0；仅「专任」（或无档案）计发。
     * 备注把「为什么是 0」写清楚，免得看表的人拿 超额×单位酬金 去对绩效酬金对不上。
     */
    private void fillPayExplain(PaySummaryExportDTO dto, BigDecimal cap200)
    {
        boolean capped = dto.getCapped() != null && dto.getCapped() == 1;
        dto.setCappedLabel(capped ? "是" : "否");

        BigDecimal total = dto.getTotalWorkload();
        BigDecimal rated = dto.getRatedWorkload();
        if (total != null && rated != null)
        {
            dto.setPayableExcess(total.min(cap200).subtract(rated).max(BigDecimal.ZERO));
        }

        List<String> notes = new ArrayList<>();
        String nature = dto.getTeacherNature();
        if (StringUtils.isNotEmpty(nature) && !"专任".equals(nature))
        {
            notes.add("人员性质「" + nature + "」不计发绩效酬金");
        }
        if (capped)
        {
            notes.add("总工作量已触 200% 上限 " + cap200.stripTrailingZeros().toPlainString() + "，绩效按封顶值计发");
        }
        if (dto.getTotalPay() == null)
        {
            notes.add("尚未核算酬金，请先执行一键核算");
        }
        dto.setRemark(notes.isEmpty() ? "-" : String.join("；", notes));
    }

    /**
     * 拼「系数说明」：把追溯一条明细所需的上下文压进一列。
     * <p>
     * 重复次序是重点——G1 的 C1 与 G3 的 K 都按第几次开课递减，
     * 只看系数值看不出是哪个班被判成第二次，故一并写出班级。
     */
    private String buildCoefRemark(PersonalWorkloadDetailExportDTO dto)
    {
        List<String> parts = new ArrayList<>();
        Long order = dto.getRepeatOrder();
        if (order != null)
        {
            // G3 按「轮」计（实习实训一轮一批学生），其余按「次」
            String unit = "G3".equals(dto.getItemType()) ? "轮" : "次";
            StringBuilder sb = new StringBuilder("第 ").append(order).append(" ").append(unit);
            if (StringUtils.isNotEmpty(dto.getClassName()))
            {
                sb.append("（").append(dto.getClassName()).append("）");
            }
            if (dto.getRepeatCoef() != null)
            {
                sb.append("，重复系数 ").append(dto.getRepeatCoef().stripTrailingZeros().toPlainString());
            }
            parts.add(sb.toString());
        }
        if (dto.getOverLimit() != null && dto.getOverLimit() == 1)
        {
            // 同一个 is_over_limit 标记在 G5 与 G6 上语义不同，不能共用一句话：
            // G6 真按 min(R6, CAP_R6_MAX) 封顶算；G5 从不封顶（ThesisCalcStrategy 无 min），
            // 其标记只表示「人数超申报门槛，须报院长批准、教务处备案」，学时仍按实际人数计。
            // 一律写「已按封顶值核算」会让审核人误判 G5 行的学时被砍过。
            parts.add("G5".equals(dto.getItemType())
                    ? "人数超申报上限，须报院长批准、教务处备案（学时按实际人数计，未封顶）"
                    : "人数超上限，已按封顶值核算");
        }
        if (StringUtils.isNotEmpty(dto.getRoleType()))
        {
            String basis = StringUtils.isNotEmpty(dto.getProrationBasis())
                    ? "，折算依据 " + dto.getProrationBasis() : "";
            parts.add("岗位 " + dto.getRoleType() + basis);
        }
        return parts.isEmpty() ? "-" : String.join("；", parts);
    }

    /** 取「姓名(工号)」作文件名片段；查不到用户则回落 userId，不让导出因此失败 */
    private String userLabel(Long userId)
    {
        try
        {
            SysUser user = sysUserService.selectUserById(userId);
            if (user != null && StringUtils.isNotEmpty(user.getNickName()))
            {
                return user.getNickName() + "(" + user.getUserName() + ")";
            }
        }
        catch (Exception e)
        {
            logger.warn("导出取教师姓名失败，回落 userId: {}", userId, e);
        }
        return String.valueOf(userId);
    }

    /** 统一写出 xlsx：文件名 URL 编码，避免中文名在部分浏览器乱码 */
    private <T> void writeExcel(HttpServletResponse response, String fileName, String sheetName,
                                Class<T> clazz, List<T> data) throws Exception
    {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), clazz).sheet(sheetName).doWrite(data);
    }
}
