# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

潍理工教学工作量智能化管理系统（WFIT Workload），基于 RuoYi-Vue 前后端分离框架定制开发，用于高校教师教学工作量核算、汇总、绩效酬金计算。

## 技术栈

- **后端**: Spring Boot 4.0.7 / Java 17 / Maven 多模块 / MyBatis / Druid / Spring Security + JWT
- **前端**: Vue 3.5 + Vite 6 + Element Plus 2.13 + Pinia 3 + Vue Router 4
- **数据库**: MySQL 8 (库名 `wflg_workload`)，Redis (缓存 + 会话)
- **API 文档**: Springdoc OpenAPI (Swagger UI at `/swagger-ui.html`)

## 目录结构

```
wfit/
├── WFIT_workload/
│   ├── rear/                          # 后端 Maven 多模块
│   │   ├── workload-admin/            # 启动入口 + 系统管理 Controller (端口 8084)
│   │   ├── workload-system/           # 业务核心：工作量核算、教学任务、教师档案等
│   │   │   ├── src/**/calc/           # 核算引擎 (WorkloadCalcService, PayCalcService, SummaryCalcService)
│   │   │   ├── src/**/controller/     # 业务 Controller (Biz*Controller)
│   │   │   ├── src/**/service/        # 业务 Service
│   │   │   ├── src/**/mapper/         # MyBatis Mapper 接口
│   │   │   └── src/**/domain/         # 实体类
│   │   ├── workload-framework/        # 框架层：Security、数据源、AOP、配置
│   │   ├── workload-common/           # 通用工具、注解、异常处理
│   │   ├── workload-quartz/           # 定时任务模块
│   │   ├── workload-generator/        # 代码生成器
│   │   ├── manage/                    # 辅助模块 (暂仅含 Main.java)
│   │   └── sql/                       # 建表 + 种子数据 + 计算规则
│   │       ├── 01_biz_schema.sql      # 业务表结构
│   │       ├── 02_biz_seed.sql        # 基础数据
│   │       ├── 03_calc_rules.sql      # 核算规则配置
│   │       └── 04_biz_test_data.sql   # 测试数据
│   └── front/RuoYi-Vue3/             # 前端 Vue 3 项目
│       └── src/
│           ├── api/system/            # 业务 API (calc, teachingTask, workloadSummary 等)
│           ├── views/system/          # 业务页面 (teachingTask, wlTheory, workloadSummary 等)
│           └── views/dashboard/       # 仪表盘 (AdminDashboard / TeacherDashboard)
├── else/                              # 需求文档、数据库设计、管理办法
└── docs/                              # 其他文档
```

## 常用命令

### 后端 (在 `WFIT_workload/rear/` 目录下)

```bash
# 编译打包 (跳过测试)
mvn clean package -DskipTests

# 仅编译指定模块
mvn clean compile -pl workload-system -am

# 运行 (主入口 workload-admin)
java -jar workload-admin/target/workload-admin.jar

# 或直接 Maven 启动
mvn spring-boot:run -pl workload-admin
```

### 前端 (在 `WFIT_workload/front/RuoYi-Vue3/` 目录下)

```bash
# 安装依赖
npm install --registry=https://registry.npmmirror.com

# 开发服务器 (默认 http://localhost:80)
npm run dev

# 生产构建
npm run build:prod
```

### 数据库初始化

```bash
# 按顺序执行 SQL
mysql -u root -p wflg_workload < rear/sql/01_biz_schema.sql
mysql -u root -p wflg_workload < rear/sql/02_biz_seed.sql
mysql -u root -p wflg_workload < rear/sql/03_calc_rules.sql
mysql -u root -p wflg_workload < rear/sql/04_biz_test_data.sql
```

## 配置要点

- 后端端口: `8084` (application.yml)
- 数据库: `127.0.0.1:3306/wflg_workload`，用户 `root`，密码 `123456` (application-druid.yml)
- Redis: `localhost:6379`，无密码
- 文件上传路径: `rear/uploadPath/`
- 前端 API 代理: 开发环境默认代理到 `http://localhost:8084`

## 业务核心：工作量核算公式

系统核心是按课程类型计算教学工作量，公式定义在 `工作量.md` 和 `03_calc_rules.sql` 中：

| 类型 | 公式 | 说明 |
|------|------|------|
| 理论课 G1 | `J1 * C1 * K1 * Q1 * Q2 * N` | J1=计划学时, C1=重复系数, K1=课程类型系数, Q1/Q2=质量系数, N=合堂系数 |
| 实践课 G2 | `J2 * K * C2 * Q1 * Q2` | J2=实践学时, K=课程系数, C2=重复系数 |
| 实习实训 G3 | `T * D * K * Q1 * Q2` | T=实际天数(×8学时), D=指导系数, K=重复系数 |
| 课程设计 G4 | `J4 * R4 * 0.4` | J4=学分, R4=人数 |
| 毕业论文 G5 | 按职称和人数分段计算 | |
| 管理岗 G6 | 按职务等级折算学时 | |

核算引擎位于 `workload-system/src/**/calc/`，由 `BizCalcController` 触发。

## RuoYi 框架约定

本项目继承 RuoYi-Vue 框架的代码规范：

- **Controller**: 返回 `AjaxResult` 或 `TableDataInfo`，分页用 `startPage()` + `getDataTable()`
- **Service**: 接口 `IXxxService` + 实现 `XxxServiceImpl`，事务注解在实现类
- **Mapper**: 接口 + XML 映射文件，XML 在 `resources/mapper/` 下
- **实体**: 继承 `BaseEntity`（含 createBy/createTime/updateBy/updateTime/remark）
- **权限**: 注解 `@PreAuthorize("@ss.hasPermi('system:xxx:list')")`，权限标识 `模块:实体:操作`
- **日志**: `@Log(title = "xxx", businessType = BusinessType.INSERT)` 操作日志注解
- **导入导出**: 实体字段加 `@Excel` 注解，配合 EasyExcel

## 注意事项

- 当前存在两套前端（Vue2 在 `rear/workload-ui` 已弃用，Vue3 在 `front/RuoYi-Vue3` 为活跃版本）
- 学期校历配置在 `application.yml` 的 `wl.semester` 节点，用于任职折算和特殊状态判定
- 业务表前缀 `biz_`，系统表前缀 `sys_`（RuoYi 内置）
- `.env.*` 文件包含敏感配置，已被 git 跟踪需注意
