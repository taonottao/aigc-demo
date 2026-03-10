<template>
  <main class="login-wrap">
    <section class="login-panel">
      <aside class="login-left">
        <h1>权限管理系统</h1>
        <p>Spring Boot 3 + JDK 21 + PostgreSQL + Vue 3</p>
        <p class="small">支持用户、组织、角色权限、日志与安全能力。</p>
      </aside>
      <section class="login-right">
        <h2>账号登录</h2>
        <div class="field">
          <label>用户名</label>
          <input v-model="form.username" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="form.password" type="password" />
        </div>
        <div class="field">
          <label>验证码</label>
          <input v-model="form.captcha" />
        </div>
        <div class="actions">
          <button class="btn primary" @click="login">登录并进入系统</button>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const form = reactive({
  username: 'smile-admin',
  password: '123456',
  captcha: 'A7P9'
})

function login() {
  if (!form.username || !form.password || !form.captcha) {
    alert('请输入完整登录信息')
    return
  }
  localStorage.setItem('loggedIn', '1')
  router.push('/dashboard')
}
</script>
