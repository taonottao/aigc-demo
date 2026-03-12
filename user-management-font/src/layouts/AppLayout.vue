<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand-block">
        <div class="brand"><span class="brand-dot" /> Admin Matrix</div>
        <p class="brand-subtitle">统一组织、用户、权限与审计能力的管理中台</p>
      </div>
      <nav class="nav">
        <RouterLink v-for="m in menus" :key="m.id" :to="m.path" class="nav-link">{{ m.name }}</RouterLink>
        <a href="#" class="nav-link nav-logout" @click.prevent="logout">退出登录</a>
      </nav>
      <div class="sidebar-foot">
        <p>Workspace</p>
        <h4>Production Ready</h4>
      </div>
    </aside>
    <main class="main">
      <section class="page-chrome">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'

const router = useRouter()
const menus = ref([
  { id: 'dashboard', name: '系统总览', path: '/dashboard' },
  { id: 'organizations', name: '组织管理', path: '/organizations' },
  { id: 'users', name: '用户管理', path: '/users' },
  { id: 'role-permission', name: '角色权限', path: '/role-permission' },
  { id: 'security', name: '日志与安全', path: '/security' }
])

onMounted(() => {
  try {
    const stored = JSON.parse(localStorage.getItem('menus') || '[]')
    if (Array.isArray(stored) && stored.length) {
      menus.value = stored.filter((m) => m?.path)
    }
  } catch (_) {
    // ignore
  }
})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('menus')
  localStorage.removeItem('permissions')
  router.push('/login')
}
</script>
