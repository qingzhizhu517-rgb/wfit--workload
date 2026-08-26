# 潍理工教学工作量智能化管理系统（WFIT Workload）

> 潍坊理工学院教学工作量核算、汇总、绩效酬金计算管理系统

## 项目简介

本系统替代教师手动汇总、核对、填写工作量的传统模式，实现教务处→学院→教师的全流程数字化管理。支持 G1-G11 共 11 类工作量的自动核算、学期汇总、绩效酬金计算、三级审批和报表导出。

## 核心功能

### 工作量核算（G1-G11）

| 类型 | 说明 | 计算方式 |
|------|------|----------|
| G1 | 理论课 | `J1 × C1 × K1 × Q1 × Q2 × Q3 × N` |
| G2 | 课内实践/实验/实训 | `J2 × K × C2 × Q1 × Q2 × Q3` |
| G3 | 教学实习/实训 | `T × D × K × Q1 × Q2 × Q3` |
| G4 | 课程设计 | `J4 × min(R4,60) × 0.4` |
| G5 | 毕业论文(设计) | `R5 × K5` |
| G6 | 集中实习(现场跟班) | `W × min(R6,20) × 0.4` |
| G7 | 第一课堂工作量合计 | G1+G2+G3+G4+G5+G6 |
| G8 | 第二课堂工作量 | 手动录入 |
| G9 | 其他工作量 | 手动录入 |
| G10 | 教学工作量合计 | G7+G8+G9 |
| G11 | 管理服务工作量 | 按岗位标准 × 任职天数/学期天数 |

### 绩效酬金

```
课时酬金 = (min(总工作量, 540) - 180) × 职称单位酬金
其他酬金 = A(重修辅导) + B(毕业实习) + C(论文重修) + D(代阅卷) + E(讲座) + F(运动会裁判) + G(夜间值班)
总计 = 课时酬金 + 其他酬金
```

| 职称 | 教授 | 副教授 | 讲师 | 助教 |
|------|------|--------|------|------|
| 单位酬金 | 70元 | 60元 | 50元 | 40元 |

### 其他功能

- **Excel 导入**：教务员上传排课 Excel → 自动匹配策略引擎计算 → 落库
- **教师自主申报**：教师登录后可填报 G8/G9/G11 附加工作量
- **三级审批**：填报中 → 教务助理待审 → 院领导待签 → 已完结
- **报表导出**：附件1（个人工作量明细表）、附件2（绩效酬金统计表）
- **仪表盘**：管理员大屏（统计+ECharts）、教师工作台（数据卡+达标面板）

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 4.0.7 / Java 17 / Maven 多模块 / MyBatis / Druid |
| 安全 | Spring Security + JWT |
| 前端 | Vue 3.5 + Vite 6 + Element Plus 2.13 + Pinia 3 |
| 数据库 | MySQL 8（库名 `wflg_workload`） |
| 缓存 | Redis |
| Excel | Apache POI 5.5.1 |
| 基础框架 | RuoYi-Vue3 前后端分离框架 |

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8+
- Redis
- Node.js 18+

### 数据库初始化

> 仓库根目录即 `wfit--workload/`，**无** `WFIT_workload/` 外层目录。SQL 脚本须按顺序执行。

```bash
cd rear/sql/
# 基础框架表（先执行）
mysql -u root -p wflg_workload < ry_20260321.sql
mysql -u root -p wflg_workload < quartz.sql
# 业务表 + 种子 + 规则 + 测试数据 + 菜单
mysql -u root -p wflg_workload < 01_biz_schema.sql
mysql -u root -p wflg_workload < 02_biz_seed.sql
mysql -u root -p wflg_workload < 03_calc_rules.sql
mysql -u root -p wflg_workload < 04_biz_test_data.sql
mysql -u root -p wflg_workload < 05_biz_menu.sql
# 角色与四端测试账号
mysql -u root -p wflg_workload < 06_test_accounts.sql
# 必须：审计日志表 biz_audit_log 仅在此脚本创建（01 未包含）
mysql -u root -p wflg_workload < 08_review_fixes.sql
# 必须：撤销教务助理 unlock 越权 + 院领导补授驳回权限
mysql -u root -p wflg_workload < 13_fix_audit_perm.sql
# 可选：部门名称学院化（100→潍理工学院、103→信息工程学院、105→经济管理学院）
mysql -u root -p wflg_workload < 12_fix_dept_mapping.sql
```

**关于 07~15 补丁脚本**（新库执行完上面即可，无需再跑）：

| 脚本 | 状态 | 说明 |
|------|------|------|
| 07 / 09 / 10 / 11 / 14 | 已并入基础脚本 | 修复内容分别合并进 `04`/`05`/`01`/`02`，新库可跳过（幂等，跑了无害） |
| 15 | 已并入基础脚本 | 补 63 个明细/配置页按钮权限登记 + 清理死权限（合并进 `05`/`06`）；存量库执行 `15_fix_menu_buttons.sql` 即可 |
| 08 / 13 | **新库必须执行** | `biz_audit_log` 审计表唯一来源；修正 `06` 授予的教务 unlock 越权 |
| 12 | 可选 | 将 ry 默认部门（若依科技/研发部门/测试部门）改名为学院名称 |

注意：`03_calc_rules.sql` 使用 `INSERT INTO`（非幂等），重复执行会因唯一键冲突报错，仅执行一次。

### 启动后端

```bash
cd rear/
mvn clean package -DskipTests
java -jar workload-admin/target/workload-admin.jar
# 或
mvn spring-boot:run -pl workload-admin
```

后端启动后访问 http://localhost:8084/swagger-ui.html 查看 API 文档。

### 启动前端

```bash
cd front/RuoYi-Vue3/
npm install --registry=https://registry.npmmirror.com
npm run dev          # 开发服务器（端口 3000）
npm run build:prod   # 生产构建
npm run lint         # ESLint 检查（lint:fix 自动修复）
```

前端开发服务器默认运行在 http://localhost:3000，`/dev-api` 前缀请求代理到后端 `http://localhost:8084`（去掉前缀）。

### 测试账号

RuoYi 内置账号 `admin / admin123`。业务测试账号（密码均为 `123456`，来自 `06_test_accounts.sql`）：

| 账号 | 角色 | 说明 |
|------|------|------|
| `admin_test` | 管理员 | 全部权限 |
| `jiaowu_test` | 教务助理 | 审批权限 |
| `teacher_test` | 教师 | 仅看本人数据 |
| `leader_test` | 院领导 | 签字权限 |

## 项目结构

```
wfit--workload/                        # 仓库根目录（无 WFIT_workload/ 外层）
├── rear/                              # 后端（Spring Boot）
│   ├── workload-admin/                # 启动入口 + 系统管理
│   ├── workload-system/               # 业务核心
│   │   └── src/**/
│   │       ├── calc/                  # 计算引擎（策略模式）
│   │       │   ├── strategy/          # G1-G11 策略实现
│   │       │   ├── rule/              # 规则参数服务
│   │       │   └── allowance/         # 酬金计算
│   │       ├── controller/            # 22 个业务 Controller
│   │       ├── service/               # 业务 Service
│   │       ├── mapper/                # MyBatis Mapper
│   │       └── domain/                # 实体类
│   ├── workload-framework/            # 框架层
│   ├── workload-common/               # 公共工具
│   └── sql/                           # DDL + 种子数据 + 菜单
├── front/RuoYi-Vue3/                  # 前端（Vue 3）
│   └── src/
│       ├── api/system/                # 31 个 API 接口
│       ├── views/system/              # 19 个业务页面
│       └── views/dashboard/           # 仪表盘
├── else/                              # 需求文档、设计文档
└── docs/                              # 开发计划与规范
```

## 计算引擎架构

系统核心采用**策略模式 + Spring IOC 动态分发**，彻底消灭 if-else：

```
前端传入工作量类型 (G1/G2/...)
        ↓
DispatcherService
        ↓ 查询 category_dict.calc_strategy
WorkloadCalcStrategy 接口
        ↓ 动态调用
TheoryCalcStrategyImpl / PracticeCalcStrategyImpl / ...
        ↓ 读取规则参数
RuleParamService (Redis 缓存)
        ↓ 计算结果
落库 biz_workload_item + biz_wl_* 明细
```

## 文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 数据库设计规范 | `docs/superpowers/specs/` | 权威 v2 设计文档 |
| M1 实施计划 | `docs/superpowers/plans/2026-07-18-*` | 环境搭建与基建 |
| M3 实施计划 | `docs/superpowers/plans/2026-07-21-*` | 计算引擎子系统 |
| M4/M5 实施计划 | `else/实施计划-M4M5-2026-07-24.md` | 导入、申报、审批、导出 |
| 进度报告 | `else/进度报告-2026-07-24.md` | 截至 2026-07-24 的进度 |
| 业务需求 | `else/工作量.md` | G1-G11 公式与系数说明 |
| 系统设计 | `else/潍理工工作量管理系统设计new).md` | E-R 图、SOP、路线图 |

## 开发进度

| 里程碑 | 内容 | 状态 |
|--------|------|------|
| M1 | 环境搭建与基建 | ✅ 已完成 |
| M2 | 权限与前端骨架改造 | ✅ 已完成 |
| M3 | 核心策略引擎（G1-G11） | ✅ 已完成 |
| M4 | Excel 导入与教师申报 | ✅ 已完成 |
| M5 | 审批流与报表导出 | ✅ 已完成 |

## 许可证

本项目基于 RuoYi-Vue3 框架开发，仅供潍坊理工学院内部使用。
