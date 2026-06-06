<template>
  <div class="algo-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">图像分割算法 — 医学影像辅助诊断</span>
          <el-tag type="danger">U-Net / SAM</el-tag>
        </div>
      </template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>对老人医学影像进行分割处理，辅助医生精准诊断病灶区域和组织结构。</template>
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
              <el-icon size="48" color="#f5222d"><UploadFilled /></el-icon>
              <div class="el-upload__text">上传医学影像</div>
              <template #tip><div class="el-upload__tip">支持 CT、X光、MRI 等医学影像图片</div></template>
            </el-upload>
          </div>
          <div class="controls" v-if="imageSrc">
            <el-form label-width="80px" size="small">
              <el-form-item label="透明度">
                <el-slider v-model="maskOpacity" :min="0" :max="100" @input="redraw" />
              </el-form-item>
              <el-form-item label="分割模式">
                <el-radio-group v-model="segMode" @change="redraw">
                  <el-radio-button label="organ">器官分割</el-radio-button>
                  <el-radio-button label="lesion">病灶检测</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>
        </el-col>
        <el-col :span="14">
          <div class="result-area" v-loading="processing">
            <h4>分割结果</h4>
            <div class="canvas-wrapper" v-if="imageSrc">
              <canvas ref="canvasRef" class="seg-canvas"></canvas>
            </div>
            <el-empty v-else description="请上传医学影像进行分割" />

            <div v-if="segResults.length" class="seg-legend">
              <h4 style="margin-top:16px">分割区域说明</h4>
              <el-table :data="segResults" stripe border size="small">
                <el-table-column label="颜色" width="60">
                  <template #default="{ row }">
                    <div class="color-dot" :style="{ background: row.color }"></div>
                  </template>
                </el-table-column>
                <el-table-column prop="region" label="区域名称" />
                <el-table-column prop="area" label="面积占比">
                  <template #default="{ row }">
                    <el-progress :percentage="row.area" :color="row.color" :stroke-width="10" />
                  </template>
                </el-table-column>
                <el-table-column prop="diagnosis" label="诊断提示" />
              </el-table>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { algorithmApi } from '@/api/algorithm'

const canvasRef = ref(null)
const imageSrc = ref(null)
const processing = ref(false)
const maskOpacity = ref(40)
const segMode = ref('organ')
const segResults = ref([])
let originalImage = null

function handleFileChange(file) {
  const reader = new FileReader()
  reader.onload = async (e) => {
    imageSrc.value = e.target.result
    processing.value = true
    await nextTick()
    loadAndProcess(e.target.result)
  }
  reader.readAsDataURL(file.raw)
}

async function loadAndProcess(src) {
  const img = new Image()
  img.onload = async () => {
    originalImage = img
    await fetchSegmentation(src)
    processing.value = false
    redraw()
  }
  img.src = src
}

function redraw() {
  if (!originalImage || !canvasRef.value) return
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  const scale = Math.min(520 / originalImage.width, 420 / originalImage.height, 1)
  canvas.width = originalImage.width * scale
  canvas.height = originalImage.height * scale

  ctx.drawImage(originalImage, 0, 0, canvas.width, canvas.height)

  const alpha = maskOpacity.value / 100

  if (segMode.value === 'organ') {
    drawOrganSegmentation(ctx, canvas.width, canvas.height, alpha)
  } else {
    drawLesionDetection(ctx, canvas.width, canvas.height, alpha)
  }
}

async function fetchSegmentation(base64) {
  try {
    const res = await algorithmApi.segment({ image: base64 })
    const organ = res.data?.organSegments || []
    const lesion = res.data?.lesionSegments || []
    const merged = [...organ, ...lesion]
    segResults.value = merged.length ? merged : []
  } catch (e) {
    ElMessage.error('分割接口不可用，使用本地示例数据')
    segResults.value = [
      { region: '心脏区域', color: 'rgba(245,34,45,0.8)', area: 18, diagnosis: '心影大小正常', type: 'ellipse', cx: 0.45, cy: 0.4, rx: 0.12, ry: 0.15 },
      { region: '左肺', color: 'rgba(24,144,255,0.8)', area: 28, diagnosis: '左肺野清晰', type: 'ellipse', cx: 0.65, cy: 0.4, rx: 0.18, ry: 0.3 },
      { region: '右肺', color: 'rgba(82,196,26,0.8)', area: 30, diagnosis: '右肺野清晰', type: 'ellipse', cx: 0.30, cy: 0.4, rx: 0.18, ry: 0.3 },
      { region: '肋骨区域', color: 'rgba(250,173,20,0.8)', area: 24, diagnosis: '骨骼结构完整', type: 'ellipse', cx: 0.50, cy: 0.35, rx: 0.35, ry: 0.35 },
      { region: '疑似结节', color: 'rgba(245,34,45,0.8)', area: 3, diagnosis: '右肺上叶小结节，建议复查CT', type: 'circle', cx: 0.35, cy: 0.35, r: 0.04 },
      { region: '钙化灶', color: 'rgba(250,173,20,0.8)', area: 2, diagnosis: '左肺下叶钙化灶，考虑陈旧性病变', type: 'circle', cx: 0.60, cy: 0.50, r: 0.03 },
      { region: '正常组织', color: 'rgba(82,196,26,0.8)', area: 95, diagnosis: '大部分肺组织正常', type: 'ellipse', cx: 0.50, cy: 0.40, rx: 0.30, ry: 0.35 }
    ]
  }
}

function drawOrganSegmentation(ctx, w, h, alpha) {
  const organs = segResults.value.filter(r => r.type === 'ellipse')
  organs.forEach(r => {
    const fill = r.color?.replace('0.8', alpha.toString()) || 'rgba(24,144,255,0.4)'
    ctx.beginPath()
    ctx.ellipse(r.cx * w, r.cy * h, r.rx * w, r.ry * h, 0, 0, Math.PI * 2)
    ctx.fillStyle = fill
    ctx.fill()
    ctx.strokeStyle = r.color || 'rgba(24,144,255,0.8)'
    ctx.lineWidth = 2
    ctx.stroke()
  })
}

function drawLesionDetection(ctx, w, h, alpha) {
  const lesions = segResults.value.filter(r => r.type === 'circle')
  lesions.forEach(l => {
    const rPixel = (l.r || 0.03) * Math.min(w, h)
    ctx.beginPath()
    ctx.arc(l.cx * w, l.cy * h, rPixel, 0, Math.PI * 2)
    ctx.fillStyle = l.color?.replace('0.8', alpha.toString()) || 'rgba(245,34,45,0.15)'
    ctx.fill()
    ctx.strokeStyle = l.color || 'rgba(245,34,45,1)'
    ctx.lineWidth = 2
    ctx.stroke()

    const endX = l.cx * w + 60
    const endY = l.cy * h - 30
    ctx.beginPath()
    ctx.moveTo(l.cx * w + rPixel, l.cy * h)
    ctx.lineTo(endX, endY)
    ctx.strokeStyle = l.color || 'rgba(245,34,45,0.8)'
    ctx.lineWidth = 1
    ctx.stroke()
    ctx.fillStyle = l.color || 'rgba(245,34,45,1)'
    ctx.font = '12px sans-serif'
    ctx.fillText(l.region, endX + 4, endY)
  })

  // 更新表格内容
  if (!segResults.value.length) {
    segResults.value = lesions
  }
}
</script>

<style scoped>
.algo-page { padding: 12px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-weight: 700; font-size: 17px; letter-spacing: 0.2px; color: #0f1f2b; }
.upload-area { min-height: 200px; background: linear-gradient(135deg, rgba(15,181,195,0.08), rgba(15,124,144,0.06)); border-radius: 14px; padding: 16px; }
.controls { margin-top: 16px; }
.result-area h4 { margin: 0 0 12px; color: #0f1f2b; }
.canvas-wrapper { border: 1px solid #eee; border-radius: 8px; overflow: hidden; display: inline-block; background: #000; }
.seg-canvas { display: block; max-width: 100%; }
.color-dot { width: 20px; height: 20px; border-radius: 4px; margin: 0 auto; }
</style>
