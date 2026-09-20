package com.workload.system.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

import com.workload.common.exception.ServiceException;

/**
 * 导入确认令牌哈希器。
 *
 * <p>预检阶段生成一次性明文令牌返回前端，服务端只落其 SHA-256 十六进制哈希
 * （{@code biz_import_batch.confirm_token_hash}）。确认阶段用 {@link #matches} 常量时间比对，
 * 明文令牌永不入库、不落日志。</p>
 *
 * @author wflg
 */
@Component
public class ImportTokenHasher
{
    /** 生成明文令牌的 SHA-256 十六进制哈希。 */
    public String hash(String token)
    {
        if (token == null)
        {
            return null;
        }
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes)
            {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new ServiceException("生成导入确认令牌哈希失败：" + e.getMessage());
        }
    }

    /** 明文令牌是否匹配存储的哈希（常量时间比对，避免时序侧信道）。 */
    public boolean matches(String token, String expectedHash)
    {
        if (token == null || expectedHash == null)
        {
            return false;
        }
        String actual = hash(token);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
