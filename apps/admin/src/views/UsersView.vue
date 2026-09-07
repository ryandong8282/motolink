<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type UserItem } from '../api'

const users = ref<UserItem[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    users.value = (await api.get<UserItem[]>('/api/v1/admin/users')).data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <h1 class="page-title">用户管理</h1>
  <el-card v-loading="loading">
    <el-table :data="users" empty-text="暂无用户">
      <el-table-column prop="nickname" label="昵称" min-width="140" />
      <el-table-column prop="phone" label="手机号" width="160" />
      <el-table-column prop="motorcycle" label="座驾" min-width="180" />
      <el-table-column prop="createdAt" label="注册时间" min-width="220" />
      <el-table-column label="操作" width="130">
        <template #default>
          <el-button size="small" disabled>查看（待做）</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
