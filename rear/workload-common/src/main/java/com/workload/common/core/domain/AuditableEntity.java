package com.workload.common.core.domain;

import java.util.Date;

/** Only the four persistence audit fields; business event dates are deliberately excluded. */
public interface AuditableEntity {
    String getCreateBy();
    void setCreateBy(String value);
    Date getCreateTime();
    void setCreateTime(Date value);
    String getUpdateBy();
    void setUpdateBy(String value);
    Date getUpdateTime();
    void setUpdateTime(Date value);
}
