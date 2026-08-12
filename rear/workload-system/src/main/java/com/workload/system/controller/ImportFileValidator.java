package com.workload.system.controller;

import org.springframework.web.multipart.MultipartFile;

/**
 * Excel 导入上传文件安全校验工具
 * <p>
 * 三重校验：空文件拒绝、扩展名白名单（仅 .xls/.xlsx，大小写不敏感）、文件大小上限。
 * 供各导入入口 Controller 复用，不涉及 Excel 解析逻辑。
 *
 * @author wflg
 */
public final class ImportFileValidator
{
    private ImportFileValidator()
    {
    }

    /**
     * 校验上传的 Excel 文件
     *
     * @param file      上传文件
     * @param maxSizeMb 文件大小上限（单位 MB）
     * @return 校验通过返回 null；校验失败返回中文错误提示
     */
    public static String validateExcelFile(MultipartFile file, long maxSizeMb)
    {
        if (file == null || file.isEmpty())
        {
            return "上传文件不能为空，请选择有效的 Excel 文件";
        }
        String filename = file.getOriginalFilename();
        String lowerName = filename == null ? "" : filename.toLowerCase();
        if (!lowerName.endsWith(".xls") && !lowerName.endsWith(".xlsx"))
        {
            return "仅支持上传 .xls 或 .xlsx 格式的 Excel 文件";
        }
        long maxBytes = maxSizeMb * 1024L * 1024L;
        if (file.getSize() > maxBytes)
        {
            return "上传文件大小不能超过 " + maxSizeMb + "MB";
        }
        return null;
    }
}
