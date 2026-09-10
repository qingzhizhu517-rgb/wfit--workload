package com.workload.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.workload.system.domain.dto.Attachment1RowDTO;
import com.workload.system.domain.dto.Attachment1TeacherDTO;
import com.workload.system.domain.dto.PaySummaryExportDTO;
import com.workload.system.domain.dto.PersonalWorkloadDetailExportDTO;

/**
 * 报表导出专用查询 Mapper
 * <p>
 * 附件1/附件2 都要横跨「明细主表 + 7 张 G 明细表 + 教学任务」或
 * 「学期汇总 + 酬金记录 + sys_user/sys_dept」，用通用 Service 逐条拼装既慢又拿不到系数原值，
 * 故单列一个导出 Mapper 一次 JOIN 取齐，避免污染各实体的 CRUD Mapper。
 *
 * @author wflg
 */
public interface BizExportMapper
{
    /**
     * 附件1：某教师某学期工作量明细（含各系数原值与班级，供追溯重复系数）
     *
     * @param userId   教师用户ID
     * @param semester 学年学期
     * @return 明细行，按类别 + 主键排序
     */
    public List<PersonalWorkloadDetailExportDTO> selectPersonalWorkloadExport(@Param("userId") Long userId,
                                                                             @Param("semester") String semester);

    /**
     * 附件2：某学期绩效酬金统计（汇总 LEFT JOIN 酬金记录，未核算酬金者酬金列为空）
     *
     * @param semester 学年学期
     * @param userId   限定教师（教师角色只导本人）；null 表示全学期
     * @return 统计行，按院部 + 工号排序
     */
    public List<PaySummaryExportDTO> selectPaySummaryExport(@Param("semester") String semester,
                                                           @Param("userId") Long userId);

    /**
     * 附件1（标准表一）：按「开课任务」聚合的教师工作量子句行。
     * <p>
     * 同一 task_id 的 G1~G6 明细合为一行（系数取 max、工作量/学时取 sum），
     * 手工/申报明细无 task_id，按 item_id 独立成行（分组键 ifnull(task_id, -id)）。
     *
     * @param userId   教师用户ID
     * @param semester 学年学期
     * @return 任务行，按明细主键插入序排序
     */
    public List<Attachment1RowDTO> selectAttachment1Rows(@Param("userId") Long userId,
                                                         @Param("semester") String semester);

    /**
     * 附件1（标准表一）：教师级信息（首行 A/B 列、AC~AM 列、标题行学年/学院）。
     * <p>
     * 取 biz_workload_summary 落库值而非现算——导出必须与「汇总页/酬金页」同源，
     * 否则教师看到导出数字与系统页面不一致。
     *
     * @param userId   教师用户ID
     * @param semester 学年学期
     * @return 教师级数据；未核算（无 summary）返回 null
     */
    public Attachment1TeacherDTO selectAttachment1Teacher(@Param("userId") Long userId,
                                                          @Param("semester") String semester);
}
