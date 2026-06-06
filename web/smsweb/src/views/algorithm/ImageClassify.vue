<template>
  <div class="algo-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">图像分类算法 — 健康检测图像分析</span>
          <el-tag type="success">ResNet / EfficientNet</el-tag>
        </div>
      </template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>对老人健康检测图像（如舌苔、面色）进行分类分析，辅助医生判断健康状况。</template>
      </el-alert>

      <el-row :gutter="24">
        <el-col :span="10">
          <div class="upload-area">
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              :on-change="handleFileChange"
              drag
            >
              <el-icon size="48" color="#52c41a"><UploadFilled /></el-icon>
              <div class="el-upload__text">上传健康检测图像</div>
              <template #tip><div class="el-upload__tip">支持舌苔、面色、皮肤等健康图像</div></template>
            </el-upload>
            <div v-if="previewSrc" class="preview-img">
              <img :src="previewSrc" alt="预览" />
            </div>
          </div>
        </el-col>
        <el-col :span="14">
          <div class="result-area" v-loading="analyzing">
            <h4>分类分析结果</h4>
            <template v-if="result">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="分类类型">{{ result.category }}</el-descriptions-item>
                <el-descriptions-item label="健康判定">
                  <el-tag :type="result.healthTag">{{ result.healthLabel }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="置信度">
                  <el-progress :percentage="result.confidence" :color="result.confidence > 80 ? '#52c41a' : '#faad14'" />
                </el-descriptions-item>
                <el-descriptions-item label="分析说明">{{ result.description }}</el-descriptions-item>
              </el-descriptions>

              <h4 style="margin-top:16px">分类概率分布</h4>
              <div class="prob-list">
                <div v-for="p in result.probabilities" :key="p.label" class="prob-item">
                  <span class="prob-label">{{ p.label }}</span>
                  <el-progress :percentage="p.value" :stroke-width="14" :color="p.color" style="flex:1" />
                </div>
              </div>
            </template>
            <el-empty v-else description="请上传图像进行分析" />
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { algorithmApi } from '@/api/algorithm'

const previewSrc = ref(null)
const analyzing = ref(false)
const result = ref(null)

function handleFileChange(file) {
  const reader = new FileReader()
  reader.onload = (e) => {
    previewSrc.value = e.target.result
    analyzeImage(e.target.result)
  }
  reader.readAsDataURL(file.raw)
}

async function analyzeImage(base64) {
  analyzing.value = true
  result.value = null
  try {
    const res = await algorithmApi.classify({ image: base64 })
    result.value = res.data
  } catch (e) {
    ElMessage.error('分类接口不可用，使用本地示例数据')
    const fallback = [
      {
        category: '舌苔分析 - 淡白舌',
        healthLabel: '气血不足',
        healthTag: 'warning',
        confidence: 87,
        description: '舌色偏淡，舌苔薄白，提示可能存在气血不足、脾虚的情况，建议进一步检查血常规，注意饮食调养。',
        probabilities: [
          { label: '淡白舌(气血不足)', value: 87, color: '#faad14' },
          { label: '红舌(热证)', value: 8, color: '#f5222d' },
          { label: '正常舌色', value: 3, color: '#52c41a' },
          { label: '紫暗舌(血瘀)', value: 2, color: '#722ed1' }
        ]
      },
      {
        category: '面色分析 - 面色萎黄',
        healthLabel: '脾胃虚弱',
        healthTag: 'warning',
        confidence: 82,
        description: '面色偏黄少华，提示脾胃运化功能减弱，营养吸收不佳，建议加强营养评估。',
        probabilities: [
          { label: '面色萎黄(脾虚)', value: 82, color: '#faad14' },
          { label: '面色红润(正常)', value: 10, color: '#52c41a' },
          { label: '面色苍白(贫血)', value: 5, color: '#1890ff' },
          { label: '面色晦暗(肾虚)', value: 3, color: '#722ed1' }
        ]
      },
      {
        category: '舌苔分析 - 正常舌象',
        healthLabel: '健康',
        healthTag: 'success',
        confidence: 92,
        description: '舌色淡红，苔薄白润，舌体大小适中，为正常舌象，提示身体状况良好。',
        probabilities: [
          { label: '正常舌象', value: 92, color: '#52c41a' },
          { label: '淡白舌(气虚)', value: 4, color: '#faad14' },
          { label: '红舌(热证)', value: 3, color: '#f5222d' },
          { label: '紫暗舌(血瘀)', value: 1, color: '#722ed1' }
        ]
      }
    ]
    result.value = fallback[Math.floor(Math.random() * fallback.length)]
  }
  analyzing.value = false
}
</script>

<style scoped>
.algo-page { padding: 12px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-weight: 700; font-size: 17px; letter-spacing: 0.2px; color: #0f1f2b; }
.upload-area { min-height: 200px; background: linear-gradient(135deg, rgba(15,181,195,0.08), rgba(15,124,144,0.06)); border-radius: 14px; padding: 16px; }
.preview-img { margin-top: 16px; text-align: center; }
.preview-img img { max-width: 100%; max-height: 250px; border-radius: 8px; border: 1px solid #eee; }
.result-area h4 { margin: 0 0 12px; color: #0f1f2b; }
.prob-list { display: flex; flex-direction: column; gap: 8px; }
.prob-item { display: flex; align-items: center; gap: 12px; }
.prob-label { width: 140px; font-size: 13px; color: #666; text-align: right; flex-shrink: 0; }
</style>
