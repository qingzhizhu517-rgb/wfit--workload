#!/usr/bin/env python3
"""Read-only checks for the integrated database contract (no automatic migration)."""
import argparse
import json
import os
import re
import subprocess
import sys


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
        'biz_workload_summary': 'needs_recalculation',
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
