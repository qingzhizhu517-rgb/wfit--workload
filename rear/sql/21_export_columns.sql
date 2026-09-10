-- ============================================================
-- 21_export_columns.sql —— 附件1「-新」39 列导出所需补字段
-- （Phase 2 前置：Task 5 说明列 / Task 6 学科门类 / Task 7 重修标志）
--
-- 新库路径：01_biz_schema.sql 已含这些列，无需执行本文件。
-- 本文件仅用于既有库升级（ALTER 非幂等，重复执行会报 Duplicate column）。
--
-- 依据：
-- - 附件1 AF「其他工作量说明」/ AI「管理服务工作量说明」→ 汇总两说明列
-- - 附件1 X/Y「文科/理工科」双列 → G5 学科门类（艺术类按文史，同 K5 归档）
-- - 用户拍板：专升本归本科；重修另设标志不占层次列（D 列）
-- ============================================================

-- Task 5：汇总表两个说明列（G7 列建表已有，无需补）
ALTER TABLE biz_workload_summary
  ADD COLUMN g8_remark  VARCHAR(255) DEFAULT NULL COMMENT '其他工作量说明（附件1 AF 列）',
  ADD COLUMN g11_remark VARCHAR(255) DEFAULT NULL COMMENT '管理服务工作量说明（附件1 AI 列）';

-- Task 6：G5 学科门类（附件1 X/Y 双列分流）
ALTER TABLE biz_wl_thesis
  ADD COLUMN discipline_category VARCHAR(20) DEFAULT NULL
    COMMENT '学科门类 SCITECH/LIBERAL_ARTS，附件1 X/Y 双列分列用；艺术类按文史';

-- 存量回填：按 K5 反推（9/5=理工，6/4=文史），NULL 时导出亦用此兜底
UPDATE biz_wl_thesis
   SET discipline_category = CASE WHEN K5 IN (9, 5) THEN 'SCITECH' ELSE 'LIBERAL_ARTS' END
 WHERE discipline_category IS NULL;

-- Task 7：教学任务重修标志（不占层次列）
ALTER TABLE biz_teaching_task
  ADD COLUMN is_retake TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '重修标志（不占层次列；课程名含「重修」时置1）';

-- 存量回填：按课程名含「重修」推导
UPDATE biz_teaching_task
   SET is_retake = 1
 WHERE course_name LIKE '%重修%';
