# AGENTS.md

High-signal guide for AI agents working in this repo. See `CLAUDE.md` for full context.

## Project

University workload management system (WFIT) — calculates teaching workload (G1-G11), summaries, pay. Built on RuoYi-Vue3 framework. All M1-M5 milestones complete.

## Quick Reference

| What | Command |
|------|---------|
| Backend build | `cd rear && mvn clean package -DskipTests` |
| Backend run | `cd rear && mvn spring-boot:run -pl workload-admin` |
| Frontend dev | `cd front/RuoYi-Vue3 && npm run dev` |
| Frontend build | `cd front/RuoYi-Vue3 && npm run build:prod` |

## Architecture

```
rear/                           # Maven multi-module (Java 17, Spring Boot 4.0.7)
├── workload-admin/             # Entry point, port 8084
├── workload-system/            # ALL business logic lives here
│   └── src/**/calc/            # Calculation engine (strategy pattern)
├── workload-framework/         # Security, datasource, AOP
├── workload-common/            # Shared utils
└── sql/                        # DDL + seed + rules (execute in order 01→10)

front/RuoYi-Vue3/               # Vue 3.5 + Vite 6 + Element Plus
└── src/views/system/           # 19 business pages
```

## Non-Obvious Facts

- **DB host is `172.19.80.1`** (WSL bridge IP), not localhost. Config in `rear/workload-admin/src/main/resources/application-druid.yml`.
- **No automated tests exist.** `spring-boot-starter-test` is not in pom.xml. Verify changes via Swagger UI (`/swagger-ui.html`) or frontend.
- **Frontend proxy**: all `/dev-api` requests strip prefix and proxy to `http://localhost:8084` (see `vite.config.js`).
- **Calculation engine uses Spring bean names** — `biz_workload_category_dict.calc_strategy` column stores bean names like `theoryCalcStrategy`. `CalcStrategyFactory` resolves by bean name, not by class.
- **`manage` module is empty** — just a hello-world `Main.java`, no business logic.
- **`rear/workload-ui` (Vue 2) is abandoned** — active frontend is `front/RuoYi-Vue3`.
- **G8/G9 have no auto-calculation** — second classroom and other workload are manual entry only.
- **Semester format**: `2025-2026-1` (academic year + semester number). Calendar config in `application.yml` under `wl.semester`.

## RuoYi Conventions (Non-Standard)

- Controller returns `AjaxResult` or `TableDataInfo`; pagination via `startPage()` + `getDataTable()`
- Service: interface `IXxxService` + impl `XxxServiceImpl`; `@Transactional` on impl only
- Permissions: `@PreAuthorize("@ss.hasPermi('system:xxx:list')")` — format is `module:entity:operation`
- Entity: extends `BaseEntity` (auto createBy/updateBy/createTime/updateTime)
- MyBatis XML maps in `resources/mapper/system/`

## SQL Execution Order

Must execute in this order (DB: `wflg_workload`):

```
ry_20260321.sql → quartz.sql → 01_biz_schema.sql → 02_biz_seed.sql → 03_calc_rules.sql → 04_biz_test_data.sql → 05_biz_menu.sql
```

Optional: `06-11` (test accounts, fixes, patches). `11_fix_duplicate_rules.sql` is idempotent — safe to re-run.

## Key Entry Points

| Component | File |
|-----------|------|
| App entry | `rear/workload-admin/src/main/java/com/workload/RuoYiApplication.java` |
| Calculation API | `rear/workload-system/**/controller/BizCalcController.java` |
| Strategy interface | `rear/workload-system/**/calc/WorkloadCalcStrategy.java` |
| Strategy factory | `rear/workload-system/**/calc/CalcStrategyFactory.java` |
| Rule params (Redis) | `rear/workload-system/**/calc/rule/RuleParamService.java` |
| Pay calculator | `rear/workload-system/**/calc/allowance/PayCalcService.java` |
| G11 generator | `rear/workload-system/**/calc/ManagementItemGenerator.java` |
| Summary service | `rear/workload-system/**/calc/SummaryCalcService.java` |

## Business Table Layering

- **Support**: `biz_teacher_profile`, `biz_workload_category_dict`, `biz_workload_rule`, `biz_pay_rate`, `biz_import_batch`
- **Source data**: `biz_teaching_task`, `biz_role_assignment`
- **Calc detail**: `biz_workload_item` + `biz_wl_*` (theory/practice/internship/courseDesign/thesis/concentratedInternship/management)
- **Summary**: `biz_workload_summary` (JSON `category_details` field for dynamic category aggregation)
- **Pay**: `biz_pay_record`, `biz_allowance_item`

## Approval State Machine

```
0 (draft) → 1 (assistant review) → 2 (leader sign) → 3 (locked)
                                                            ↑
-1 (rejected) → back to 0, can resubmit
```

## Environment

- Backend port: `8084`
- Frontend dev port: `3000`
- Redis: `localhost:6379`, no password
- DB password: `123456` (in `application-druid.yml`)
- Test accounts: `admin_test`, `jiaowu_test`, `teacher_test`, `leader_test` (all password `123456`)

## Files with Sensitive Config (git-tracked)

- `.env.development` — API base URL
- `rear/workload-admin/src/main/resources/application-druid.yml` — DB credentials
