package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.service.IBizTeacherProfileService;

/**
 * 教师业务档案Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizTeacherProfileServiceImpl implements IBizTeacherProfileService 
{
    @Autowired
    private BizTeacherProfileMapper bizTeacherProfileMapper;

    /**
     * 查询教师业务档案
     * 
     * @param userId 教师业务档案主键
     * @return 教师业务档案
     */
    @Override
    public BizTeacherProfile selectBizTeacherProfileByUserId(Long userId)
    {
        return bizTeacherProfileMapper.selectBizTeacherProfileByUserId(userId);
    }

    /**
     * 查询教师业务档案列表
     * 
     * @param bizTeacherProfile 教师业务档案
     * @return 教师业务档案
     */
    @Override
    public List<BizTeacherProfile> selectBizTeacherProfileList(BizTeacherProfile bizTeacherProfile)
    {
        return bizTeacherProfileMapper.selectBizTeacherProfileList(bizTeacherProfile);
    }

    /**
     * 新增教师业务档案
     * 
     * @param bizTeacherProfile 教师业务档案
     * @return 结果
     */
    @Override
    public int insertBizTeacherProfile(BizTeacherProfile bizTeacherProfile)
    {
        bizTeacherProfile.setCreateTime(DateUtils.getNowDate());
        return bizTeacherProfileMapper.insertBizTeacherProfile(bizTeacherProfile);
    }

    /**
     * 修改教师业务档案
     * 
     * @param bizTeacherProfile 教师业务档案
     * @return 结果
     */
    @Override
    public int updateBizTeacherProfile(BizTeacherProfile bizTeacherProfile)
    {
        bizTeacherProfile.setUpdateTime(DateUtils.getNowDate());
        return bizTeacherProfileMapper.updateBizTeacherProfile(bizTeacherProfile);
    }

    /**
     * 批量删除教师业务档案
     * 
     * @param userIds 需要删除的教师业务档案主键
     * @return 结果
     */
    @Override
    public int deleteBizTeacherProfileByUserIds(Long[] userIds)
    {
        return bizTeacherProfileMapper.deleteBizTeacherProfileByUserIds(userIds);
    }

    /**
     * 删除教师业务档案信息
     * 
     * @param userId 教师业务档案主键
     * @return 结果
     */
    @Override
    public int deleteBizTeacherProfileByUserId(Long userId)
    {
        return bizTeacherProfileMapper.deleteBizTeacherProfileByUserId(userId);
    }
}
