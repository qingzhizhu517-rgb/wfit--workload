package com.workload.system.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import com.workload.system.domain.BizWorkloadCalcSnapshot;

/** Independent pre-upgrade five/six-part algorithm; never calls production hash code. */
public final class SnapshotHashTestFixture
{
    public static String legacyHash(BizWorkloadCalcSnapshot snapshot)
    {
        try
        {
            String payload = snapshot.getFactorJson() + "\n" + snapshot.getSourceJson() + "\n"
                    + (snapshot.getFormulaExpression() == null ? "" : snapshot.getFormulaExpression()) + "\n"
                    + (snapshot.getRuleVersion() == null ? "" : snapshot.getRuleVersion()) + "\n"
                    + snapshot.getResult().toPlainString()
                    + (snapshot.getContextJson() == null ? "" : "\n" + snapshot.getContextJson());
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e) { throw new AssertionError(e); }
    }
}
