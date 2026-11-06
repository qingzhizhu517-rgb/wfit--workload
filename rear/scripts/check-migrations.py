#!/usr/bin/env python3
"""Read-only checks for the integrated database contract (no automatic migration)."""
import argparse
import json
import os
import re
import subprocess
import sys


def normalize_review_check_clause(clause):
    """Tokenize the verified MySQL 9 form, preserving grouping and literals.

    Unknown syntax fails closed. Do not strip all parentheses or whitespace inside
    strings: that could turn a weakened CHECK into the same apparent expression.
    """
    token = re.compile(
        r"\s+|`(?P<quoted>[A-Za-z_][A-Za-z_0-9]*)`|"
        r"(?P<string>(?:_utf8mb4)?(?:'[A-Z_0-9 ]*'|\\'[A-Z_0-9 ]*\\'))|"
        r"(?P<word>[A-Za-z_][A-Za-z_0-9]*)|(?P<number>[0-9]+)|"
        r"(?P<symbol>>=|>|[(),])", re.IGNORECASE)
    tokens = []
    position = 0
    while position < len(clause):
        match = token.match(clause, position)
        if match is None:
            return None
        position = match.end()
        if match.group('quoted') is not None:
            tokens.append(('word', match.group('quoted').lower()))
        elif match.group('string') is not None:
            value = re.sub(r'^_utf8mb4', '', match.group('string'), flags=re.IGNORECASE)
            # CHECK_CLAUSE represents its delimiter quotes as backslash-quoted on
            # the verified server. No escapes inside a literal are normalized.
            literal = value[2:-2] if value.startswith("\\'") else value[1:-1]
            tokens.append(('string', literal))
        elif match.group('word') is not None:
            tokens.append(('word', match.group('word').lower()))
        elif match.group('number') is not None:
            tokens.append(('number', match.group('number')))
        elif match.group('symbol') is not None:
            tokens.append(('symbol', match.group('symbol')))
    return tuple(tokens)


def check_review_constraints(constraints):
    """Compare complete CHECK expressions, not names or permissive fragments.

    Parentheses follow actual MySQL 9.0.1 canonical metadata. A different server's
    redundant-parenthesis rendering may conservatively fail until verified.
    """
    settlement = ('(((settlement_snapshot_id is null) and (settlement_snapshot_hash is null)) '
                  'or ((settlement_snapshot_id is not null) and (settlement_snapshot_hash is not null)))')
    expected = {
        ('biz_workload_review_batch', 'ck_review_batch_version'):
            '((summary_data_version > 0) and (format_version > 0))',
        ('biz_workload_review_batch', 'ck_review_batch_settlement'): settlement,
        ('biz_workload_item_review', 'ck_item_review_type'):
            "(review_type in ('TEACHING_TASK','MANUAL_DECLARATION','G11_ROLE','OVER_LIMIT'))",
        ('biz_workload_item_review', 'ck_item_review_result'):
            "(result in ('PENDING','PASS','REJECT','WAIVE'))",
        ('biz_workload_item_review', 'ck_item_review_reason'):
            "((result not in ('REJECT','WAIVE')) or ((reason is not null) and (char_length(trim(reason)) > 0)))",
        ('biz_workload_item_review', 'ck_item_review_version'):
            '((version >= 0) and (format_version > 0))',
        ('biz_offline_adjustment_note', 'ck_adjustment_reason'):
            '(char_length(trim(reason)) > 0)',
        ('biz_offline_adjustment_note', 'ck_adjustment_settlement'): settlement,
    }
    checks = {}
    for (table, name), expected_clause in expected.items():
        enforced, clause = constraints.get((table, name), ('', ''))
        actual_tokens = normalize_review_check_clause(clause)
        expected_tokens = normalize_review_check_clause(expected_clause)
        checks[f'review:constraint:{table}.{name}'] = (
            enforced == 'YES' and actual_tokens is not None and
            expected_tokens is not None and actual_tokens == expected_tokens)
    return checks


def check_review_contract(present, indexes, defaults):
    """Validate only T06 structure; never infer historical review completion."""
    checks = {}
    fields = {
        'biz_workload_summary': 'review_batch_no data_version last_calculated_at',
        'biz_workload_review_batch': 'id summary_id review_batch_no summary_data_version user_id semester settlement_snapshot_id settlement_snapshot_hash format_version snapshot_json data_hash created_by created_at',
        'biz_workload_item_review': 'id summary_id item_id review_batch_no review_type format_version snapshot_json data_hash result reason reviewer_id reviewer_name reviewed_at version create_by create_time update_by update_time',
        'biz_offline_adjustment_note': 'id summary_id user_id semester request_key adjustment_amount reason handled_at handled_by_id handled_by reference_no note_text attachment_ref settlement_snapshot_id settlement_snapshot_hash created_by created_at',
    }
    for table, names in fields.items():
        for column in names.split():
            checks[f'review:column:{table}.{column}'] = (table, column) in present
    def is_integer(table, column, nullable='NO'):
        kind, null = present.get((table, column), ('', ''))
        return kind.split('(')[0] == 'bigint' and null == nullable
    checks['review:batch_version'] = is_integer('biz_workload_review_batch', 'summary_data_version')
    checks['review:summary_version'] = is_integer('biz_workload_summary', 'data_version')
    checks['review:cas_version'] = is_integer('biz_workload_item_review', 'version')
    checks['review:adjustment_signed_amount'] = present.get(
        ('biz_offline_adjustment_note', 'adjustment_amount')) == ('decimal(12,2)', 'NO')
    checks['review:adjustment_business_date'] = present.get(
        ('biz_offline_adjustment_note', 'handled_at')) == ('date', 'NO')
    checks['review:adjustment_optional_note'] = present.get(
        ('biz_offline_adjustment_note', 'note_text')) == ('text', 'YES')
    # IF NOT EXISTS does not repair an incompatible pre-existing table. A UNIQUE
    # index permits multiple NULL keys, so validate required types/nullability too.
    required_shapes = {
        'biz_workload_review_batch': {
            'bigint': 'id summary_id summary_data_version user_id',
            'varchar(50)': 'review_batch_no', 'varchar(20)': 'semester',
            'int': 'format_version', 'json': 'snapshot_json', 'char(64)': 'data_hash',
            'varchar(64)': 'created_by', 'datetime': 'created_at',
        },
        'biz_workload_item_review': {
            'bigint': 'id summary_id item_id version', 'varchar(50)': 'review_batch_no',
            'varchar(32)': 'review_type', 'int': 'format_version', 'json': 'snapshot_json',
            'char(64)': 'data_hash', 'varchar(16)': 'result',
            'varchar(64)': 'create_by', 'datetime': 'create_time',
        },
        'biz_offline_adjustment_note': {
            'bigint': 'id summary_id user_id handled_by_id', 'varchar(20)': 'semester',
            'varchar(64)': 'request_key handled_by created_by',
            'decimal(12,2)': 'adjustment_amount', 'varchar(500)': 'reason',
            'date': 'handled_at', 'datetime': 'created_at',
        },
    }
    for table, groups in required_shapes.items():
        for expected_kind, names in groups.items():
            for column in names.split():
                kind, nullable = present.get((table, column), ('', ''))
                # MySQL 8 can include integer display widths; unsigned still fails.
                kind = re.sub(r'^(bigint|int)\([0-9]+\)$', r'\1', kind)
                checks[f'review:shape:{table}.{column}'] = (
                    kind == expected_kind and nullable == 'NO')
    checks['review:version_zero_by_default'] = defaults.get(('biz_workload_summary', 'data_version')) == '0'
    checks['review:historical_audit_nullable'] = all(
        present.get(('biz_audit_log', column)) == (kind, 'YES')
        for column, kind in [('create_by', 'varchar(64)'), ('update_by', 'varchar(64)'), ('update_time', 'datetime')])
    checks['review:historical_calculated_at_nullable'] = present.get(
        ('biz_workload_summary', 'last_calculated_at')) == ('datetime', 'YES')
    checks['review:no_polymorphic_target'] = all(
        ('biz_workload_item_review', column) not in present for column in ('target_type', 'target_id'))
    for label, table, name, unique, columns in [
        ('unique_batch', 'biz_workload_review_batch', 'uk_summary_review_batch', '0', 'summary_id,review_batch_no'),
        ('unique_item_in_batch', 'biz_workload_item_review', 'uk_summary_batch_item', '0', 'summary_id,review_batch_no,item_id'),
        ('result_page', 'biz_workload_item_review', 'idx_review_summary_result', '1', 'summary_id,review_batch_no,result,id'),
        ('summary_page', 'biz_workload_summary', 'idx_summary_review_center', '1', 'status,semester,review_batch_no,user_id'),
        ('unique_note_request', 'biz_offline_adjustment_note', 'uk_adjustment_request', '0', 'summary_id,request_key'),
        ('note_page', 'biz_offline_adjustment_note', 'idx_adjustment_summary', '1', 'summary_id,id'),
    ]:
        checks['review:' + label] = indexes.get((table, name)) == (unique, columns)
    return checks


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mysql', default='mysql', help='MySQL CLI executable')
    args = parser.parse_args()
    required = ('WFIT_DB_HOST', 'WFIT_DB_PORT', 'WFIT_DB_NAME',
                'WFIT_DB_USER', 'WFIT_DB_PASSWORD')
    missing = [key for key in required if not os.environ.get(key)]
    if missing:
        parser.error('Missing environment variables: ' + ', '.join(missing))
    database = os.environ['WFIT_DB_NAME']
    if not re.fullmatch(r'[A-Za-z0-9_]+', database):
        parser.error('WFIT_DB_NAME must contain only letters, digits and underscores')
    env = dict(os.environ, MYSQL_PWD=os.environ['WFIT_DB_PASSWORD'])
    command = [args.mysql, '--no-defaults', '--protocol=TCP', '--batch',
               '--skip-column-names', '--connect-timeout=10',
               '--host=' + os.environ['WFIT_DB_HOST'],
               '--port=' + os.environ['WFIT_DB_PORT'],
               '--user=' + os.environ['WFIT_DB_USER'], '--database=' + database]

    def query(sql):
        result = subprocess.run(command, input=sql, text=True, env=env,
                                capture_output=True, timeout=30, check=False)
        if result.returncode:
            # Do not include server/CLI diagnostics: they may contain connection details.
            raise RuntimeError('Database check failed; verify connectivity and SELECT permissions')
        return [line.split('\t') for line in result.stdout.splitlines()]

    columns = query("SELECT table_name,column_name,column_type,is_nullable "
                    "FROM information_schema.columns WHERE table_schema=DATABASE();")
    present = {(table, column): (kind.lower(), nullable)
               for table, column, kind, nullable in columns}
    checks = {}
    expected = {
        'biz_import_batch': 'template_version template_category source_hash operator_id '
                            'operator_name confirm_token_hash confirmed_at',
        'biz_teaching_task': 'input_planned_hours input_days input_weeks input_credits quantity_type',
        'biz_import_row': 'id batch_id row_no raw_json normalized_json status error_code '
                          'error_message target_id create_by create_time update_by update_time remark',
        'biz_workload_item': 'calculation_version last_calculated_at factor_provenance_json',
        'biz_allowance_item': 'calculation_evidence_json',
        'biz_workload_calc_snapshot': 'item_id calculation_version snapshot_hash context_json',
        'biz_coefficient_adjustment': 'id item_id status',
        'biz_workload_summary': 'needs_recalculation settlement_snapshot_id',
        'biz_pay_record': 'settlement_snapshot_id',
        'biz_workload_settlement_snapshot': 'id summary_id user_id semester settlement_version format_version payload_json hash created_by created_at',
        'biz_workload_period_lock': 'user_id semester',
    }
    for table, names in expected.items():
        for column in names.split():
            checks[f'column:{table}.{column}'] = (table, column) in present
    checks['context:nullable_longtext'] = present.get(
        ('biz_workload_calc_snapshot', 'context_json')) == ('longtext', 'YES')
    for table, column in [('biz_workload_item', 'factor_provenance_json'),
                          ('biz_allowance_item', 'calculation_evidence_json')]:
        checks[f'provenance:{table}.{column}:nullable_longtext'] = (
            present.get((table, column)) == ('longtext', 'YES'))
    freshness = present.get(('biz_workload_summary', 'needs_recalculation'), ('', ''))
    checks['freshness:non_null_flag'] = (
        freshness[0].split('(')[0] == 'tinyint' and freshness[1] == 'NO')
    default = query("SELECT column_default FROM information_schema.columns "
                    "WHERE table_schema=DATABASE() AND table_name='biz_workload_summary' "
                    "AND column_name='needs_recalculation';")
    checks['freshness:uncalculated_by_default'] = default == [['1']]
    index_rows = query("SELECT table_name,index_name,non_unique,"
                       "GROUP_CONCAT(column_name ORDER BY seq_in_index) "
                       "FROM information_schema.statistics WHERE table_schema=DATABASE() "
                       "GROUP BY table_name,index_name,non_unique;")
    indexes = {(table, name): (non_unique, fields)
               for table, name, non_unique, fields in index_rows}
    checks['index:import_source_non_unique'] = indexes.get(
        ('biz_import_batch', 'idx_import_source')) == ('1', 'import_type,source_hash')
    checks['index:no_legacy_unique_import_source'] = (
        'biz_import_batch', 'uk_import_source') not in indexes
    checks['index:unique_import_row'] = indexes.get(
        ('biz_import_row', 'uk_batch_row')) == ('0', 'batch_id,row_no')
    checks['index:unique_snapshot_version'] = indexes.get(
        ('biz_workload_calc_snapshot', 'uk_item_version')) == ('0', 'item_id,calculation_version')
    checks['index:unique_teacher_semester_lock'] = indexes.get(
        ('biz_workload_period_lock', 'PRIMARY')) == ('0', 'user_id,semester')
    checks['index:unique_settlement_version'] = indexes.get(('biz_workload_settlement_snapshot', 'uk_summary_settlement_version')) == ('0', 'summary_id,settlement_version')
    defaults = query("SELECT table_name,column_name,column_default FROM information_schema.columns "
                     "WHERE table_schema=DATABASE() AND table_name='biz_workload_summary' "
                     "AND column_name='data_version';")
    checks.update(check_review_contract(present, indexes,
                  {(table, column): value for table, column, value in defaults}))
    constraint_rows = query("SELECT t.table_name,t.constraint_name,t.enforced,HEX(c.check_clause) "
                            "FROM information_schema.table_constraints t "
                            "JOIN information_schema.check_constraints c "
                            "ON c.constraint_schema=t.constraint_schema AND c.constraint_name=t.constraint_name "
                            "WHERE t.constraint_schema=DATABASE() AND t.constraint_type='CHECK';")
    # HEX avoids mysql CLI's extra backslash/tab escaping of CHECK_CLAUSE text.
    constraints = {(table, name): (enforced, bytes.fromhex(clause_hex).decode('utf-8'))
                   for table, name, enforced, clause_hex in constraint_rows}
    checks.update(check_review_constraints(constraints))
    tables = {table for table, _ in present}
    if {'sys_menu', 'sys_role', 'sys_role_menu'} <= tables:
        menus = query("SELECT menu_id,parent_id,menu_type,status FROM sys_menu "
                      "WHERE perms='system:workloadItem:review';")
        checks['permission:single_active_review_button'] = (
            len(menus) == 1 and menus[0][1:] == ['2010', 'F', '0'])
        roles = query("SELECT r.role_id,r.role_key,COUNT(m.menu_id) FROM sys_role r "
                      "LEFT JOIN sys_role_menu grants ON grants.role_id=r.role_id "
                      "LEFT JOIN sys_menu m ON m.menu_id=grants.menu_id "
                      "AND m.perms='system:workloadItem:review' WHERE r.status='0' "
                      "GROUP BY r.role_id,r.role_key;")
        allowed = {'admin', 'biz_admin', 'jiaowu', 'assistant'}
        checks['permission:reviewer_role_exists'] = any(key in allowed for _, key, _ in roles)
        for role_id, key, count in roles:
            checks[f'permission:role:{role_id}:{key}'] = (
                int(count) == 1 if key in allowed else int(count) == 0)
        review_permissions = {
            'system:auditRecord:list': allowed,
            'system:auditRecord:query': allowed,
            'system:auditRecord:review': allowed,
            'system:auditRecord:waive': {'admin', 'biz_admin'},
            'system:offlineAdjustmentNote:query': allowed,
            'system:offlineAdjustmentNote:add': {'admin', 'biz_admin'},
        }
        for permission, allowed_keys in review_permissions.items():
            rows = query("SELECT menu_id,parent_id,menu_type,visible,status,IFNULL(component,'') "
                         "FROM sys_menu WHERE perms='" + permission + "';")
            checks['permission:hidden:' + permission] = (
                len(rows) == 1 and rows[0][1:] == ['2020', 'F', '1', '0', ''])
            grants = query("SELECT r.role_id,r.role_key,r.status,r.del_flag,COUNT(m.menu_id) "
                           "FROM sys_role r LEFT JOIN sys_role_menu g ON g.role_id=r.role_id "
                           "LEFT JOIN sys_menu m ON m.menu_id=g.menu_id AND m.perms='" + permission + "' "
                           "GROUP BY r.role_id,r.role_key,r.status,r.del_flag;")
            for role_id, key, status, deleted, count in grants:
                expected_count = 1 if key in allowed_keys and status == '0' and deleted == '0' else 0
                checks[f'permission:{permission}:role:{role_id}'] = int(count) == expected_count
    else:
        checks['permission:base_tables_exist'] = False
    result = {
        'database': database,
        'server_version': query('SELECT VERSION();')[0][0],
        'table_count': len(tables),
        'passed': all(checks.values()),
        'checks': checks,
        'missing_or_invalid': [key for key, ok in checks.items() if not ok],
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as exc:
        print(json.dumps({'passed': False, 'error': str(exc)}, ensure_ascii=False))
        sys.exit(2)
