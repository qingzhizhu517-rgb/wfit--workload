package com.workload.system.calc.rule;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import com.workload.common.exception.ServiceException;

/** DATE 有效区间包含首尾日；同一最近生效日出现多个有效版本时拒绝歧义。 */
public final class EffectiveDateResolver
{
    private EffectiveDateResolver() { }

    public static <T> T select(List<T> rows, LocalDate asOfDate,
            Function<T, LocalDate> from, Function<T, LocalDate> to, String label)
    {
        Objects.requireNonNull(asOfDate, "asOfDate");
        Comparator<LocalDate> order = Comparator.nullsFirst(Comparator.naturalOrder());
        T selected = null;
        for (T row : rows)
        {
            LocalDate start = from.apply(row);
            LocalDate end = to.apply(row);
            if ((start != null && start.isAfter(asOfDate)) || (end != null && end.isBefore(asOfDate))) continue;
            if (selected == null || order.compare(start, from.apply(selected)) > 0) selected = row;
        }
        if (selected != null)
        {
            LocalDate selectedStart = from.apply(selected);
            long count = rows.stream().filter(row -> Objects.equals(from.apply(row), selectedStart))
                    .filter(row -> to.apply(row) == null || !to.apply(row).isBefore(asOfDate)).count();
            if (count > 1) throw new ServiceException("生效版本冲突: " + label + "，日期 " + asOfDate);
        }
        return selected;
    }
}
