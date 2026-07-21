package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.PayCalcService;
import com.workload.system.calc.allowance.AllowanceCalcStrategy;
import com.workload.system.calc.allowance.AllowanceStrategyFactory;
import com.workload.system.mapper.BizAllowanceItemMapper;
import com.workload.system.domain.BizAllowanceItem;
import com.workload.system.service.IBizAllowanceItemService;

/**
 * 其他酬金明细Service业务层处理
 *
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizAllowanceItemServiceImpl implements IBizAllowanceItemService
{
    @Autowired
    private BizAllowanceItemMapper bizAllowanceItemMapper;

    @Autowired
    private AllowanceStrategyFactory allowanceStrategyFactory;

    @Autowired
    private PayCalcService payCalcService;

    /**
     * 查询其他酬金明细
     *
     * @param id 其他酬金明细主键
     * @return 其他酬金明细
     */
    @Override
    public BizAllowanceItem selectBizAllowanceItemById(Long id)
    {
        return bizAllowanceItemMapper.selectBizAllowanceItemById(id);
    }

    /**
     * 查询其他酬金明细列表
     *
     * @param bizAllowanceItem 其他酬金明细
     * @return 其他酬金明细
     */
    @Override
    public List<BizAllowanceItem> selectBizAllowanceItemList(BizAllowanceItem bizAllowanceItem)
    {
        return bizAllowanceItemMapper.selectBizAllowanceItemList(bizAllowanceItem);
    }

    /**
     * 新增其他酬金明细（自动按 fee_type 策略计算金额）
     *
     * @param bizAllowanceItem 其他酬金明细
     * @return 结果
     */
    @Override
    @Transactional
    public int insertBizAllowanceItem(BizAllowanceItem bizAllowanceItem)
    {
        payCalcService.assertAllowanceEditable(bizAllowanceItem.getUserId(), bizAllowanceItem.getSemester());
        recalcAmount(bizAllowanceItem);
        bizAllowanceItem.setCreateTime(DateUtils.getNowDate());
        return bizAllowanceItemMapper.insertBizAllowanceItem(bizAllowanceItem);
    }

    /**
     * 修改其他酬金明细（自动按 fee_type 策略重算金额）
     *
     * @param bizAllowanceItem 其他酬金明细
     * @return 结果
     */
    @Override
    @Transactional
    public int updateBizAllowanceItem(BizAllowanceItem bizAllowanceItem)
    {
        payCalcService.assertAllowanceEditable(bizAllowanceItem.getUserId(), bizAllowanceItem.getSemester());
        recalcAmount(bizAllowanceItem);
        bizAllowanceItem.setUpdateTime(DateUtils.getNowDate());
        return bizAllowanceItemMapper.updateBizAllowanceItem(bizAllowanceItem);
    }

    /**
     * 批量删除其他酬金明细
     *
     * @param ids 需要删除的其他酬金明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizAllowanceItemByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            BizAllowanceItem old = bizAllowanceItemMapper.selectBizAllowanceItemById(id);
            if (old != null)
            {
                payCalcService.assertAllowanceEditable(old.getUserId(), old.getSemester());
            }
        }
        return bizAllowanceItemMapper.deleteBizAllowanceItemByIds(ids);
    }

    /**
     * 删除其他酬金明细信息
     *
     * @param id 其他酬金明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizAllowanceItemById(Long id)
    {
        BizAllowanceItem old = bizAllowanceItemMapper.selectBizAllowanceItemById(id);
        if (old != null)
        {
            payCalcService.assertAllowanceEditable(old.getUserId(), old.getSemester());
        }
        return bizAllowanceItemMapper.deleteBizAllowanceItemById(id);
    }

    /**
     * 按 fee_type 策略计算金额（D 代阅卷首期未启用；A 跟班/单独开班保留手工金额）
     */
    private void recalcAmount(BizAllowanceItem item)
    {
        AllowanceCalcStrategy strategy = allowanceStrategyFactory.get(item.getFeeType());
        if (strategy == null)
        {
            throw new ServiceException("酬金类型未启用: " + item.getFeeType() + "（D代阅卷待正式文件）");
        }
        item.setAmount(strategy.calculate(item));
    }
}
