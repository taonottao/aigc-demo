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
          <div style="display: grid; grid-template-columns: 1fr auto; gap: 10px; align-items: center;">
            <input v-model="form.captcha" />
            <button class="btn" @click="refreshCaptcha">刷新：{{ captchaCode }}</button>
          </div>
        </div>
        <div class="actions">
          <button class="btn primary" @click="login">登录并进入系统</button>
        </div>
        <p v-if="errorMessage" class="small" style="color: var(--danger)">{{ errorMessage }}</p>
      </section>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCaptcha, login as loginApi } from '../api/auth'

const router = useRouter()

const form = reactive({
  username: 'smile-admin',
  password: '123456',
  captcha: '',
  captchaId: ''
})

const captchaCode = ref('')
const errorMessage = ref('')

async function refreshCaptcha() {
  const data = await getCaptcha()
  form.captchaId = data.captchaId
  captchaCode.value = data.code
}

async function login() {
  if (!form.username || !form.password || !form.captcha) {
    alert('请输入完整登录信息')
    return
  }
  errorMessage.value = ''
  try {
    const res = await loginApi({
      username: form.username,
      password: form.password,
      captchaId: form.captchaId,
      captcha: form.captcha
    })
    localStorage.setItem('token', res.token)
    localStorage.setItem('menus', JSON.stringify(res.menus || []))
    localStorage.setItem('permissions', JSON.stringify(res.permissions || []))
    router.push('/dashboard')
  } catch (err) {
    errorMessage.value = err.message
    await refreshCaptcha()
  }
}

onMounted(refreshCaptcha)
</script>
