package com.workload.system.calc;

import java.time.LocalDate;
import java.time.MonthDay;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;

/**
 * 学期校历（学期字符串 -> 日期区间），默认 秋 09-01~01-31 / 春 02-20~07-15，
 * 可在 application.yml 用 wl.semester.* 覆盖
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component
@ConfigurationProperties(prefix = "wl.semester")
public class SemesterCalendar
{
    /** 秋季学期开学（MM-dd） */
    private String autumnStart = "09-01";

    /** 秋季学期结束（MM-dd） */
    private String autumnEnd = "01-31";

    /** 春季学期开学（MM-dd） */
    private String springStart = "02-20";

    /** 春季学期结束（MM-dd） */
    private String springEnd = "07-15";

    /**
     * 解析学期（如 2025-2026-1）为日期区间
     *
     * @param semester 学期字符串
     * @return [学期起, 学期止]
     */
    public LocalDate[] rangeOf(String semester)
    {
        try
        {
            String[] parts = semester.split("-");
            int yearStart = Integer.parseInt(parts[0]);
            int yearEnd = Integer.parseInt(parts[1]);
            int term = Integer.parseInt(parts[2]);
            if (term == 1)
            {
                return new LocalDate[] { MonthDay.parse("--" + autumnStart).atYear(yearStart),
                        MonthDay.parse("--" + autumnEnd).atYear(yearEnd) };
            }
            return new LocalDate[] { MonthDay.parse("--" + springStart).atYear(yearEnd),
                    MonthDay.parse("--" + springEnd).atYear(yearEnd) };
        }
        catch (Exception e)
        {
            throw new ServiceException("学期格式无法解析: " + semester + "（期望 2025-2026-1）");
        }
    }

    public String getAutumnStart()
    {
        return autumnStart;
    }

    public void setAutumnStart(String autumnStart)
    {
        this.autumnStart = autumnStart;
    }

    public String getAutumnEnd()
    {
        return autumnEnd;
    }

    public void setAutumnEnd(String autumnEnd)
    {
        this.autumnEnd = autumnEnd;
    }

    public String getSpringStart()
    {
        return springStart;
    }

    public void setSpringStart(String springStart)
    {
        this.springStart = springStart;
    }

    public String getSpringEnd()
    {
        return springEnd;
    }

    public void setSpringEnd(String springEnd)
    {
        this.springEnd = springEnd;
    }
}
