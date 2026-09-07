<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type Overview, type RoomItem } from '../api'

const overview = ref<Overview>({ users: 0, rooms: 0, activeRides: 0 })
const rooms = ref<RoomItem[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const [overviewResponse, roomsResponse] = await Promise.all([
      api.get<Overview>('/api/v1/admin/overview'),
      api.get<RoomItem[]>('/api/v1/admin/rooms'),
    ])
    overview.value = overviewResponse.data
    rooms.value = roomsResponse.data.slice(0, 5)
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <h1 class="page-title">数据总览</h1>
  <el-alert v-if="error" :title="error" type="error" show-icon class="section" />
  <div v-loading="loading" class="metric-grid">
    <el-card><div>注册用户</div><div class="metric-value">{{ overview.users }}</div></el-card>
    <el-card><div>已创建车队</div><div class="metric-value">{{ overview.rooms }}</div></el-card>
    <el-card><div>正在骑行</div><div class="metric-value">{{ overview.activeRides }}</div></el-card>
  </div>

  <el-card class="section">
    <template #header><strong>最近车队</strong></template>
    <el-table :data="rooms" empty-text="暂无数据">
      <el-table-column prop="roomCode" label="房间号" width="120" />
      <el-table-column prop="name" label="车队名称" />
      <el-table-column label="在线成员" width="120">
        <template #default="scope">{{ scope.row.members.length }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
    </el-table>
  </el-card>
</template>
