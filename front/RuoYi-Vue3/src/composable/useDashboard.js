import { ref, reactive, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import * as echarts from 'echarts'
import { listWorkloadSummary } from '@/api/system/workloadSummary'
import { exportPaySummary } from '@/api/system/export'

/**
 * 教务/院领导仪表盘共享逻辑
 */
export function useDashboard() {
  const { proxy } = getCurrentInstance()

  const chartLoading = ref(false)
  const chartRef = ref(null)
  const chartView = ref('bar')
  let chartInstance = null

  const auditCounts = reactive({
    draft: 0,
    pending: 0,
    signed: 0,
    completed: 0
  })

  function formatMoney(val) {
    if (val == null) return '--'
    return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }

  async function fetchAuditCounts() {
    const statuses = [
      { key: 'draft', status: 0 },
      { key: 'pending', status: 1 },
      { key: 'signed', status: 2 },
      { key: 'completed', status: 3 }
    ]
    // 并行请求各状态计数，单个失败不影响其他面板
    await Promise.all(statuses.map(s =>
      listWorkloadSummary({ status: s.status, pageNum: 1, pageSize: 1 })
        .then(res => {
          auditCounts[s.key] = res.total || 0
        })
        .catch(() => {
          // ignore
        })
    ))
  }

  function handleExportPaySummary() {
    proxy.$prompt('请输入学年学期（如 2025-2026-1）', '导出绩效酬金统计表', {
      confirmButtonText: '导出',
      cancelButtonText: '取消',
      inputPattern: /^\d{4}-\d{4}-[12]$/,
      inputErrorMessage: '格式如 2025-2026-1',
      inputPlaceholder: '2025-2026-1'
    }).then(({ value }) => {
      proxy.$modal.loading('正在导出...')
      exportPaySummary({ semester: value }).then(res => {
        const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `绩效酬金统计_${value}.xlsx`
        link.click()
        window.URL.revokeObjectURL(url)
        proxy.$modal.closeLoading()
      }).catch(() => {
        proxy.$modal.closeLoading()
      })
    }).catch(() => {})
  }

  function renderChart() {
    if (!chartRef.value) return
    if (!chartInstance) {
      chartInstance = echarts.init(chartRef.value)
    }

    const auditData = [
      { value: auditCounts.draft, name: '填报中', itemStyle: { color: '#909399' } },
      { value: auditCounts.pending, name: '教务助理待审', itemStyle: { color: '#e6a23c' } },
      { value: auditCounts.signed, name: '院领导待签', itemStyle: { color: '#409eff' } },
      { value: auditCounts.completed, name: '已完结', itemStyle: { color: '#67c23a' } }
    ]

    let option
    if (chartView.value === 'pie') {
      option = {
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0 },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, formatter: '{b}\n{c}条' },
          data: auditData
        }]
      }
    } else {
      option = {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: auditData.map(d => d.name)
        },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          type: 'bar',
          data: auditData,
          barMaxWidth: 60
        }]
      }
    }
    chartInstance.setOption(option, true)
  }

  function handleResize() {
    chartInstance?.resize()
  }

  function setupChart() {
    onMounted(() => {
      window.addEventListener('resize', handleResize)
    })
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
      if (chartInstance) {
        chartInstance.dispose()
        chartInstance = null
      }
    })
  }

  return {
    chartLoading,
    chartRef,
    chartView,
    auditCounts,
    formatMoney,
    fetchAuditCounts,
    handleExportPaySummary,
    renderChart,
    handleResize,
    setupChart
  }
}
