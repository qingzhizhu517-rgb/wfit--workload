package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizTeacherProfile;

/**
 * 教师业务档案Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizTeacherProfileService 
{
    /**
     * 查询教师业务档案
     * 
     * @param userId 教师业务档案主键
     * @return 教师业务档案
     */
    public BizTeacherProfile selectBizTeacherProfileByUserId(Long userId);

    /**
     * 查询教师业务档案列表
     * 
     * @param bizTeacherProfile 教师业务档案
     * @return 教师业务档案集合
     */
    public List<BizTeacherProfile> selectBizTeacherProfileList(BizTeacherProfile bizTeacherProfile);

    /**
     * 新增教师业务档案
     * 
     * @param bizTeacherProfile 教师业务档案
     * @return 结果
     */
    public int insertBizTeacherProfile(BizTeacherProfile bizTeacherProfile);

    /**
     * 修改教师业务档案
     * 
     * @param bizTeacherProfile 教师业务档案
     * @return 结果
     */
    public int updateBizTeacherProfile(BizTeacherProfile bizTeacherProfile);

    /**
     * 批量删除教师业务档案
     * 
     * @param userIds 需要删除的教师业务档案主键集合
     * @return 结果
     */
    public int deleteBizTeacherProfileByUserIds(Long[] userIds);

    /**
     * 删除教师业务档案信息
     * 
     * @param userId 教师业务档案主键
     * @return 结果
     */
    public int deleteBizTeacherProfileByUserId(Long userId);
}
