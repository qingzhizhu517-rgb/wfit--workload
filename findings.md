# 核查证据（2026-10-25）

## 证据边界
仓库为 /Users/a1/Desktop/wfit--workload。当前工作树有既有开发内容，分析不得覆盖。既有根目录规划文件不存在，已建立本轮记录。

## 架构、实现与验证
- 后端4个Maven模块；业务集中在workload-system；calc下33个Java文件；controller目录24个Java文件（包括ImportFileValidator辅助类，不能当成24个Controller）。
- 本地38个测试Java类，含1个显式MySQL IT；AGENTS/CLAUDE“只有两个策略测试”已过时。
- .github目录不存在；本地package.json只有dev/build/preview/lint，没有test:unit，远程已有前端单元测试命令。
- 既有计划复选框严重滞后：imports/export-pay均0勾选，但远程已实现主要任务；审核中心计划需逐项对照，不可据勾选数量断言进度。
- README仍列G1-G3乘Q3及教师自报G11，与近期已确认口径冲突；后续需以策略实现核实并修正文档。
- Java环境存在漂移：项目target=17，本机mvn默认Java26；已发现JDK17可用于本轮验证。
- 本轮JDK17 `mvn -B -ntp test`：292个测试，失败/错误/跳过0，BUILD SUCCESS（13.384s）。默认不运行IT。
- 本轮 `npm run build:prod`：成功（15.80s）。`npm run lint`退出1：199文件，1477 errors/86 warnings；1305条linebreak-style。
- 缓存旧待办需更正：规则Service增删改已有evict；仍无按历史学期选值接口，Redis键仅规则code且无TTL，时间跨有效期和字典策略缓存失效仍需验证。
- ruleVersionOf仅生成RULE-<semester>，不能代表不可变规则版本；快照展示未校验哈希，汇总/酬金缺独立不可变上下文。
- 本地审核有教务核对、待审系数门禁和汇总锁；独立审核中心、审核批次、线下补差、统一审计填充器在本地及远程均未找到对应实现。
- 远程岗位减免importPreview/importConfirm后端已存在，但roleAssignment前端无对应调用；teacherProfile页面仍直调旧importData，无新花名册预检接入。
- 旧审核中心计划使用22号迁移文件，与现有22号已冲突；现阶段应先复核方案，不能原样执行。
- 本轮显式MySQL IT：14项通过，失败/错误/跳过0，BUILD SUCCESS（5.369s）；常规测试292与IT14是两个独立命令，非同一次全量验收。
- 本轮只读复核数据库：MySQL9.0.1，52表，6教师/9任务/14明细/0持久快照；快照表有context_json，缺biz_import_row与远程新列，也无已规划的审核明细表及迁移历史表。
- G1-G3策略不乘Q3；G4按实际人数、不截断，超过60告警；G6仍按20封顶。README旧公式不应作为验收期望。
- 源子表删除前已有assertEditable，不能再列“删除无冻结保护”为未修复；onDetailDeleted只清主表结果且不刷汇总/酬金，是否造成可提交的陈旧汇总需专项复现。
- 后端unlock已永久拒绝；前端workloadSummary仍有解锁按钮和可重新编辑文案，确认是前后端行为不一致，不是已证明可绕过后端锁。
- 前端构建提示VITE_APP_TITLE缺失；有两个较大chunk。尚未进行浏览器实测或性能测试，不能用文件大小推导运行性能结论。
- 最终diff空白检查未通过（exit2），发现既有修改的空白/CRLF问题；新增报告内容使用LF，未修改既有代码来掩盖基线问题。
