# 数据库迁移清单（更新2026-11-06）

编号不是执行记录。部署前按结构、索引和权限确认状态，不能以“已到28”推断兼容。现已在MySQL9.0.1与8.4.11完成隔离结构/恢复与应用启动验证；不代表其它8.0小版本或正式上线已验收。

## 本次存量库升级

前置：业务/菜单表、23快照版本结构、24系数申请表已存在。本次恢复副本依次执行：

| 文件 | 前置 | 后置契约 |
|---|---|---|
| 28_import_framework.sql | import_batch、teaching_task | import_row，7个批次列，5个任务数量列，普通来源索引；移除历史唯一来源索引 |
| 30_snapshot_context.sql | 23快照表 | 可空 LONGTEXT context_json；旧快照不回填 |
| 31_item_review_permission.sql | 业务菜单2010及角色 | 独立核对按钮；按角色key授管理/教务，不授教师 |
| 32_workload_consistency.sql | summary及现行状态0/1/2 | 教师学期互斥锁表；needs_recalculation非空默认1，旧草稿需重算，既有待审/完结保持原流程 |
| 33_factor_provenance.sql | workload_item、allowance_item | 两个可空LONGTEXT来源/实算证据列；旧行保持NULL，不回填历史 |
| 34_settlement_snapshot.sql | summary、pay_record及33来源结构 | 只追加结算快照表及唯一summary/version；两个可空pointer；不回填历史 |
| 35_workload_review_contract.sql | through34、biz_audit_log、业务菜单及角色 | batch/item_review/线下补差三表；summary版本0及可空批次/日期；audit_log仅三nullable列；六项隐藏F权限最小授予；不回填历史 |

2026-10-30，33在13306验证副本与QA克隆分别执行两轮通过，两业务表全部原列/行数哈希不变；新列类型/可空及只读检查器通过，见`docs/验收/33迁移演练证据.json`。B已完成来源链隔离验收，见`docs/验收/T05因子与其他酬金来源.md`。

2026-10-30，34在两个13306副本各执行两轮，原汇总/酬金全部原列内容hash不变，新表为空、指针可空、唯一索引和只读checker通过。见`docs/验收/34迁移演练证据.json`。C技术隔离验收已完成，详见`docs/验收/T05结算证据与资格.md`；正式启用学期仍空，非业务启用确认。

28/30/31重复执行两轮通过；32亦单独重复两轮验证，53→54表、原列数据保持一致（`docs/验收/32迁移演练证据.json`）。已有业务行保持不变，证据见 `docs/验收/数据库升级与恢复.md`。只读检查命令（五项 WFIT_DB_HOST/PORT/NAME/USER/PASSWORD 由环境注入）：

```sh
python3 rear/scripts/check-migrations.py --mysql /usr/local/mysql/bin/mysql
```

返回0为本次集成列/索引/核对及T06审核权限、CHECK执行状态满足；1为缺失或不符；2为配置/连接失败。不执行迁移，不证明全部历史业务种子正确。

## 编号与历史映射

- 远程28固定为导入框架。原本地28_snapshot_context等字节移为30_snapshot_context；原本地29_item_review_permission等字节移为31_item_review_permission。29作为历史编号保留，不复用。
- 已执行旧名字的环境按列与权限探测确认；内容哈希不变，不重写快照。
- 旧计划22_calculation_consistency实际为23，22_workload_requirements实际为28。旧审核中心计划的22未落地且冲突，已修订为35_workload_review_contract；不得再以旧名执行。

## 空库与历史脚本

空的专用开发库候选顺序：`ry_20260321 → quartz → 01 → 02 → 03 → 04 → 05 → 06 → 08 → 13 → 16 → 17 → 18 → 19 → 23 → 24 → 25 → 26 → 27 → 28 → 30 → 31 → 32 → 33 → 34 → 35`。2026-11-06在13306独立空库wfit_fresh_20261106按此顺序完整执行26个脚本通过，形成58表、216项只读契约检查通过；已验收748e3c2应用独立18085启动健康200并停止。另在独立13308 MySQL8.4.11复现26脚本/58表216检查，并从9副本逻辑恢复保全全部biz行hash，748e3c2启动健康200；MySQL8.0未验，不等于完整业务链验收。证据见`docs/验收/空库初始化MySQL9证据.json`、`docs/验收/空库初始化MySQL8与恢复证据.json`。该路径包含测试数据与测试账号，不直接用于共享部署。

| 脚本 | 使用边界与检查 |
|---|---|
| ry、quartz、01 | 含DROP，仅空库；核对sys/QRTZ/biz基础表。quartz模块已下线，表仅兼容历史 |
| 02、03 | 规则种子；03非幂等，只一次；核对rule_code、生效日、费率 |
| 04、06 | 本地测试样本与角色账号；不是正式人员初始化 |
| 05 | 重置业务菜单及授权，不在既有库盲目重放 |
| 08、13 | 审计日志表及旧权限修复；依赖05/06；13必须在08之后 |
| 07/09/10/11/14/15 | 相关修复已并04/05/01/02/05+06；存量库按缺项审阅，不整组重放 |
| 12 | 可选示例部门映射，不覆盖真实组织 |
| 16 | 已下线模块菜单清理，保留有效监控入口 |
| 17 | 旧审批3迁2；检查汇总状态和审批权限 |
| 18、19 | G5报批阈值与D费率；检查实际政策与规则启用状态 |
| 20 | 已废弃，不执行历史学年值转换 |
| 21 | 导出列非幂等ALTER；已并01，存在时跳过 |
| 22 | G11学期来源列及唯一键；已并01，不换算历史值 |
| 23 | 快照、item计算版本；依赖01；检查item/version唯一索引 |
| 24 | 系数申请状态机；依赖23；检查审批字段 |
| 25/26/27 | 分类导入、系数申请/列表菜单；依赖业务菜单及角色，按顺序 |
| 35 | fresh01已含新三表、summary版本及audit_log，05/06已含六权限；仍末尾执行35兼容旧fresh路径；禁止在存量重放01/05/06 |

应用回退保留新增表、可空列和新快照；不DROP新增数据。数据恢复须恢复到独立目标并核验，不能直接覆盖现有业务库。

## 当前脚本 SHA-256

哈希标识当前文件字节，不代表已执行；改动迁移后需复核并更新。旧脚本注释可能保留历史口径，以现行确认记录为准。

| 文件 | SHA-256 |
|---|---|
| `01_biz_schema.sql` | `b15f05d627e2d35c8d019cb44735bee7730877f821db624a9e9388901b744cb7` |
| `02_biz_seed.sql` | `25d3aff52f183a004e6b885d5662e0e188d93b1076aba8d85b4a3bcfc9ffafc7` |
| `03_calc_rules.sql` | `ca34a45bb1c42ff521f6a64ef927d50a878d32b1575982ed91bc6946bd8ef9ba` |
| `04_biz_test_data.sql` | `7a565a7d883ee9172628e1d3f17dd6737c327acd340ffc8bc64f4ebab30b8de7` |
| `05_biz_menu.sql` | `c0fad441abf1ea05770cb380ff79008224a7f8b8b4948ac647bf533724866ed3` |
| `06_test_accounts.sql` | `c78f2f1fe3d6cbba3a37f6fc2e369196502c57bbe0462f863c040d705e2947b1` |
| `07_fix_test_data.sql` | `e490b11719db21b9fb4b652a4776e9a53121af80b922c5c20f8fe613716b2bdc` |
| `08_review_fixes.sql` | `f224a175dc850cab453b70a14b4f656e5d1cb9423c6ac6d3ddb35f35d6bafdf1` |
| `09_dashboard_perm.sql` | `ae22d4df714d5fe1898f4fcb18001f0c5455f43e68b85ebdd94d04806deda0ab` |
| `10_item_role_type.sql` | `0b068ed9d51de0829ed13f64ab7823bc22adb85b8c65ba608c696efb4a45aebb` |
| `11_fix_duplicate_rules.sql` | `650b0ef14060fefed3cd7746291ac5667966ce24b2649d9b2cedb34f91d28ce5` |
| `12_fix_dept_mapping.sql` | `91880bee00b4333f99e76604ba85bbb795337f10b5c8eb4e1cfbaa8b05b1e88a` |
| `13_fix_audit_perm.sql` | `7469fac462a20983ff03b784538d27052995de75c8cb6ed21cc8225d47bc8f71` |
| `14_fix_calc_rules.sql` | `6bcd316d4f6cd163281f685f04fef7c5666bf3818f324ae2a9170dd422fa0b7a` |
| `15_fix_menu_buttons.sql` | `15c2d4bae1d247ae55808838dc937e902612e79925a4bc3b1ba3ce4d9cd3443a` |
| `16_remove_unused_modules.sql` | `6aa0bc3f7e4928002ef9f5316b1bc40dbaaecbafdc5bb2ecd9bd755f8d1f7da9` |
| `17_simplify_approval.sql` | `d9484da86d89c1ef451b81b289353f0dab42cacffcd28dbd0ed17906762e4667` |
| `18_fix_r5_approval.sql` | `2bb49fdd540a9ce4858e208b84ed5c669ff4ec664497c94abeb78e34612b9d56` |
| `19_enable_allowance_d.sql` | `1786aa76de648a96f6526af06f92389a186aa10e6f31312df05a80cb6692ca72` |
| `20_service_rate_semantics.sql` | `4c580ce0f46f2f9bbc36758957737a1cfab02fac9bdcc725889cf038c7396d2b` |
| `21_export_columns.sql` | `f7e042199baa68b9438084e4c34112191a1ce74d8ff05c6d6e9701a971c1e669` |
| `22_semester_position_workload.sql` | `9bfaf1e916dc083637cd4ea79ae9e74efeec1afb3e981be038bd024b09cf87b9` |
| `23_calculation_consistency.sql` | `5b5f943d6747087d7333a3cbb7cf29e4fe57739df350096418e1c3006b831ee7` |
| `24_coefficient_adjustment.sql` | `cdef871939d18aa7caf27c08ab6506132e92b38d264f04ff900ee4fda323299c` |
| `25_calculation_menu.sql` | `649ac760364721e5022b2be69dd23cad112a96144be29d70d7cdcb0c45e1e803` |
| `26_coefficient_teacher_perm.sql` | `c810399960a26413d929e5a99f50f3bbbbc43e30a46a2a877f7b95a7cad966e4` |
| `27_calc_flow_menu.sql` | `e061d98709e17d34d6264766f41425ce5d1422ee28bb306bbc1b6f80b73a24ed` |
| `28_import_framework.sql` | `042ec49220cebacc010af1b9265a7c7365c3d62251202f227b105b96499acbdd` |
| `30_snapshot_context.sql` | `358b80863d33648a453153a004fc16c68e662188e9b0f841f137cf7fb9d081af` |
| `31_item_review_permission.sql` | `054181fc0624593a584441b782e0f2865d9dd747155342ee1f930fa71dc3b11f` |
| `32_workload_consistency.sql` | `86ba2308657e9b233f1f0ac257cc6c2ad93155f92a4dc28aebb25a3b1c6224a2` |
| `33_factor_provenance.sql` | `389a7e9a2d3429f9e5ed1fe2fe5c67e675908fb938ba7832354f10429eda4dff` |
| `34_settlement_snapshot.sql` | `1ce51db2a0684d288d072480817b98af826d50e0d4aa1bd6bd241672d814c6f4` |
| `35_workload_review_contract.sql` | `58230eb606e901e7540cadf0810b91518f0f7384b6b07e339cca5421658e93ce` |
| `quartz.sql` | `68ddc1c2dcd003e5a5470d88f23bfe8daee54d2b6ae77c720e5b82f8dd57ef0a` |
| `ry_20260321.sql` | `c9fbf093800da257534a3329a82d77ab2d5987d9987639c92a43026a33ff7f69` |

32必须先于T03应用发布执行。新代码依赖该锁表及列；仅应用回退不能撤销已产生的数据，禁止DROP表/列回退。旧版不会维护新鲜度标志，若短暂回退旧应用，应禁止业务写入；重新启用新应用前必须对草稿重新标记待核算并重算。

33必须先于读取/写入来源证据列的B应用启动；旧应用可忽略新增可空列，但不得覆盖新证据或用当前政策回填旧NULL。禁止DROP列回退。

34必须先于结算引擎启动。只保留新增结构与快照，不DROP回退；正式启用学期默认空，隔离verification学期不是正式业务批准。

35目前为T06技术契约，不启用T07接口或T08页面。data_version=0、last_calculated_at=NULL表示未纳入新版审核；禁止伪造历史批次/审核结果/来源。只补biz_audit_log.create_by/update_by/update_time三个nullable字段，旧create_time/operator保真；不得对所有biz表做MIGRATION/NOW回填。批次/补差created_*与T05快照字段由专用路径保留，不受通用审计写入覆盖。

35的六项权限按perms查重自动分配menu_id，均为2020下隐藏F按钮；waive和追加补差仅admin/biz_admin，其它四权限再授jiaowu/assistant，teacher/leader无新审核权限。保留既有system:workloadItem:review（草稿核对）。重复执行只补授权矩阵并清除六项新增权限的越权关联，不重置其它菜单。幂等操作需独占迁移窗口，不能并行执行多个迁移进程。

审核证据JSON格式、生命周期/CAS、补差永久只追加、终审门禁和教师本人范围由T07落实；35不是这些业务约束已经上线的证明。2026-11-06在MySQL9.0.1的两个13306克隆各执行两轮35通过，全部原biz原列行数/hash不变、三个新表空、checker通过；另每库14项共28项事务回滚约束/唯一键用例通过，见`docs/验收/35迁移演练证据.json`。MySQL8与整套空库初始化尚未验证。回退保留新增结构/证据，不DROP。
