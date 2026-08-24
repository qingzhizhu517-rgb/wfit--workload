package com.workload.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.workload.common.exception.ServiceException;
import com.workload.common.utils.SecurityUtils;
import com.workload.common.core.domain.entity.SysDept;
import com.workload.common.core.domain.entity.SysUser;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.dto.TeacherProfileImportDTO;
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
            teacherProfileMapper.insertBizTeacherProfile(profile);
        }
    }
}
