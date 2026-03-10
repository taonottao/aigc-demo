<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand"><span class="brand-dot" /> Admin Matrix</div>
      <nav class="nav">
        <RouterLink to="/dashboard">系统总览</RouterLink>
        <RouterLink to="/organizations">组织管理</RouterLink>
        <RouterLink to="/users">用户管理</RouterLink>
        <RouterLink to="/role-permission">角色权限</RouterLink>
        <RouterLink to="/security">日志与安全</RouterLink>
        <a href="#" @click.prevent="logout">退出登录</a>
      </nav>
    </aside>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'

const router = useRouter()

function logout() {
  localStorage.removeItem('loggedIn')
  router.push('/login')
}
</script>
