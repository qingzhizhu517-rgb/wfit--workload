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
└── sql/                        # DDL + seed + rules (fresh DB: 01→06 + 08 + 13)

front/RuoYi-Vue3/               # Vue 3.5 + Vite 6 + Element Plus
└── src/views/system/           # 19 business pages
```

## Non-Obvious Facts

- **Credentials come from environment variables.** `WFIT_DB_PASSWORD`, `WFIT_DRUID_PASSWORD`, and `WFIT_TOKEN_SECRET` have no defaults — the backend fails to start if they are unset. Copy `.env.example` to `.env` (gitignored) and fill them in, or inject via IDEA run configuration. DB host defaults to `127.0.0.1`; the `172.19.80.1` in older docs is a retired WSL bridge IP.
- **Limited automated tests.** Only `workload-system` has JUnit 5 tests (`CalcStrategyFactoryTest`, `StrategyCacheTest` in `src/test/**/calc/strategy/`); run via `mvn test -pl workload-system`. No `spring-boot-starter-test` / integration tests. Verify other changes via Swagger UI (`/swagger-ui.html`) or frontend.
- **Frontend proxy**: all `/dev-api` requests strip prefix and proxy to `http://localhost:8084` (see `vite.config.js`).
- **Calculation engine uses Spring bean names** — `biz_workload_category_dict.calc_strategy` column stores bean names like `theoryCalcStrategy`. `CalcStrategyFactory` resolves by bean name, not by class.
- **`manage` module is empty** — just a hello-world `Main.java`, no business logic.
- **Vue 2 `workload-ui` has been removed** - the only frontend is `front/RuoYi-Vue3` (Vue 3).
- **G8/G9 have no auto-calculation** — second classroom and other workload are manual entry only.
- **Semester format**: `2025-2026-1` (academic year + semester number). Calendar config in `application.yml` under `wl.semester`.

## RuoYi Conventions (Non-Standard)

- Controller returns `AjaxResult` or `TableDataInfo`; pagination via `startPage()` + `getDataTable()`
- Service: interface `IXxxService` + impl `XxxServiceImpl`; `@Transactional` on impl only
- Permissions: `@PreAuthorize("@ss.hasPermi('system:xxx:list')")` — format is `module:entity:operation`
- Entity: extends `BaseEntity` (auto createBy/updateBy/createTime/updateTime)
- MyBatis XML maps in `resources/mapper/system/`

## SQL Execution Order

Fresh database (DB: `wflg_workload`) — execute in order:

```
ry_20260321.sql → quartz.sql → 01_biz_schema.sql → 02_biz_seed.sql → 03_calc_rules.sql → 04_biz_test_data.sql → 05_biz_menu.sql → 06_test_accounts.sql → 08_review_fixes.sql → 13_fix_audit_perm.sql
```

- `08` is mandatory: `biz_audit_log` (19th table) is created ONLY here — `01_biz_schema.sql` does not include it.
- `13` is mandatory: revokes the (role3, 20209 unlock) over-privilege granted by `06`, and grants leader reject (20207).
- `07`/`09`/`10`/`11`/`14` are already merged into `04`/`05`/`01`/`02` respectively — skip on a fresh DB (all idempotent, harmless if run).
- `12` is optional: renames dept 100/103/105 to school names (ry defaults are 若依科技/研发部门/测试部门).
- `03` is NOT idempotent (plain `INSERT INTO`) — run exactly once.

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
- Redis: `WFIT_REDIS_HOST` (default `localhost`) : `WFIT_REDIS_PORT` (default 6379), no password locally
- DB credentials: from `WFIT_DB_USER` / `WFIT_DB_PASSWORD` (see `.env.example`)
- Test accounts: `admin_test`, `jiaowu_test`, `teacher_test`, `leader_test` (all password `123456`) — local development only; change or remove before any shared deployment

## Secrets Handling

- All credentials are injected via environment variables; see `.env.example` (tracked) and `.env` (gitignored).
- `.mcp.json` holds a local MySQL password and is gitignored — do not commit it.
- ⚠️ Historical leak: the DB password, Druid console password, and JWT secret were committed in plaintext in earlier commits. The working tree is clean now, but old commits remain reachable. The real remediation is **rotating those credentials**, not editing the files.
- `front/RuoYi-Vue3/.env.development` is tracked but contains only the page title and `/dev-api` prefix — no secrets.
