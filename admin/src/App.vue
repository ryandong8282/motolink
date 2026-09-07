<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

interface Team {
  id: string
  roomCode: string
  name: string
  routeNote: string
  capacity: number
  memberCount: number
  privateRoom: boolean
  createdAt: string
}

const loading = ref(true)
const apiOnline = ref(false)
const teams = ref<Team[]>([
  {
    id: 'demo-team',
    roomCode: '520131',
    name: '周末环湖小队',
    routeNote: '城北集合，沿湖骑行约 45km',
    capacity: 12,
    memberCount: 3,
    privateRoom: false,
    createdAt: new Date().toISOString(),
  },
])

const onlineMembers = computed(() =>
  teams.value.reduce((sum, team) => sum + team.memberCount, 0),
)

onMounted(async () => {
  try {
    const response = await fetch('/api/v1/teams')
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const payload = (await response.json()) as Team[]
    teams.value = payload
    apiOnline.value = true
  } catch {
    apiOnline.value = false
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">M</span>
        <div>
          <strong>MotoLink</strong>
          <small>运营管理台</small>
        </div>
      </div>
      <nav>
        <button class="nav-item active">实时概览</button>
        <button class="nav-item">用户管理</button>
        <button class="nav-item">车队房间</button>
        <button class="nav-item">内容审核</button>
        <button class="nav-item">系统配置</button>
      </nav>
      <div class="scope-note">
        <strong>MVP 范围</strong>
        <span>当前页面只提供运营骨架，权限、审核流和统计口径在第二阶段接入。</span>
      </div>
    </aside>

    <main>
      <header>
        <div>
          <p class="eyebrow">OPERATIONS</p>
          <h1>骑行实时概览</h1>
          <p>查看当前车队、在线成员和服务连接状态。</p>
        </div>
        <div :class="['status', apiOnline ? 'online' : 'demo']">
          <span></span>
          {{ apiOnline ? 'API 已连接' : '演示数据' }}
        </div>
      </header>

      <section class="metrics">
        <article>
          <span>活跃车队</span>
          <strong>{{ teams.length }}</strong>
          <small>当前未解散房间</small>
        </article>
        <article>
          <span>在线成员</span>
          <strong>{{ onlineMembers }}</strong>
          <small>按车队成员汇总</small>
        </article>
        <article>
          <span>对讲状态</span>
          <strong>正常</strong>
          <small>RTC 尚处于 Mock 模式</small>
        </article>
        <article>
          <span>待处理举报</span>
          <strong>0</strong>
          <small>MVP 暂未开放社区</small>
        </article>
      </section>

      <section class="panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">LIVE ROOMS</p>
            <h2>车队房间</h2>
          </div>
          <button @click="location.reload()">刷新</button>
        </div>

        <div v-if="loading" class="empty">正在读取车队数据…</div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>车队</th>
                <th>房间号</th>
                <th>在线</th>
                <th>类型</th>
                <th>路线备注</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="team in teams" :key="team.id">
                <td><strong>{{ team.name }}</strong></td>
                <td><code>{{ team.roomCode }}</code></td>
                <td>{{ team.memberCount }} / {{ team.capacity }}</td>
                <td>{{ team.privateRoom ? '私密' : '公开' }}</td>
                <td class="route">{{ team.routeNote || '—' }}</td>
                <td><span class="badge">运行中</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="lower-grid">
        <article class="panel compact">
          <p class="eyebrow">QUALITY</p>
          <h2>实时质量基线</h2>
          <dl>
            <div><dt>抢麦租约</dt><dd>20 秒</dd></div>
            <div><dt>位置在线 TTL</dt><dd>30 秒</dd></div>
            <div><dt>轨迹上传</dt><dd>批量去重</dd></div>
          </dl>
        </article>
        <article class="panel compact warning">
          <p class="eyebrow">BEFORE PRODUCTION</p>
          <h2>上线前置项</h2>
          <p>完成 RTC 实车 POC、后台定位验证、企业资质和隐私合规清单后，再切换生产模式。</p>
        </article>
      </section>
    </main>
  </div>
</template>

<style>
:root {
  font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  color: #eff2f5;
  background: #0b0d10;
  font-synthesis: none;
}

* { box-sizing: border-box; }
body { margin: 0; min-width: 1100px; min-height: 100vh; }
button { font: inherit; }

.layout { display: grid; grid-template-columns: 244px 1fr; min-height: 100vh; }
.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 26px 18px;
  border-right: 1px solid #252931;
  background: #111318;
  display: flex;
  flex-direction: column;
}
.brand { display: flex; gap: 12px; align-items: center; padding: 0 8px 28px; }
.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: #ff6b35;
  color: white;
  font-weight: 900;
}
.brand strong, .brand small { display: block; }
.brand small { margin-top: 3px; color: #7f8794; }
nav { display: grid; gap: 6px; }
.nav-item {
  border: 0;
  border-radius: 12px;
  padding: 12px 14px;
  text-align: left;
  color: #9da5b2;
  background: transparent;
}
.nav-item.active { color: white; background: #242830; }
.scope-note {
  margin-top: auto;
  padding: 15px;
  border: 1px solid #343946;
  border-radius: 14px;
  background: #171a20;
}
.scope-note strong, .scope-note span { display: block; }
.scope-note span { margin-top: 7px; color: #89919f; font-size: 12px; line-height: 1.6; }

main { padding: 34px 42px 60px; }
header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; }
h1 { margin: 4px 0 6px; font-size: 32px; }
h2 { margin: 4px 0 0; font-size: 20px; }
header p { margin: 0; color: #8e97a5; }
.eyebrow { margin: 0; color: #ff8457; font-size: 11px; font-weight: 800; letter-spacing: .13em; }
.status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 13px;
  border-radius: 999px;
  background: #252932;
  color: #b8c0ca;
  font-size: 13px;
}
.status span { width: 8px; height: 8px; border-radius: 50%; background: #f3b461; }
.status.online span { background: #59dc8a; }

.metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 18px; }
.metrics article, .panel {
  border: 1px solid #272b33;
  border-radius: 18px;
  background: #15181d;
}
.metrics article { padding: 18px; }
.metrics span, .metrics small { display: block; color: #858e9b; }
.metrics strong { display: block; margin: 9px 0 5px; font-size: 28px; }
.metrics small { font-size: 12px; }

.panel { padding: 22px; }
.panel-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.panel-heading button {
  border: 1px solid #3a404b;
  border-radius: 10px;
  padding: 8px 13px;
  color: #d9dee5;
  background: #22262d;
  cursor: pointer;
}
.table-wrap { overflow: hidden; border: 1px solid #282d35; border-radius: 13px; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 14px 15px; border-bottom: 1px solid #272b32; text-align: left; }
th { color: #7f8896; background: #1b1e24; font-size: 12px; font-weight: 700; }
td { color: #c9cfd7; font-size: 13px; }
tbody tr:last-child td { border-bottom: 0; }
code { color: #ff9872; }
.route { max-width: 300px; color: #8f98a5; }
.badge { padding: 5px 9px; border-radius: 999px; color: #61dc91; background: #173423; font-size: 12px; }
.empty { padding: 30px; text-align: center; color: #89919f; }

.lower-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin-top: 18px; }
.compact { min-height: 190px; }
dl { margin: 18px 0 0; }
dl div { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #282c34; }
dt { color: #89929f; }
dd { margin: 0; font-weight: 700; }
.warning { border-color: #4b3725; background: #1d1814; }
.warning > p:last-child { color: #b8a99d; line-height: 1.7; }
</style>
