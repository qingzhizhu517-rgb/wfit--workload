"""Read-only catalog checks: no database process or credentials required."""
import importlib.util
import json
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'check-migrations.py'
spec = importlib.util.spec_from_file_location('migration_checker', SCRIPT)
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)


class ReviewMigrationContractTest(unittest.TestCase):
    def checks(self, columns=None, indexes=None, defaults=None):
        self.assertTrue(hasattr(checker, 'check_review_contract'),
                        'Missing read-only T06 catalog validator')
        return checker.check_review_contract(columns or {}, indexes or {}, defaults or {})

    def test_missing_review_tables_are_rejected(self):
        checks = self.checks()
        self.assertFalse(all(checks.values()))
        self.assertFalse(checks['review:batch_version'])
        self.assertFalse(checks['review:unique_item_in_batch'])

    def test_nullable_historical_audit_fields_are_required(self):
        columns = {('biz_audit_log', field): (kind, 'YES') for field, kind in [
            ('create_by', 'varchar(64)'), ('update_by', 'varchar(64)'),
            ('update_time', 'datetime')]}
        checks = self.checks(columns)
        self.assertTrue(checks['review:historical_audit_nullable'])
        columns[('biz_audit_log', 'create_by')] = ('varchar(64)', 'NO')
        self.assertFalse(self.checks(columns)['review:historical_audit_nullable'])

    def test_index_order_and_fresh_version_default_are_checked(self):
        indexes = {('biz_workload_item_review', 'uk_summary_batch_item'):
                   ('0', 'summary_id,review_batch_no,item_id')}
        checks = self.checks(indexes=indexes,
                             defaults={('biz_workload_summary', 'data_version'): '0'})
        self.assertTrue(checks['review:unique_item_in_batch'])
        self.assertTrue(checks['review:version_zero_by_default'])
        indexes[('biz_workload_item_review', 'uk_summary_batch_item')] = (
            '1', 'item_id,review_batch_no,summary_id')
        self.assertFalse(self.checks(indexes=indexes)['review:unique_item_in_batch'])
        self.assertFalse(self.checks(defaults={('biz_workload_summary', 'data_version'): '1'})[
            'review:version_zero_by_default'])

    def test_adjustment_has_signed_amount_and_business_processing_date(self):
        checks = self.checks()
        self.assertIn('review:adjustment_signed_amount', checks)
        self.assertFalse(checks['review:adjustment_signed_amount'])
        columns = {
            ('biz_offline_adjustment_note', 'adjustment_amount'): ('decimal(12,2)', 'NO'),
            ('biz_offline_adjustment_note', 'handled_at'): ('date', 'NO'),
            ('biz_offline_adjustment_note', 'note_text'): ('text', 'YES'),
        }
        checks = self.checks(columns)
        self.assertTrue(checks['review:adjustment_signed_amount'])
        self.assertTrue(checks['review:adjustment_business_date'])
        self.assertTrue(checks['review:adjustment_optional_note'])
        columns[('biz_offline_adjustment_note', 'adjustment_amount')] = ('decimal(12,2) unsigned', 'NO')
        self.assertFalse(self.checks(columns)['review:adjustment_signed_amount'])

    def test_snapshot_payload_and_evidence_hash_cannot_be_nullable(self):
        checks = self.checks()
        self.assertIn('review:shape:biz_workload_review_batch.snapshot_json', checks)
        for table in ('biz_workload_review_batch', 'biz_workload_item_review'):
            payload = (table, 'snapshot_json')
            data_hash = (table, 'data_hash')
            columns = {payload: ('json', 'NO'), data_hash: ('char(64)', 'NO')}
            self.assertTrue(self.checks(columns)['review:shape:' + '.'.join(payload)])
            self.assertTrue(self.checks(columns)['review:shape:' + '.'.join(data_hash)])
            columns[payload] = ('json', 'YES')
            self.assertFalse(self.checks(columns)['review:shape:' + '.'.join(payload)])

    def test_every_business_unique_key_column_rejects_nullability(self):
        # MySQL UNIQUE permits repeated NULLs: the index alone is insufficient.
        keys = [
            ('biz_workload_review_batch', 'summary_id', 'bigint'),
            ('biz_workload_review_batch', 'review_batch_no', 'varchar(50)'),
            ('biz_workload_item_review', 'summary_id', 'bigint'),
            ('biz_workload_item_review', 'review_batch_no', 'varchar(50)'),
            ('biz_workload_item_review', 'item_id', 'bigint'),
            ('biz_offline_adjustment_note', 'summary_id', 'bigint'),
            ('biz_offline_adjustment_note', 'request_key', 'varchar(64)'),
        ]
        for table, column, kind in keys:
            with self.subTest(table=table, column=column):
                key = f'review:shape:{table}.{column}'
                self.assertIn(key, self.checks())
                self.assertTrue(self.checks({(table, column): (kind, 'NO')})[key])
                self.assertFalse(self.checks({(table, column): (kind, 'YES')})[key])
                self.assertFalse(self.checks({(table, column): ('varchar(1)', 'NO')})[key])

    def test_required_references_and_versions_reject_nullable_or_wrong_type(self):
        required = [
            ('biz_workload_review_batch', 'user_id', 'bigint'),
            ('biz_workload_review_batch', 'semester', 'varchar(20)'),
            ('biz_workload_review_batch', 'summary_data_version', 'bigint'),
            ('biz_workload_review_batch', 'format_version', 'int'),
            ('biz_workload_item_review', 'version', 'bigint'),
            ('biz_workload_item_review', 'format_version', 'int'),
            ('biz_offline_adjustment_note', 'user_id', 'bigint'),
            ('biz_offline_adjustment_note', 'semester', 'varchar(20)'),
            ('biz_offline_adjustment_note', 'handled_by_id', 'bigint'),
        ]
        for table, column, kind in required:
            with self.subTest(table=table, column=column):
                key = f'review:shape:{table}.{column}'
                self.assertIn(key, self.checks())
                self.assertTrue(self.checks({(table, column): (kind, 'NO')})[key])
                self.assertFalse(self.checks({(table, column): (kind, 'YES')})[key])
                self.assertFalse(self.checks({(table, column): ('text', 'NO')})[key])


class ReviewCheckClauseTest(unittest.TestCase):
    def setUp(self):
        # Actual MySQL 9.0.1 CHECK_CLAUSE metadata, read using HEX on 2026-11-06.
        path = Path(__file__).parent / 'fixtures/mysql9_review_check_clauses.json'
        self.catalog = {(row['table'], row['name']): ('YES', row['clause'])
                        for row in json.loads(path.read_text())}

    def checks(self, catalog):
        self.assertTrue(hasattr(checker, 'check_review_constraints'),
                        'Missing CHECK-clause semantic validator')
        return checker.check_review_constraints(catalog)

    def test_current_mysql_metadata_is_accepted(self):
        self.assertEqual(len(self.checks(self.catalog)), 8)
        self.assertTrue(all(self.checks(self.catalog).values()))

    def test_named_enforced_constraints_cannot_be_true_or_relaxed(self):
        for key, (_, original) in self.catalog.items():
            for weakened in ('TRUE', original + ' OR TRUE', original + ' /* unknown syntax */'):
                with self.subTest(key=key, weakened=weakened):
                    catalog = dict(self.catalog)
                    catalog[key] = ('YES', weakened)
                    self.assertFalse(self.checks(catalog)['review:constraint:' + '.'.join(key)])

    def test_grouping_literal_content_and_enforcement_are_preserved(self):
        key = ('biz_workload_item_review', 'ck_item_review_reason')
        original = self.catalog[key][1]
        relaxed = original.replace('and (char_length', 'or (char_length')
        for enforced, clause in [('NO', original), ('YES', relaxed),
                                 ('YES', original.replace('REJECT', 'REJECT '))]:
            with self.subTest(enforced=enforced, clause=clause):
                catalog = dict(self.catalog)
                catalog[key] = (enforced, clause)
                self.assertFalse(self.checks(catalog)['review:constraint:' + '.'.join(key)])
        key = ('biz_workload_review_batch', 'ck_review_batch_settlement')
        catalog = dict(self.catalog)
        # Same operands/operators, different AND/OR grouping must fail.
        catalog[key] = ('YES', '((`settlement_snapshot_id` is null) and '
                        '((`settlement_snapshot_hash` is null) or '
                        '(`settlement_snapshot_id` is not null)) and '
                        '(`settlement_snapshot_hash` is not null))')
        self.assertFalse(self.checks(catalog)['review:constraint:' + '.'.join(key)])

    def test_only_formatting_tokens_are_normalized(self):
        key = ('biz_workload_item_review', 'ck_item_review_result')
        catalog = dict(self.catalog)
        catalog[key] = ('YES', "( RESULT  IN ('PENDING', 'PASS', 'REJECT', 'WAIVE') )")
        self.assertTrue(self.checks(catalog)['review:constraint:' + '.'.join(key)])


if __name__ == '__main__':
    unittest.main()
