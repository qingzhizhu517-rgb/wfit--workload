import { readFile, writeFile, mkdir } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { ESLint } from 'eslint'

// Exact per-file/rule/message/source/column signatures plus multiplicity: an old
// fix cannot pay for a new error elsewhere. Line numbers are diagnostic only.
export function collectIssues(results, root) {
  const entries = new Map()
  for (const result of results) {
    const lines = (result.source ?? '').split(/\r?\n/)
    for (const message of result.messages) {
      const issue = {
        file: path.relative(root, result.filePath).split(path.sep).join('/'),
        rule: message.ruleId ?? '<fatal>',
        severity: message.severity,
        message: message.message,
        source: lines[(message.line ?? 1) - 1] ?? '',
        column: message.column ?? 0,
        nodeType: message.nodeType ?? null,
        fatal: Boolean(message.fatal)
      }
      const signature = JSON.stringify(issue)
      const existing = entries.get(signature)
      if (existing) existing.count++
      else entries.set(signature, { ...issue, count: 1 })
    }
  }
  return [...entries.values()].sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b), 'en'))
}

const key = ({ count, ...issue }) => JSON.stringify(issue)
export function compareIssues(baseline, current) {
  const difference = (left, right) => {
    const known = new Map(right.map(issue => [key(issue), issue.count]))
    return left.flatMap(issue => {
      const count = issue.count - (known.get(key(issue)) ?? 0)
      return count > 0 ? [{ ...issue, count }] : []
    })
  }
  return { added: difference(current, baseline), removed: difference(baseline, current) }
}

async function main() {
  const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
  const eslint = new ESLint({ cwd: root, extensions: ['.js', '.vue'], ignorePath: path.join(root, '.gitignore') })
  const results = await eslint.lintFiles(['.'])
  const issues = collectIssues(results, root)
  const errors = results.reduce((n, result) => n + result.errorCount, 0)
  const warnings = results.reduce((n, result) => n + result.warningCount, 0)
  const reportDir = path.join(root, 'artifacts/lint')
  await mkdir(reportDir, { recursive: true })
  await writeFile(path.join(reportDir, 'eslint.json'), JSON.stringify(results, null, 2) + '\n')
  const counts = field => Object.entries(issues.reduce((acc, issue) => {
    const value = acc[issue[field]] ?? { errors: 0, warnings: 0 }
    value[issue.severity === 2 ? 'errors' : 'warnings'] += issue.count
    acc[issue[field]] = value
    return acc
  }, {})).sort(([a], [b]) => a.localeCompare(b, 'en'))
  const table = field => `| ${field} | Errors | Warnings |\n|---|---:|---:|\n` + counts(field).map(([name, count]) => `| ${name} | ${count.errors} | ${count.warnings} |`).join('\n')
  const summary = `# Full ESLint debt\n\n${errors} errors, ${warnings} warnings. Full lint ${errors ? 'FAILS' : 'passes'}; baseline gate does not mean lint is clean.\n\n${table('rule')}\n\n${table('file')}\n`
  await writeFile(path.join(reportDir, 'summary.md'), summary)
  console.log(summary)
  const overrideIndex = process.argv.indexOf('--baseline')
  const baselinePath = overrideIndex >= 0 ? path.resolve(process.argv[overrideIndex + 1]) : path.join(root, 'eslint-baseline.json')
  if (process.argv.includes('--write')) {
    await writeFile(baselinePath, JSON.stringify({ formatVersion: 1, errors, warnings, issues }, null, 2) + '\n')
    console.log('Baseline written for explicit review; this does not make full lint pass.')
    return
  }
  const baseline = JSON.parse(await readFile(baselinePath, 'utf8'))
  if (baseline.formatVersion !== 1 || !Array.isArray(baseline.issues)) throw new Error('Invalid baseline format')
  const comparison = compareIssues(baseline.issues, issues)
  await writeFile(path.join(reportDir, 'comparison.json'), JSON.stringify(comparison, null, 2) + '\n')
  console.log(`New violations: ${comparison.added.reduce((n, issue) => n + issue.count, 0)}; removed: ${comparison.removed.reduce((n, issue) => n + issue.count, 0)}`)
  if (comparison.added.length) {
    console.error(JSON.stringify(comparison.added, null, 2))
    process.exitCode = 1
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch(error => { console.error(error); process.exitCode = 1 })
}
