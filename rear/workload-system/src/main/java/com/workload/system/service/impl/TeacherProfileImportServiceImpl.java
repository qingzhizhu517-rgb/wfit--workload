package com.workload.system.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import com.workload.common.utils.SecurityUtils;
import com.workload.common.core.domain.entity.SysDept;
import com.workload.common.core.domain.entity.SysUser;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.dto.TeacherProfileImportDTO;
import com.workload.system.domain.dto.TeacherRosterImportDTO;
import com.workload.system.domain.dto.importing.ImportRowView;
import com.workload.system.domain.dto.importing.TeacherRosterPreviewResponse;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.service.ITeacherProfileImportService;
import com.workload.system.service.ISysDeptService;
import com.workload.system.service.ISysUserService;

/**
 * 教师档案 Excel 导入服务实现
 *
 * @author wflg
 */
@Service
public class TeacherProfileImportServiceImpl implements ITeacherProfileImportService
{
    private static final Logger log = LoggerFactory.getLogger(TeacherProfileImportServiceImpl.class);

    /** 教师角色ID：与 sys_role role_id=4 教师角色对应，变更需同步 rear/sql/06_test_accounts.sql */
    private static final Long TEACHER_ROLE_ID = 4L;

    /** 手机号校验规则：仅当单元格非空时校验 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 邮箱校验规则：仅当单元格非空时校验 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    /**
     * 新建教师初始密码策略：导入时统一使用该默认密码，教师首次登录后须自行修改密码。
     * 由配置项 wfit.default-password 注入（见 application.yml wfit 段）。
     */
    @Value("${wfit.default-password:123456}")
    private String defaultPassword;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysDeptService sysDeptService;

    @Autowired
    private BizTeacherProfileMapper teacherProfileMapper;

    @Override
    public void importTeacherProfiles(List<TeacherProfileImportDTO> rows, String fileName, boolean updateSupport, int startRowNumber)
    {
        // 方法局部部门缓存：每次导入自建，避免实例级缓存跨请求/并发导入导致的脏数据
        Map<String, Long> deptCache = new HashMap<>();
        // 获取代理对象以支持事务
        TeacherProfileImportServiceImpl proxy = (TeacherProfileImportServiceImpl) AopContext.currentProxy();
        for (int i = 0; i < rows.size(); i++)
        {
            proxy.processSingleRow(rows.get(i), updateSupport, deptCache, startRowNumber + i);
        }
    }

    /**
     * 处理单行导入（每行独立事务）
     *
     * @param rowNumber 数据行号（从 1 开始，与 ExcelImportListener 错误行号口径一致）
     */
    @Transactional(rollbackFor = Exception.class)
    public void processSingleRow(TeacherProfileImportDTO dto, boolean updateSupport, Map<String, Long> deptCache, int rowNumber)
    {
        // 1. 校验必填字段
        validateRow(dto, rowNumber);

        // 2. 查找或创建系统用户
        SysUser user = findOrCreateUser(dto, deptCache);

        // 3. 创建或更新教师档案
        createOrUpdateTeacherProfile(dto, user, updateSupport);
    }

    /**
     * 校验必填字段与格式（错误信息带行号，与 ImportResult.ErrorRow “第 X 行”风格一致）
     */
    private void validateRow(TeacherProfileImportDTO dto, int rowNumber)
    {
        String rowPrefix = "第 " + rowNumber + " 行：";
        if (!StringUtils.hasText(dto.getUserCode()))
        {
            throw new ServiceException(rowPrefix + "教师工号不能为空");
        }
        if (!StringUtils.hasText(dto.getNickName()))
        {
            throw new ServiceException(rowPrefix + "教师姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getDeptName()))
        {
            throw new ServiceException(rowPrefix + "院部名称不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle()))
        {
            throw new ServiceException(rowPrefix + "职称不能为空");
        }
        // 校验职称
        String title = dto.getTitle();
        if (!title.matches("教授|副教授|讲师|助教|未定级"))
        {
            throw new ServiceException(rowPrefix + "职称必须为：教授/副教授/讲师/助教/未定级，当前: " + title);
        }
        // 校验人员性质（如果填写了）
        if (StringUtils.hasText(dto.getTeacherNature()))
        {
            String nature = dto.getTeacherNature();
            if (!nature.matches("专任|外聘|校企|银龄|青州外聘"))
            {
                throw new ServiceException(rowPrefix + "人员性质必须为：专任/外聘/校企/银龄/青州外聘，当前: " + nature);
            }
        }
        // 校验手机号（仅当单元格非空时）
        if (StringUtils.hasText(dto.getPhonenumber()) && !PHONE_PATTERN.matcher(dto.getPhonenumber().trim()).matches())
        {
            throw new ServiceException(rowPrefix + "手机号格式不正确，应为 11 位大陆手机号，当前: " + dto.getPhonenumber());
        }
        // 校验邮箱（仅当单元格非空时）
        if (StringUtils.hasText(dto.getEmail()) && !EMAIL_PATTERN.matcher(dto.getEmail().trim()).matches())
        {
            throw new ServiceException(rowPrefix + "邮箱格式不正确，当前: " + dto.getEmail());
        }
    }

    /**
     * 查找或创建系统用户
     */
    private SysUser findOrCreateUser(TeacherProfileImportDTO dto, Map<String, Long> deptCache)
    {
        // 先按工号查找
        SysUser user = sysUserService.selectUserByUserName(dto.getUserCode());
        if (user != null)
        {
            // 用户已存在，更新昵称（如果有变化）
            if (StringUtils.hasText(dto.getNickName()) && !dto.getNickName().equals(user.getNickName()))
            {
                user.setNickName(dto.getNickName());
                sysUserService.updateUser(user);
            }
            return user;
        }

        // 用户不存在，创建新用户
        user = new SysUser();
        user.setUserName(dto.getUserCode());
        user.setNickName(dto.getNickName());

        // 查找部门
        Long deptId = findDeptId(dto.getDeptName(), deptCache);
        user.setDeptId(deptId);

        // 设置可选字段
        if (StringUtils.hasText(dto.getPhonenumber()))
        {
            user.setPhonenumber(dto.getPhonenumber());
        }
        if (StringUtils.hasText(dto.getEmail()))
        {
            user.setEmail(dto.getEmail());
        }

        // 设置初始密码（统一默认密码策略，见 defaultPassword 字段注释）
        user.setPassword(SecurityUtils.encryptPassword(defaultPassword));
        // pwdUpdateDate 刻意保持 null：表示“从未修改过初始密码”，
        // ForcePasswordChangeInterceptor 据此强制该教师首次登录后必须改密。
        // SysUserMapper.insertUser 对该字段有 <if test="pwdUpdateDate != null"> 条件，
        // 不赋值即不写入该列，DDL 无默认值故落库为 NULL。切勿在此设置当前时间。
        user.setPwdUpdateDate(null);

        // 设置状态正常
        user.setStatus("0");

        // 插入用户
        sysUserService.insertUser(user);

        // 分配教师角色
        sysUserService.insertUserAuth(user.getUserId(), new Long[]{TEACHER_ROLE_ID});

        return user;
    }

    /**
     * 根据部门名称查找部门ID（导入方法内局部缓存）
     * <p>
     * 部门不存在时抛出 ServiceException：该异常会使当前行事务回滚，
     * 并由导入监听器（batchSize=1）捕获计入该行失败明细，不再静默落空。
     */
    private Long findDeptId(String deptName, Map<String, Long> deptCache)
    {
        // 先查本次导入的局部缓存
        Long cached = deptCache.get(deptName);
        if (cached != null)
        {
            return cached;
        }
        // 查数据库
        SysDept query = new SysDept();
        query.setDeptName(deptName);
        List<SysDept> depts = sysDeptService.selectDeptList(query);
        if (depts == null || depts.isEmpty())
        {
            throw new ServiceException("院部 '" + deptName + "' 不存在，请先在系统中创建该部门");
        }
        Long deptId = depts.get(0).getDeptId();
        deptCache.put(deptName, deptId);
        return deptId;
    }

    /**
     * 创建或更新教师档案
     */
    private void createOrUpdateTeacherProfile(TeacherProfileImportDTO dto, SysUser user, boolean updateSupport)
    {
        // 检查是否已有档案
        BizTeacherProfile existing = teacherProfileMapper.selectBizTeacherProfileByUserId(user.getUserId());
        if (existing != null)
        {
            if (!updateSupport)
            {
                log.info("教师档案已存在，跳过: userId={}, 工号={}", user.getUserId(), dto.getUserCode());
                return;
            }
            // 更新现有档案
            existing.setTitle(dto.getTitle());
            if (StringUtils.hasText(dto.getTeacherNature()))
            {
                existing.setTeacherNature(dto.getTeacherNature());
            }
            teacherProfileMapper.updateBizTeacherProfile(existing);
        }
        else
        {
            // 创建新档案
            BizTeacherProfile profile = new BizTeacherProfile();
            profile.setUserId(user.getUserId());
            profile.setTitle(dto.getTitle());
            profile.setTeacherNature(StringUtils.hasText(dto.getTeacherNature()) ? dto.getTeacherNature() : "专任");
            profile.setSpecialStatus("正常");
            profile.setDeptId(user.getDeptId());
            // 导入直连 mapper 未经 Service，需手动补审计字段，否则详情页创建时间/创建者为空
            profile.setCreateBy(SecurityUtils.getUsername());
            profile.setCreateTime(DateUtils.getNowDate());
            teacherProfileMapper.insertBizTeacherProfile(profile);
        }
    }

    // ==================== 花名册字段白名单预检（Task 6，preview-only） ====================

    /** 花名册 Sheet1 表头名 → 白名单字段的显式映射：仅这些列会被读入 DTO，其余列（含敏感列）一律忽略。 */
    private static final Map<String, java.util.function.BiConsumer<TeacherRosterImportDTO, String>> ROSTER_WHITELIST =
            buildRosterWhitelist();

    /** 敏感列关键字：表头包含任一关键字即判为敏感列（只提示表头名，绝不读取其值）。 */
    private static final String[] SENSITIVE_KEYWORDS = {
            "身份证", "住址", "家庭住址", "现住址", "银行卡", "开户行", "卡号",
            "籍贯", "户籍", "出生", "民族", "政治面貌"};

    /** 岗位 → 人员性质自动映射（默认口径，可后续配置化）。未命中即进入待确认队列并阻断。 */
    private static final Map<String, String> NATURE_MAPPING = Map.of(
            "教师", "专任",
            "银铃教师", "银龄",
            "银龄教师", "银龄");

    /** 已知职称集合：命中即可映射；「无」「讲师待遇」等未命中值进入待确认并阻断。 */
    private static final Set<String> KNOWN_TITLES = Set.of("教授", "副教授", "讲师", "助教", "未定级");

    private static Map<String, java.util.function.BiConsumer<TeacherRosterImportDTO, String>> buildRosterWhitelist()
    {
        Map<String, java.util.function.BiConsumer<TeacherRosterImportDTO, String>> map = new HashMap<>();
        map.put("系", TeacherRosterImportDTO::setDepartmentName);
        map.put("教研室", TeacherRosterImportDTO::setTeachingOffice);
        map.put("员工姓名", TeacherRosterImportDTO::setNickName);
        map.put("职务", TeacherRosterImportDTO::setPositionName);
        map.put("岗位", TeacherRosterImportDTO::setEmploymentType);
        map.put("职称", TeacherRosterImportDTO::setTitle);
        map.put("工号", TeacherRosterImportDTO::setUserCode);
        map.put("联系方式", TeacherRosterImportDTO::setPhonenumber);
        map.put("职务匹配工作量", (dto, v) -> dto.setPositionWorkload(parseDecimal(v)));
        return map;
    }

    private static BigDecimal parseDecimal(String v)
    {
        if (!StringUtils.hasText(v))
        {
            return null;
        }
        try
        {
            return new BigDecimal(v.trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    /** 解析出的一行：物理行号 + 白名单字段。 */
    private record RosterRow(int rowNo, TeacherRosterImportDTO dto)
    {
    }

    @Override
    public TeacherRosterPreviewResponse previewRoster(InputStream input, String fileName)
    {
        byte[] bytes = readAllBytes(input);

        List<RosterRow> parsed = new ArrayList<>();
        List<String> allHeaders = new ArrayList<>();
        // 按列名显式挑白名单列读入 DTO；敏感列的值绝不进入任何字段（无论列序如何）。
        parseRosterWhitelist(bytes, parsed, allHeaders);

        return buildPreview(parsed, allHeaders);
    }

    /**
     * 用 Map 读取花名册：invokeHeadMap 拿到全部表头名并只锁定白名单列的列索引；
     * invoke 时<b>只</b>按白名单列索引取值写入 DTO，敏感列索引一律不读，从根上杜绝敏感值进入内存后续流程。
     */
    private void parseRosterWhitelist(byte[] bytes, List<RosterRow> parsed, List<String> allHeaders)
    {
        EasyExcel.read(new ByteArrayInputStream(bytes), new AnalysisEventListener<Map<Integer, String>>()
        {
            /** 列索引 → 白名单字段 setter（仅白名单列入表，敏感列不入表故永不被读值）。 */
            private final Map<Integer, java.util.function.BiConsumer<TeacherRosterImportDTO, String>> whitelistCols =
                    new HashMap<>();

            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context)
            {
                headMap.forEach((idx, name) ->
                {
                    if (name == null)
                    {
                        return;
                    }
                    String header = name.trim();
                    allHeaders.add(header);
                    java.util.function.BiConsumer<TeacherRosterImportDTO, String> setter = ROSTER_WHITELIST.get(header);
                    if (setter != null)
                    {
                        whitelistCols.put(idx, setter);
                    }
                });
            }

            @Override
            public void invoke(Map<Integer, String> rowData, AnalysisContext context)
            {
                TeacherRosterImportDTO dto = new TeacherRosterImportDTO();
                // 只遍历白名单列索引取值；敏感列索引不在 whitelistCols 中，其值绝不被读取
                whitelistCols.forEach((idx, setter) -> setter.accept(dto, rowData.get(idx)));
                int rowNo = context.readRowHolder().getRowIndex();
                parsed.add(new RosterRow(rowNo, dto));
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context)
            {
                // 无需收尾
            }
        }).sheet().doRead();
    }

    /** 由解析结果计算统计、异常队列与逐行视图（纯内存，不落任何表）。 */
    private TeacherRosterPreviewResponse buildPreview(List<RosterRow> parsed, List<String> allHeaders)
    {
        // 1) 工号计数（用于重复判定）
        Map<String, Integer> codeCounts = new LinkedHashMap<>();
        for (RosterRow r : parsed)
        {
            String code = trimToNull(r.dto().getUserCode());
            if (code != null)
            {
                codeCounts.merge(code, 1, Integer::sum);
            }
        }
        List<String> duplicateCodes = new ArrayList<>();
        codeCounts.forEach((code, cnt) ->
        {
            if (cnt > 1)
            {
                duplicateCodes.add(code);
            }
        });
        // 重复工号 → 出现的行号，供警告定位
        Map<String, List<Integer>> dupRowNos = new LinkedHashMap<>();

        // 2) 逐行判定 + 统计
        Map<String, Integer> deptCounts = new LinkedHashMap<>();
        Map<String, Integer> employmentTypeCounts = new LinkedHashMap<>();
        Set<String> unresolvedTitles = new LinkedHashSet<>();
        Set<String> unresolvedNatures = new LinkedHashSet<>();
        List<ImportRowView> rows = new ArrayList<>();
        int ready = 0;
        int blocked = 0;

        for (RosterRow r : parsed)
        {
            TeacherRosterImportDTO dto = r.dto();
            String code = trimToNull(dto.getUserCode());
            String title = trimToNull(dto.getTitle());
            String employment = trimToNull(dto.getEmploymentType());

            deptCounts.merge(defaultLabel(dto.getDepartmentName()), 1, Integer::sum);
            employmentTypeCounts.merge(defaultLabel(dto.getEmploymentType()), 1, Integer::sum);

            List<String> reasons = new ArrayList<>();
            if (code == null)
            {
                reasons.add("工号不能为空");
            }
            else if (codeCounts.getOrDefault(code, 0) > 1)
            {
                reasons.add("工号重复：" + code);
                dupRowNos.computeIfAbsent(code, k -> new ArrayList<>()).add(r.rowNo());
            }
            if (!StringUtils.hasText(dto.getNickName()))
            {
                reasons.add("员工姓名不能为空");
            }
            if (title == null || !KNOWN_TITLES.contains(title))
            {
                unresolvedTitles.add(title == null ? "(空)" : title);
                reasons.add("职称未映射：" + (title == null ? "(空)" : title));
            }
            if (employment == null || !NATURE_MAPPING.containsKey(employment))
            {
                unresolvedNatures.add(employment == null ? "(空)" : employment);
                reasons.add("人员性质待确认（岗位=" + (employment == null ? "(空)" : employment) + "）");
            }

            String status = reasons.isEmpty() ? "READY" : "BLOCKED";
            if (reasons.isEmpty())
            {
                ready++;
            }
            else
            {
                blocked++;
            }
            rows.add(new ImportRowView(r.rowNo(), code, trimToNull(dto.getNickName()), status,
                    reasons.isEmpty() ? null : "VALIDATION",
                    reasons.isEmpty() ? null : truncate(String.join("；", reasons), 500)));
        }

        // 3) 敏感列表头（只名不含值）
        List<String> sensitiveHeaders = new ArrayList<>();
        for (String header : allHeaders)
        {
            if (isSensitiveHeader(header) && !sensitiveHeaders.contains(header))
            {
                sensitiveHeaders.add(header);
            }
        }

        // 4) 警告
        List<String> warnings = new ArrayList<>();
        warnings.add("本次为教师档案字段参考预检，不会导入任何真实教师，仅用于字段白名单核对与异常报告");
        dupRowNos.forEach((code, rowNos) ->
                warnings.add("工号 " + code + " 重复，出现在第 " + rowNos + " 行（物理行号，含表头 0 基），须人工选择保留或合并，系统不自动合并"));
        if (!sensitiveHeaders.isEmpty())
        {
            warnings.add("检测到敏感列（" + String.join("、", sensitiveHeaders) + "），已忽略不予读取或落库，仅提示表头");
        }
        if (blocked > 0)
        {
            warnings.add("有 " + blocked + " 行被阻断（重复工号/未知职称/待确认人员性质/缺必填）");
        }

        int uniqueCodes = codeCounts.size();
        return new TeacherRosterPreviewResponse(parsed.size(), ready, blocked, uniqueCodes,
                duplicateCodes, deptCounts, employmentTypeCounts,
                new ArrayList<>(unresolvedTitles), new ArrayList<>(unresolvedNatures),
                sensitiveHeaders, rows, warnings);
    }

    private static boolean isSensitiveHeader(String header)
    {
        if (!StringUtils.hasText(header))
        {
            return false;
        }
        for (String kw : SENSITIVE_KEYWORDS)
        {
            if (header.contains(kw))
            {
                return true;
            }
        }
        return false;
    }

    private static String trimToNull(String s)
    {
        if (s == null)
        {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String defaultLabel(String s)
    {
        String t = trimToNull(s);
        return t == null ? "(未填写)" : t;
    }

    private static String truncate(String s, int max)
    {
        if (s == null)
        {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private byte[] readAllBytes(InputStream input)
    {
        try
        {
            return input.readAllBytes();
        }
        catch (IOException e)
        {
            throw new ServiceException("读取花名册文件失败：" + e.getMessage());
        }
    }
}
