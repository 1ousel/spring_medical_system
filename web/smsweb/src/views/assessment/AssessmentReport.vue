<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="老人姓名">
          <el-input v-model="searchName" placeholder="请输入姓名" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="评估类型">
          <el-select v-model="searchType" clearable placeholder="全部" style="width:160px">
            <el-option v-for="t in assessTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button :icon="Plus" type="success" @click="openDialog()">新建评估</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top:12px">
      <el-table :data="reportList" border stripe>
        <el-table-column type="index" width="60" align="center" />
        <el-table-column prop="elderName" label="老人姓名" width="100" />
        <el-table-column prop="assessType" label="评估类型" width="160" />
        <el-table-column prop="assessDate" label="评估日期" width="120" />
        <el-table-column prop="evaluator" label="评估医生" width="100" />
        <el-table-column prop="score" label="综合得分" width="100" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.score" :stroke-width="12"
              :color="row.score >= 80 ? '#52c41a' : row.score >= 60 ? '#fa8c16' : '#ff4d4f'"
              :show-text="false" style="width:60px;display:inline-block" />
            <span style="margin-left:8px;font-weight:600">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="评估等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === '优' ? 'success' : row.level === '良' ? 'primary' : row.level === '中' ? 'warning' : 'danger'">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="conclusion" label="评估结论" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" width="130" align="center">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="viewReport(row)">查看</el-button>
            <el-button text size="small" type="warning" @click="openDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size"
          :total="total" layout="total, prev, pager, next" background small @change="loadList" />
      </div>
    </el-card>

    <el-dialog v-model="reportDialogVisible" title="评估报告详情" width="720px">
      <div v-if="currentReport">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="老人姓名">{{ currentReport.elderName }}</el-descriptions-item>
          <el-descriptions-item label="评估类型">{{ currentReport.assessType }}</el-descriptions-item>
          <el-descriptions-item label="评估日期">{{ currentReport.assessDate }}</el-descriptions-item>
          <el-descriptions-item label="评估医生">{{ currentReport.evaluator }}</el-descriptions-item>
          <el-descriptions-item label="综合得分">{{ currentReport.score }}</el-descriptions-item>
          <el-descriptions-item label="评估等级"><el-tag>{{ currentReport.level }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="评估结论" :span="3">{{ currentReport.conclusion }}</el-descriptions-item>
        </el-descriptions>
        <el-divider>评估详情</el-divider>
        <el-table :data="currentReport.details" border size="small">
          <el-table-column prop="item" label="评估项目" />
          <el-table-column prop="score" label="得分" width="80" align="center" />
          <el-table-column prop="maxScore" label="满分" width="80" align="center" />
          <el-table-column prop="remark" label="备注" />
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" :title="editingReport ? '编辑评估报告' : '新建评估报告'" width="560px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" label-width="100px">
        <el-form-item label="老人姓名" required>
          <el-select v-model="editForm.elderName" filterable style="width:100%">
            <el-option v-for="e in elders" :key="e.id" :label="e.name" :value="e.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估类型" required>
          <el-select v-model="editForm.assessType" style="width:100%">
            <el-option v-for="t in assessTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估日期">
          <el-date-picker v-model="editForm.assessDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="评估医生">
          <el-select v-model="editForm.evaluator" filterable style="width:100%">
            <el-option v-for="d in doctors" :key="d.id" :label="d.realName" :value="d.realName" />
          </el-select>
        </el-form-item>
        <el-form-item label="综合得分">
          <el-input-number v-model="editForm.score" :min="0" :max="100" style="width:100%" />
        </el-form-item>
        <el-form-item label="评估结论">
          <el-input v-model="editForm.conclusion" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveReport">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { assessmentApi } from '@/api/assessment'
import { elderApi } from '@/api/elder'
import { userApi } from '@/api/user'

const searchName = ref('')
const searchType = ref('')
const reportDialogVisible = ref(false)
const editDialogVisible = ref(false)
const currentReport = ref(null)
const editingReport = ref(null)
const editFormRef = ref()
const loading = ref(false)
const page = reactive({ current: 1, size: 10 })
const total = ref(0)

const assessTypes = ['ADL生活能力评估', '认知功能评估(MMSE)', '营养状况评估(MNA)', '跌倒风险评估', '心理健康评估', '综合能力评估']
const elders = ref([])
const doctors = ref([])
const editForm = reactive({ elderName: '', assessType: '', assessDate: '', evaluator: '', score: 80, conclusion: '' })
const reportList = ref([])

async function loadList() {
  loading.value = true
  try {
    const res = await assessmentApi.list({ page: page.current, size: page.size, elderName: searchName.value, assessType: searchType.value })
    reportList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function loadOptions() {
  const [elderRes, doctorRes] = await Promise.all([elderApi.list({ page: 1, size: 100 }), userApi.getDoctors()])
  elders.value = elderRes.data?.records || []
  doctors.value = doctorRes.data || []
}

function viewReport(row) { currentReport.value = row; reportDialogVisible.value = true }
function openDialog(row = null) {
  editingReport.value = row
  if (row) Object.assign(editForm, { elderName: row.elderName, assessType: row.assessType, assessDate: row.assessDate, evaluator: row.evaluator, score: row.score, conclusion: row.conclusion })
  else Object.assign(editForm, { elderName: '', assessType: '', assessDate: '', evaluator: '', score: 80, conclusion: '' })
  editDialogVisible.value = true
}
async function saveReport() {
  try {
    if (editingReport.value) {
      await assessmentApi.update(editingReport.value.id, editForm)
      ElMessage.success('修改成功')
    } else {
      await assessmentApi.save(editForm)
      ElMessage.success('新建成功')
    }
    editDialogVisible.value = false
    loadList()
  } catch {}
}
function deleteReport(row) {
  ElMessageBox.confirm(`确定删除该报告？`, '提示', { type: 'warning' })
    .then(() => assessmentApi.remove(row.id).then(() => { ElMessage.success('删除成功'); loadList() }))
}

onMounted(() => { loadList(); loadOptions() })
</script>

<style scoped>
.search-card :deep(.el-card__body) { padding: 16px 20px 0; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
