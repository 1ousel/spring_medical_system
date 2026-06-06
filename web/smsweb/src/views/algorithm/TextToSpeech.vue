<template>
  <div class="algo-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">TTS 语音合成</span>
          <el-tag type="success">Edge TTS</el-tag>
        </div>
      </template>

      <AlgorithmServicePanel />

      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>语速由服务端合成控制；音调、音量在浏览器播放时实时生效。需先启动 Python 服务并保持网络可用。</template>
      </el-alert>

      <el-row :gutter="24">
        <el-col :span="12">
          <h4>预设播报内容</h4>
          <div class="preset-list">
            <el-card
              v-for="(item, idx) in presetItems"
              :key="idx"
              shadow="hover"
              class="preset-card"
              :class="{ playing: playingIdx === idx }"
            >
              <div class="preset-header">
                <el-tag :type="item.tagType" size="small">{{ item.tag }}</el-tag>
                <el-button
                  :type="playingIdx === idx ? 'danger' : 'primary'"
                  size="small"
                  circle
                  :loading="loadingIdx === idx"
                  :disabled="isBusy && loadingIdx !== idx && playingIdx !== idx"
                  @click="togglePreset(idx, item.text)"
                >
                  <el-icon><component :is="playingIdx === idx ? 'VideoPause' : 'VideoPlay'" /></el-icon>
                </el-button>
              </div>
              <p class="preset-text">{{ item.text }}</p>
            </el-card>
          </div>
        </el-col>

        <el-col :span="12">
          <h4>自定义语音合成</h4>
          <el-form label-width="80px">
            <el-form-item label="播报内容">
              <el-input v-model="customText" type="textarea" :rows="4" placeholder="输入需要转换为语音的文本..." />
            </el-form-item>
            <el-form-item :label="`语速 ${speechRate.toFixed(1)}x`">
              <el-slider v-model="speechRate" :min="0.5" :max="2" :step="0.1" show-stops />
            </el-form-item>
            <el-form-item :label="`音调 ${speechPitch.toFixed(1)}x`">
              <el-slider v-model="speechPitch" :min="0.5" :max="2" :step="0.1" show-stops />
            </el-form-item>
            <el-form-item :label="`音量 ${Math.round(speechVolume * 100)}%`">
              <el-slider v-model="speechVolume" :min="0.1" :max="1.5" :step="0.1" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="customLoading" :disabled="!customText.trim() || isBusy" @click="speakCustom">
                <el-icon><Headset /></el-icon> 开始播报
              </el-button>
              <el-button :disabled="!isBusy" @click="stopSpeaking">
                <el-icon><SwitchButton /></el-icon> 停止
              </el-button>
            </el-form-item>
          </el-form>

          <h4 style="margin-top:16px">播报历史</h4>
          <el-table :data="speakHistory" stripe size="small" max-height="200">
            <el-table-column prop="time" label="时间" width="100" />
            <el-table-column prop="text" label="内容" show-overflow-tooltip />
          </el-table>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { algorithmApi } from '@/api/algorithm'
import AlgorithmServicePanel from '@/components/AlgorithmServicePanel.vue'
import { ElMessage } from 'element-plus'

const customText = ref('')
const speechRate = ref(1)
const speechPitch = ref(1)
const speechVolume = ref(1)
const playingIdx = ref(-1)
const loadingIdx = ref(-1)
const customLoading = ref(false)
const speakHistory = ref([])

let audioContext = null
let currentSource = null
let currentGain = null

const isBusy = computed(() => customLoading.value || loadingIdx.value >= 0 || playingIdx.value >= 0)

const presetItems = [
  { tag: '高危预警', tagType: 'danger', text: '紧急通知：A101-1床张桂英老人血压异常，收缩压165毫米汞柱，已超过警戒值，请医护人员立即前往处理。' },
  { tag: '高危预警', tagType: 'danger', text: '紧急通知：A102-2床李福贵老人检测到跌倒行为，请立即确认老人安全状况并采取救护措施。' },
  { tag: '评估报告', tagType: 'warning', text: '评估播报：赵国庆老人跌倒风险评估得分45分，等级为差，跌倒风险较高，建议加强看护并使用辅助行走器具。' },
  { tag: '健康提醒', tagType: 'success', text: '健康播报：张桂英老人本月生活能力评估得分82分，等级良好，生活自理能力正常，请继续保持当前护理方案。' },
  { tag: '系统通知', tagType: 'info', text: '系统通知：当前有5条待处理预警，3台设备离线或故障，请管理人员及时关注。' }
]

function getAudioContext() {
  if (!audioContext) {
    const Ctx = window.AudioContext || window.webkitAudioContext
    audioContext = new Ctx()
  }
  return audioContext
}

function pitchToDetune(pitch) {
  const cents = (Number(pitch) - 1) * 900
  return Math.max(-900, Math.min(900, cents))
}

function volumeToGain(volume) {
  return Math.max(0.05, Math.min(1.5, Number(volume)))
}

async function playAudioWithEffects(src, pitch, volume) {
  const ctx = getAudioContext()
  if (ctx.state === 'suspended') {
    await ctx.resume()
  }

  const response = await fetch(src)
  const arrayBuffer = await response.arrayBuffer()
  const audioBuffer = await ctx.decodeAudioData(arrayBuffer)

  const source = ctx.createBufferSource()
  source.buffer = audioBuffer
  source.detune.value = pitchToDetune(pitch)

  const gainNode = ctx.createGain()
  gainNode.gain.value = volumeToGain(volume)

  source.connect(gainNode)
  gainNode.connect(ctx.destination)

  currentSource = source
  currentGain = gainNode

  return new Promise((resolve, reject) => {
    source.onended = () => resolve()
    source.onerror = (e) => reject(e)
    source.start(0)
  })
}

async function togglePreset(idx, text) {
  if (playingIdx.value === idx) {
    stopSpeaking()
    return
  }
  await playText(text, idx)
}

async function speakCustom() {
  const text = customText.value.trim()
  if (!text) return
  await playText(text, -1, true)
}

async function playText(text, presetIdx = -1, isCustom = false) {
  stopSpeaking()
  if (isCustom) {
    customLoading.value = true
  } else {
    loadingIdx.value = presetIdx
  }

  try {
    const res = await algorithmApi.tts({
      text,
      rate: speechRate.value,
      pitch: speechPitch.value,
      volume: speechVolume.value
    })

    const src = res.data?.audioBase64
    if (!src) {
      throw new Error('未返回音频数据')
    }

    if (presetIdx >= 0) {
      playingIdx.value = presetIdx
    }

    await playAudioWithEffects(src, speechPitch.value, speechVolume.value)

    speakHistory.value.unshift({
      time: new Date().toLocaleTimeString(),
      text: text.length > 50 ? `${text.slice(0, 50)}...` : text
    })
  } catch {
    resetPlayState()
  } finally {
    customLoading.value = false
    loadingIdx.value = -1
    if (playingIdx.value === presetIdx || presetIdx < 0) {
      resetPlayState()
    }
  }
}

function resetPlayState() {
  playingIdx.value = -1
  loadingIdx.value = -1
  customLoading.value = false
}

function stopSpeaking() {
  if (currentSource) {
    try {
      currentSource.stop()
    } catch {
      // ignore if already stopped
    }
    currentSource.disconnect()
    currentSource = null
  }
  if (currentGain) {
    currentGain.disconnect()
    currentGain = null
  }
  resetPlayState()
}

onUnmounted(() => {
  stopSpeaking()
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }
})
</script>

<style scoped>
.algo-page { padding: 12px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-weight: 700; font-size: 17px; letter-spacing: 0.2px; color: #0f1f2b; }
h4 { margin: 0 0 12px; color: #0f1f2b; }

.preset-list { display: flex; flex-direction: column; gap: 12px; }
.preset-card { transition: all 0.3s; border-radius: 12px; }
.preset-card.playing { border-color: #0fb5c3; box-shadow: 0 0 10px rgba(15, 181, 195, 0.28); }
.preset-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.preset-text { font-size: 13px; color: #666; margin: 0; line-height: 1.6; }
</style>
