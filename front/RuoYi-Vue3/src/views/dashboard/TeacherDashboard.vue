<template>
  <div class="teacher-dashboard">
    <el-row :gutter="20" class="mb20">
      <el-col :span="24">
        <el-card shadow="hover" class="welcome-card">
          <div class="user-info">
            <el-avatar :size="60" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
            <div class="info-text">
              <h2>张老师，您好！祝您今天工作顺利。</h2>
              <p class="role-desc">当前岗位：讲师 | 所属院系：大数据学院 | 额定工作量：240 学时/学年</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb20">
      <el-col :xs="12" :sm="12" :lg="8">
        <el-card shadow="hover" class="data-card info-bg">
          <div class="card-header">本学期承担课程</div>
          <div class="card-value">3 <span class="unit">门</span></div>
          <div class="card-bottom">计划总学时：128 学时</div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="12" :lg="8">
        <el-card shadow="hover" class="data-card success-bg">
          <div class="card-header">本学期已核算工作量</div>
          <div class="card-value">156.5 <span class="unit">标准学时</span></div>
          <div class="card-bottom">折算后较原计划增加 28.5 学时</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :lg="8">
        <el-card shadow="hover" class="data-card warning-bg">
          <div class="card-header">预计超工作量绩效(税前)</div>
          <div class="card-value">¥ 0.00</div> 
          <div class="card-bottom">注：超出每学期180学时或学年240学时后计算</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-title">
              <span>近期工作量核算明细</span>
              <el-button type="primary" link>查看全部明细 >></el-button>
            </div>
          </template>
          <el-table :data="recentWorkloads" style="width: 100%" size="small">
            <el-table-column prop="courseName" label="课程名称" min-width="120" />
            <el-table-column prop="courseType" label="类型" width="80">
               <template #default="scope">
                 <el-tag :type="scope.row.courseType === '理论课' ? '' : 'success'">{{ scope.row.courseType }}</el-tag>
               </template>
            </el-table-column>
            <el-table-column prop="baseHours" label="计划学时" width="80" align="center" />
            <el-table-column prop="coefficient" label="综合系数" width="80" align="center" />
            <el-table-column prop="finalHours" label="核算学时" width="100" align="center">
              <template #default="scope">
                <strong style="color: #67C23A;">{{ scope.row.finalHours }}</strong>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-title">
              <span>服务指南</span>
            </div>
          </template>
          <div class="action-list">
            <el-button type="primary" plain class="action-btn" icon="Document">查看学校工作量管理办法</el-button>
            <el-button type="warning" plain class="action-btn" icon="Warning">对核算结果有异议？发起申诉</el-button>
            <el-button type="success" plain class="action-btn" icon="Download">下载个人工作量证明</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="TeacherDashboard">
import { ref } from 'vue'

// 模拟最近的核算数据
const recentWorkloads = ref([
  {
    courseName: 'Java企业级应用开发',
    courseType: '理论课',
    baseHours: 48,
    coefficient: 1.1, // 必修课系数
    finalHours: 52.8
  },
  {
    courseName: '数据库系统原理',
    courseType: '理论课',
    baseHours: 32,
    coefficient: 1.2, // 合堂150人以上等系数
    finalHours: 38.4
  },
  {
    courseName: 'Web前端开发实践',
    courseType: '实践课',
    baseHours: 16,
    coefficient: 0.9,
    finalHours: 14.4
  }
])
</script>

<style scoped lang="scss">
.teacher-dashboard {
  .mb20 {
    margin-bottom: 20px;
  }
  
  .welcome-card {
    background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
    .user-info {
      display: flex;
      align-items: center;
      padding: 10px 0;
      
      .info-text {
        margin-left: 20px;
        h2 {
          margin: 0 0 10px 0;
          color: #303133;
          font-size: 22px;
        }
        .role-desc {
          margin: 0;
          color: #606266;
          font-size: 14px;
        }
      }
    }
  }

  .data-card {
    color: #fff;
    border: none;
    
    &.info-bg { background: linear-gradient(to right, #4facfe 0%, #00f2fe 100%); }
    &.success-bg { background: linear-gradient(to right, #43e97b 0%, #38f9d7 100%); }
    &.warning-bg { background: linear-gradient(to right, #fa709a 0%, #fee140 100%); }

    .card-header {
      font-size: 16px;
      opacity: 0.9;
      margin-bottom: 15px;
    }
    .card-value {
      font-size: 32px;
      font-weight: bold;
      margin-bottom: 15px;
      .unit {
        font-size: 14px;
        font-weight: normal;
        opacity: 0.8;
      }
    }
    .card-bottom {
      font-size: 12px;
      opacity: 0.8;
      border-top: 1px solid rgba(255,255,255,0.2);
      padding-top: 10px;
    }
  }

  .card-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
  }

  .action-list {
    display: flex;
    flex-direction: column;
    gap: 15px;
    .action-btn {
      margin: 0;
      justify-content: flex-start;
      padding-left: 20px;
    }
  }
}
</style>