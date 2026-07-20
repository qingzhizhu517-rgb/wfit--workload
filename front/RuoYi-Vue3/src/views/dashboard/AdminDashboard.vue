<template>
  <div class="app-container dashboard-editor-container">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="24">
        <h2 class="dashboard-title">欢迎使用潍坊理工学院教学工作量智能化管理系统</h2>
        <p class="dashboard-subtitle">当前学期：2025-2026学年 第一学期</p>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="panel-group">
      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel">
          <div class="card-panel-icon-wrapper icon-tasks">
            <el-icon><Document /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">本学期任务总数</div>
            <count-to :start-val="0" :end-val="1245" :duration="2600" class="card-panel-num" />
          </div>
        </div>
      </el-col>
      
      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel">
          <div class="card-panel-icon-wrapper icon-workload">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">已核算总工作量</div>
            <count-to :start-val="0" :end-val="45210" :duration="3000" class="card-panel-num" />
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel">
          <div class="card-panel-icon-wrapper icon-teacher">
            <el-icon><User /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">参与核算教师数</div>
            <count-to :start-val="0" :end-val="328" :duration="2000" class="card-panel-num" />
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
        <div class="card-panel">
          <div class="card-panel-icon-wrapper icon-warning">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">待处理异议数</div>
            <count-to :start-val="0" :end-val="12" :duration="1500" class="card-panel-num" style="color: #ff4949;"/>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :lg="8">
        <el-card class="box-card">
          <template #header>
            <div class="clearfix">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="quick-links">
            <el-button type="primary" plain icon="Upload">导入教务处Excel</el-button>
            <el-button type="success" plain icon="Edit">录入特殊工作量</el-button>
            <el-button type="warning" plain icon="Download">导出附件一报表</el-button>
            <el-button type="info" plain icon="Setting">基础系数配置</el-button>
          </div>
        </el-card>

        <el-card class="box-card" style="margin-top: 20px;">
          <template #header>
            <div class="clearfix">
              <span>待办事项</span>
            </div>
          </template>
          <div class="todo-list">
             <el-alert title="大数据学院李老师提交了实践课核算异议" type="warning" :closable="false" style="margin-bottom: 10px;" />
             <el-alert title="外聘教师张老师本月课时费需复核" type="info" :closable="false" style="margin-bottom: 10px;" />
             <el-alert title="请尽快完成2025-2026-1学期期中核算汇总" type="error" :closable="false" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :lg="16">
        <el-card class="box-card">
          <template #header>
            <div class="clearfix">
              <span>各学院核算进度概览 (图表预留区)</span>
            </div>
          </template>
          <div class="chart-placeholder">
            <div style="height: 350px; background-color: #f5f7fa; display: flex; justify-content: center; align-items: center; color: #909399;">
              [Echarts 柱状图区域：X轴-各学院，Y轴-已核算/未核算任务数]
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index">
// 实际开发中，需要引入 CountTo 组件，或者自己实现数字滚动动画
// 这里为了演示结构，假设组件已全局注册
// import CountTo from 'vue-count-to'
import { Document, DataLine, User, Warning, Upload, Edit, Download, Setting } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 84px);
}

.dashboard-title {
  margin-top: 0;
  margin-bottom: 5px;
  color: #303133;
}

.dashboard-subtitle {
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
}

.panel-group {
  margin-bottom: 20px;

  .card-panel-col {
    margin-bottom: 20px;
  }

  .card-panel {
    height: 108px;
    cursor: pointer;
    font-size: 12px;
    position: relative;
    overflow: hidden;
    color: #666;
    background: #fff;
    box-shadow: 4px 4px 40px rgba(0, 0, 0, .05);
    border-color: rgba(0, 0, 0, .05);
    border-radius: 4px;
    display: flex;
    align-items: center;
    padding: 0 20px;

    &:hover {
      .card-panel-icon-wrapper {
        color: #fff;
      }
      .icon-tasks { background: #40c9c6; }
      .icon-workload { background: #36a3f7; }
      .icon-teacher { background: #f4516c; }
      .icon-warning { background: #e6a23c; }
    }

    .card-panel-icon-wrapper {
      padding: 16px;
      transition: all 0.38s ease-out;
      border-radius: 6px;
      font-size: 48px;
    }

    .icon-tasks { color: #40c9c6; }
    .icon-workload { color: #36a3f7; }
    .icon-teacher { color: #f4516c; }
    .icon-warning { color: #e6a23c; }

    .card-panel-description {
      margin-left: auto;
      font-weight: bold;
      text-align: right;

      .card-panel-text {
        line-height: 18px;
        color: rgba(0, 0, 0, 0.45);
        font-size: 16px;
        margin-bottom: 12px;
      }

      .card-panel-num {
        font-size: 20px;
        color: #666;
      }
    }
  }
}

.quick-links {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  
  .el-button {
    margin: 0;
    width: 100%;
    height: 60px;
    font-size: 15px;
  }
}
</style>