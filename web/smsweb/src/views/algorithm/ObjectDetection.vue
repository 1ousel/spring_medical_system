<template>

  <div class="algo-page">

    <el-card shadow="hover">

      <template #header>

        <div class="card-header">

          <span class="card-title">目标检测算法 — 老人异常行为检测</span>

          <el-tag type="primary">YOLOv8</el-tag>

        </div>

      </template>



      <AlgorithmServicePanel ref="servicePanelRef" />



      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">

        <template #title>通过老人佩戴的设备采集图像，检测老人异常行为（如跌倒、长时间卧床），触发健康预警。请先启动 Python 算法服务，再上传图片进行检测。</template>

      </el-alert>



      <el-row :gutter="24">

        <el-col :span="12">

          <div class="upload-area">

            <el-upload

              :key="uploadKey"

              ref="uploadRef"

              :auto-upload="false"

              :show-file-list="false"

              accept="image/*"

              :on-change="handleFileChange"

              drag

            >

              <el-icon size="48" color="#1890ff"><UploadFilled /></el-icon>

              <div class="el-upload__text">拖拽图片到此处 或 <em>点击上传</em></div>

              <template #tip><div class="el-upload__tip">支持 jpg/png 格式，可重复上传同一张图片</div></template>

            </el-upload>

          </div>

        </el-col>

        <el-col :span="12">

          <div class="result-area">

            <h4>检测结果</h4>

            <div v-if="detecting" class="detect-loading">

              <el-icon class="is-loading" size="32"><Loading /></el-icon>

              <span>正在检测中，请稍候...</span>

            </div>

            <div class="canvas-wrapper" v-else-if="imageSrc">

              <canvas ref="canvasRef" class="detect-canvas"></canvas>

            </div>

            <el-empty v-else description="请上传图片进行检测" />

          </div>

        </el-col>

      </el-row>



      <el-alert

        v-if="alertCreated"

        type="error"

        :closable="false"

        show-icon

        style="margin-top:16px"

        :title="`检测到跌倒！已自动创建健康预警 ${alertCode}`"

      />



      <div v-if="detections.length" class="detection-list">

        <el-table :data="detections" stripe border size="small">

          <el-table-column prop="label" label="检测目标" />

          <el-table-column prop="confidence" label="置信度">

            <template #default="{ row }">

              <el-progress :percentage="Math.round(row.confidence * 100)" :color="row.confidence > 0.8 ? '#f5222d' : '#faad14'" />

            </template>

          </el-table-column>

          <el-table-column prop="action" label="触发预警">

            <template #default="{ row }">

              <el-tag :type="row.alert ? 'danger' : 'success'" size="small">{{ row.alert ? '是' : '否' }}</el-tag>

            </template>

          </el-table-column>

        </el-table>

      </div>

    </el-card>

  </div>

</template>



<script setup>

import { ref, nextTick } from 'vue'

import { ElMessage } from 'element-plus'

import { algorithmApi } from '@/api/algorithm'

import AlgorithmServicePanel from '@/components/AlgorithmServicePanel.vue'



const uploadRef = ref(null)

const canvasRef = ref(null)

const servicePanelRef = ref(null)

const imageSrc = ref(null)

const detections = ref([])

const alertCreated = ref(false)

const alertCode = ref('')

const detecting = ref(false)

const uploadKey = ref(0)



async function handleFileChange(file) {

  if (detecting.value) {

    ElMessage.warning('正在检测中，请稍候')

    return

  }



  const reader = new FileReader()

  reader.onload = async (e) => {

    imageSrc.value = e.target.result

    alertCreated.value = false

    alertCode.value = ''

    detections.value = []

    detecting.value = true



    await nextTick()

    try {

      const result = await fetchDetections(e.target.result)

      detecting.value = false

      await nextTick()

      drawDetections(e.target.result, result.detections)

      if (result.alertCreated) {

        alertCreated.value = true

        alertCode.value = result.alertCode || ''

        ElMessage.error('检测到跌倒行为，已自动创建健康预警')

      }

    } finally {

      detecting.value = false

      uploadKey.value += 1

    }

  }

  reader.readAsDataURL(file.raw)

}



async function fetchDetections(base64) {

  try {

    const res = await algorithmApi.detect({ image: base64 })

    if (res.data?.mock) {

      ElMessage.warning(res.data?.message || '算法服务未启动，当前为演示数据')

    } else if (res.data?.serviceError) {

      ElMessage.error(res.data?.message || '算法服务调用失败')

    }

    return {

      detections: res.data?.detections || [],

      alertCreated: res.data?.alertCreated,

      alertCode: res.data?.alertCode

    }

  } catch (e) {

    ElMessage.error(e.message || '检测接口不可用')

    return { detections: [] }

  }

}



function drawDetections(src, detectionList) {

  const canvas = canvasRef.value

  if (!canvas) return

  const ctx = canvas.getContext('2d')

  const img = new Image()

  img.onload = () => {

    const scale = Math.min(500 / img.width, 400 / img.height, 1)

    canvas.width = img.width * scale

    canvas.height = img.height * scale

    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)



    const finalDetections = detectionList && detectionList.length ? detectionList : []

    finalDetections.forEach(d => {

      const x = d.x * canvas.width

      const y = d.y * canvas.height

      const w = d.w * canvas.width

      const h = d.h * canvas.height

      ctx.strokeStyle = d.alert ? '#f5222d' : '#52c41a'

      ctx.lineWidth = 2

      ctx.strokeRect(x, y, w, h)

      ctx.fillStyle = d.alert ? 'rgba(245,34,45,0.15)' : 'rgba(82,196,26,0.1)'

      ctx.fillRect(x, y, w, h)

      ctx.fillStyle = d.alert ? '#f5222d' : '#52c41a'

      ctx.font = '14px sans-serif'

      ctx.fillText(`${d.label} ${Math.round(d.confidence * 100)}%`, x + 4, y - 6)

    })



    detections.value = finalDetections.map(d => ({

      label: d.label,

      confidence: d.confidence,

      alert: d.alert

    }))

  }

  img.src = src

}

</script>



<style scoped>

.algo-page { padding: 12px; }

.card-header { display: flex; align-items: center; justify-content: space-between; }

.card-title { font-weight: 700; font-size: 17px; letter-spacing: 0.2px; color: #0f1f2b; }

.upload-area { min-height: 300px; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, rgba(15,181,195,0.08), rgba(15,124,144,0.06)); border-radius: 14px; }

.result-area h4 { margin: 0 0 12px; color: #0f1f2b; }

.detect-loading { min-height: 200px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: #64748b; }

.canvas-wrapper { border: 1px solid rgba(15,124,144,0.12); border-radius: 12px; overflow: hidden; display: inline-block; box-shadow: 0 12px 26px rgba(12,125,150,0.12); }

.detect-canvas { display: block; max-width: 100%; }

.detection-list { margin-top: 20px; }

</style>

