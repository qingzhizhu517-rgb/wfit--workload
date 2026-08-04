package com.workload.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
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

    /** 教师角色ID */
    private static final Long TEACHER_ROLE_ID = 4L;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysDeptService sysDeptService;

    @Autowired
    private BizTeacherProfileMapper teacherProfileMapper;

    /** 部门名称缓存，避免 N+1 查询 */
    private final Map<String, Long> deptCache = new HashMap<>();

    @Override
    public void importTeacherProfiles(List<TeacherProfileImportDTO> rows, String fileName, boolean updateSupport)
    {
        deptCache.clear();
        // 获取代理对象以支持事务
        TeacherProfileImportServiceImpl proxy = (TeacherProfileImportServiceImpl) AopContext.currentProxy();
        for (TeacherProfileImportDTO row : rows)
        {
            proxy.processSingleRow(row, updateSupport);
        }
    }

    /**
     * 处理单行导入（每行独立事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public void processSingleRow(TeacherProfileImportDTO dto, boolean updateSupport)
    {
        // 1. 校验必填字段
        validateRow(dto);

        // 2. 查找或创建系统用户
        SysUser user = findOrCreateUser(dto);

        // 3. 创建或更新教师档案
        createOrUpdateTeacherProfile(dto, user, updateSupport);
    }

    /**
     * 校验必填字段
     */
    private void validateRow(TeacherProfileImportDTO dto)
    {
        if (!StringUtils.hasText(dto.getUserCode()))
        {
            throw new ServiceException("教师工号不能为空");
        }
        if (!StringUtils.hasText(dto.getNickName()))
        {
            throw new ServiceException("教师姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getDeptName()))
        {
            throw new ServiceException("院部名称不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle()))
        {
            throw new ServiceException("职称不能为空");
        }
        // 校验职称
        String title = dto.getTitle();
        if (!title.matches("教授|副教授|讲师|助教|未定级"))
        {
            throw new ServiceException("职称必须为：教授/副教授/讲师/助教/未定级，当前: " + title);
        }
        // 校验人员性质（如果填写了）
        if (StringUtils.hasText(dto.getTeacherNature()))
        {
            String nature = dto.getTeacherNature();
            if (!nature.matches("专任|外聘|校企|银龄|青州外聘"))
            {
                throw new ServiceException("人员性质必须为：专任/外聘/校企/银龄/青州外聘，当前: " + nature);
            }
        }
    }

    /**
     * 查找或创建系统用户
     */
    private SysUser findOrCreateUser(TeacherProfileImportDTO dto)
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
        Long deptId = findDeptId(dto.getDeptName());
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

        // 设置默认密码（工号后6位或默认密码）
        String defaultPassword = SecurityUtils.encryptPassword("123456");
        user.setPassword(defaultPassword);

        // 设置状态正常
        user.setStatus("0");

        // 插入用户
        sysUserService.insertUser(user);

        // 分配教师角色
        sysUserService.insertUserAuth(user.getUserId(), new Long[]{TEACHER_ROLE_ID});

        return user;
    }

    /**
     * 根据部门名称查找部门ID（带缓存）
     */
    private Long findDeptId(String deptName)
    {
        // 先查缓存
        if (deptCache.containsKey(deptName))
        {
            return deptCache.get(deptName);
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
