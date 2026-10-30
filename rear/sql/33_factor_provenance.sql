-- T05-B: nullable immutable source evidence only; no historical backfill or state/amount rewrite.
SET @wfit_provenance_missing = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_workload_item' AND column_name='factor_provenance_json') = 0;
SET @wfit_provenance_ddl = IF(@wfit_provenance_missing, 'ALTER TABLE biz_workload_item ADD COLUMN factor_provenance_json LONGTEXT NULL COMMENT ''T05 actual calculation source evidence; NULL means missing historical source''', 'SELECT 1');
PREPARE wfit_provenance_stmt FROM @wfit_provenance_ddl;
EXECUTE wfit_provenance_stmt;
DEALLOCATE PREPARE wfit_provenance_stmt;
SET @wfit_provenance_missing = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_allowance_item' AND column_name='calculation_evidence_json') = 0;
SET @wfit_provenance_ddl = IF(@wfit_provenance_missing, 'ALTER TABLE biz_allowance_item ADD COLUMN calculation_evidence_json LONGTEXT NULL COMMENT ''T05 actual calculation source evidence; NULL means missing historical source''', 'SELECT 1');
PREPARE wfit_provenance_stmt FROM @wfit_provenance_ddl;
EXECUTE wfit_provenance_stmt;
DEALLOCATE PREPARE wfit_provenance_stmt;
