-- ============================================================
-- 潍理工教师工作量管理系统 种子数据
-- G 类别字典 11 条 / 全局规则参数 28 条 / 职称酬金费率 4 条
-- ============================================================

-- G 类别字典（使用 INSERT IGNORE 确保幂等性）
INSERT IGNORE INTO biz_workload_category_dict (type_code, type_name, parent_group, calc_strategy, is_calc_excess, sort_order) VALUES
('G1','理论课','TEACHING','theoryCalcStrategy',1,1),
('G2','课内实践/实验/实训','TEACHING','practiceCalcStrategy',1,2),
('G3','教学实习/实训','TEACHING','internshipTrainingCalcStrategy',1,3),
('G4','课程设计','TEACHING','courseDesignCalcStrategy',1,4),
('G5','毕业论文(设计)','TEACHING','thesisCalcStrategy',1,5),
('G6','集中实习(现场跟班)','TEACHING','concentratedInternshipCalcStrategy',1,6),
('G7','第一课堂工作量','TEACHING',NULL,1,7),
('G8','第二课堂工作量','EXTRA',NULL,1,8),
('G9','其他工作量','EXTRA',NULL,1,9),
('G10','教学工作量合计','TEACHING',NULL,1,10),
('G11','管理服务工作量','ADMIN','managementCalcStrategy',1,11);

-- 全局规则参数(系数档位/常数)
-- 使用 INSERT IGNORE + effective_from 确保幂等性（唯一约束 uk_rule_code_eff 生效）
INSERT IGNORE INTO biz_workload_rule (rule_code, rule_value, rule_desc, effective_from) VALUES
('COEF_CLASS_120_150', 1.10, '合堂120-150人系数N', '2025-09-01'),
('COEF_CLASS_151_UP',  1.20, '合堂151人及以上系数N', '2025-09-01'),
('COEF_REPEAT_1ST',    1.00, '理论课第一次C1', '2025-09-01'),
('COEF_REPEAT_2ND',    0.90, '理论课第二次C1/C2', '2025-09-01'),
('COEF_REPEAT_3RD_UP', 0.80, '理论课第三次及以后C1', '2025-09-01'),
('COEF_PRACTICE_LG',   1.00, '实践课理工类系数K', '2025-09-01'),
('COEF_PRACTICE_OTHER',0.90, '实践课其他专业系数K', '2025-09-01'),
('COEF_TRAIN_D_LG',    4.00, '实习实训指导系数D理工类', '2025-09-01'),
('COEF_TRAIN_D_ART',   3.00, '实习实训指导系数D艺术类', '2025-09-01'),
('COEF_TRAIN_D_HUM',   2.00, '实习实训指导系数D文史类', '2025-09-01'),
('COEF_TRAIN_D_UNIT',  2.00, '实习实训单位指导系数D', '2025-09-01'),
('COEF_THESIS_K5_LG_B',9.00, '毕业论文K5理工类本科', '2025-09-01'),
('COEF_THESIS_K5_LG_C',5.00, '毕业论文K5理工类专科', '2025-09-01'),
('COEF_THESIS_K5_HU_B',6.00, '毕业论文K5文史类本科', '2025-09-01'),
('COEF_THESIS_K5_HU_C',4.00, '毕业论文K5文史类专科', '2025-09-01'),
('CONST_COURSE_DESIGN',0.40, '课程设计/集中实习常数', '2025-09-01'),
('CAP_R4_MAX',         60.00,'课程设计指导人数上限', '2025-09-01'),
('CAP_R6_MAX',         20.00,'集中实习指导人数上限', '2025-09-01'),
('CAP_R5_BACHELOR',    10.00,'毕业论文本科人数上限', '2025-09-01'),
('CAP_R5_JUNIOR',      15.00,'毕业论文专科人数上限', '2025-09-01'),
('RATED_WORKLOAD',     180.00,'学期绩效额定', '2025-09-01'),
('CAP_200PCT',         540.00,'200%封顶阈值(180*3)', '2025-09-01'),
('BASIC_TEACH_PROF',   128.00,'达标基本教学量教授/年', '2025-09-01'),
('BASIC_TEACH_APROF',  240.00,'达标基本教学量副教授/年', '2025-09-01'),
('BASIC_TEACH_LECT',   240.00,'达标基本教学量讲师/年', '2025-09-01'),
('BASIC_TEACH_ASSIST', 192.00,'达标基本教学量助教/年', '2025-09-01'),
('FACTOR_MATERNITY',   0.50, '产假达标折算系数', '2025-09-01'),
('BASIC_TEACH_PHD',    128.00,'在职读博达标基本教学量/年', '2025-09-01');

-- 职称单位酬金费率（使用 INSERT IGNORE 确保幂等性）
INSERT IGNORE INTO biz_pay_rate (title, rate, effective_from) VALUES
('教授',   70.00, '2025-09-01'),
('副教授', 60.00, '2025-09-01'),
('讲师',   50.00, '2025-09-01'),
('助教',   40.00, '2025-09-01');
