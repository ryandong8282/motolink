<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type RoomItem } from '../api'

const rooms = ref<RoomItem[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    rooms.value = (await api.get<RoomItem[]>('/api/v1/admin/rooms')).data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <h1 class="page-title">车队房间</h1>
  <el-card v-loading="loading">
    <el-table :data="rooms" empty-text="暂无房间">
      <el-table-column prop="roomCode" label="房间号" width="120" />
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column label="成员" width="100">
        <template #default="scope">{{ scope.row.members.length }} / {{ scope.row.maxMembers }}</template>
      </el-table-column>
      <el-table-column label="公开" width="90">
        <template #default="scope">{{ scope.row.publicRoom ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createdAt" label="创建时间" min-width="220" />
    </el-table>
  </el-card>
</template>
