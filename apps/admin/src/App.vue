<template>
  <el-container class="layout">
    <el-aside width="240px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">M</div>
        <div>
          <strong>MotoLink</strong>
          <small>MVP Console</small>
        </div>
      </div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item index="/">数据总览</el-menu-item>
        <el-menu-item index="/users">用户管理</el-menu-item>
        <el-menu-item index="/rooms">车队房间</el-menu-item>
      </el-menu>
      <div class="warning">MVP 管理接口尚未接入管理员认证，请勿直接暴露到公网。</div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div>
          <strong>摩托车友组队平台</strong>
          <span>开发环境</span>
        </div>
      </el-header>
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
