<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'

interface Overview {
  users: number
  openTeams: number
  totalTeams: number
  activeRides: number
  totalRides: number
  latestLocationRows: number
}

const loading = ref(false)
const error = ref('')
const overview = ref<Overview>({
  users: 0,
  openTeams: 0,
  totalTeams: 0,
  activeRides: 0,
  totalRides: 0,
  latestLocationRows: 0,
})

const cards = computed(() => [
  { label: '注册用户', value: overview.value.users, helper: '开发态登录产生' },
  { label: '开放车队', value: overview.value.openTeams, helper: `累计 ${overview.value.totalTeams}` },
  { label: '进行中骑行', value: overview.value.activeRides, helper: `累计 ${overview.value.totalRides}` },
  { label: '在线位置', value: overview.value.latestLocationRows, helper: '最近 90 秒上报' },
])

async function refresh() {
  loading.value = true
  error.value = ''
  try {
    const response = await axios.get<Overview>('/api/v1/admin/overview')
    overview.value = response.data
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(refresh)
</script>

<template>
  <el-container class="layout">
    <el-aside width="240px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">M</div>
        <div>
          <strong>MotoLink</strong>
          <span>Operations MVP</span>
        </div>
      </div>

      <el-menu default-active="dashboard" class="menu">
        <el-menu-item index="dashboard">运行概览</el-menu-item>
        <el-menu-item index="users" disabled>用户管理 · 下一阶段</el-menu-item>
        <el-menu-item index="teams" disabled>车队监控 · 下一阶段</el-menu-item>
        <el-menu-item index="moderation" disabled>内容审核 · 下一阶段</el-menu-item>
      </el-menu>
    </el-aside>

    <el-main class="main">
      <header class="topbar">
        <div>
          <p class="eyebrow">MVP CONTROL CENTER</p>
          <h1>运行概览</h1>
          <p>先验证车队、麦权、位置与轨迹闭环，再扩展社区和运营能力。</p>
        </div>
        <el-button type="primary" :loading="loading" @click="refresh">刷新数据</el-button>
      </header>

      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        class="alert"
      />

      <section class="metric-grid">
        <article v-for="card in cards" :key="card.label" class="metric-card">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.helper }}</small>
        </article>
      </section>

      <section class="panel-grid">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-title">
              <span>关键链路</span>
              <el-tag type="success" effect="plain">Scaffold</el-tag>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item timestamp="1" type="success">开发态手机号登录</el-timeline-item>
            <el-timeline-item timestamp="2" type="success">创建或加入车队</el-timeline-item>
            <el-timeline-item timestamp="3" type="success">Redis 半双工麦权租约</el-timeline-item>
            <el-timeline-item timestamp="4" type="success">位置与轨迹写入 PostgreSQL</el-timeline-item>
            <el-timeline-item timestamp="5" type="warning">接入真实 RTC 与实机 POC</el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-title">
              <span>生产化提醒</span>
              <el-tag type="warning" effect="plain">Not production ready</el-tag>
            </div>
          </template>
          <ul class="risk-list">
            <li>当前 RTC Token 为 mock，不传输真实音频。</li>
            <li>WebSocket 尚未做 JWT、成员鉴权和频率限制。</li>
            <li>后台音频、后台定位和头盔蓝牙必须实机验证。</li>
            <li>陌生人附近定位需要模糊化、限频和隐私授权。</li>
          </ul>
        </el-card>
      </section>
    </el-main>
  </el-container>
</template>
