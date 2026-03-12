<template>
  <main class="login-page">
    <section class="hero-panel">
      <aside class="brand-column">
        <p class="eyebrow">User Management Platform</p>
        <h1 class="headline">统一账号与权限中台</h1>
        <p class="subtitle">
          简约、稳定、可审计。集中管理组织、用户、角色与权限策略，提升团队协作效率与系统安全性。
        </p>
        <ul class="feature-list">
          <li>组织架构与岗位权限联动</li>
          <li>角色菜单与按钮级授权</li>
          <li>安全策略与操作日志留痕</li>
        </ul>
        <div class="metric-grid">
          <article class="metric-card">
            <p>组织节点</p>
            <h3>1,284</h3>
          </article>
          <article class="metric-card">
            <p>活跃用户</p>
            <h3>9,612</h3>
          </article>
          <article class="metric-card">
            <p>权限策略</p>
            <h3>3,047</h3>
          </article>
        </div>
        <div class="trust-row">
          <span>审计留痕</span>
          <span>双重校验</span>
          <span>分级授权</span>
        </div>
      </aside>
      <section class="form-column">
        <header class="form-header">
          <h2>登录系统</h2>
          <p>请输入账号信息继续访问控制台</p>
        </header>
        <form class="login-form" @submit.prevent="login">
          <label class="field">
            <span>用户名</span>
            <input v-model="form.username" autocomplete="username" />
          </label>
          <label class="field">
            <span>密码</span>
            <input v-model="form.password" type="password" autocomplete="current-password" />
          </label>
          <label class="field">
            <span>验证码</span>
            <div class="captcha-row">
              <input v-model="form.captcha" maxlength="8" />
              <button class="captcha-btn" type="button" @click="refreshCaptcha" title="刷新验证码">
                {{ captchaCode || '点击刷新' }}
              </button>
            </div>
          </label>
          <div class="assist-row">
            <label class="remember">
              <input type="checkbox" v-model="rememberMe" />
              <span>记住登录状态</span>
            </label>
            <button class="link-btn" type="button">无法登录？</button>
          </div>
          <p v-if="errorMessage" class="error-tip">{{ errorMessage }}</p>
          <button class="submit-btn" type="submit" :disabled="loggingIn">
            {{ loggingIn ? '登录中...' : '登录并进入系统' }}
          </button>
          <p class="form-note">登录即表示你同意平台访问与安全策略。</p>
        </form>
        <footer class="form-footer">© {{ currentYear }} User Management Suite</footer>
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
const loggingIn = ref(false)
const rememberMe = ref(true)
const currentYear = new Date().getFullYear()

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
  if (loggingIn.value) {
    return
  }
  loggingIn.value = true
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
  } finally {
    loggingIn.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<style scoped>
.login-page {
  --surface: #f8f8f6;
  --surface-elevated: #ffffff;
  --text: #131313;
  --text-muted: #606060;
  --line: #e7e5e2;
  --accent: #202020;
  --accent-soft: #efedea;
  --danger: #b42318;
  --shadow-lg: 0 30px 70px rgba(17, 17, 17, 0.1);
  --shadow-sm: 0 8px 18px rgba(17, 17, 17, 0.07);
  --space-xs: 8px;
  --space-sm: 12px;
  --space-md: 18px;
  --space-lg: 26px;
  --space-xl: 40px;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 42px;
  background:
    radial-gradient(circle at 7% 12%, #efedea 0%, transparent 37%),
    radial-gradient(circle at 95% 82%, #ece9e5 0%, transparent 35%),
    linear-gradient(165deg, #f7f6f4 0%, #f0efec 100%);
}

.hero-panel {
  width: min(1120px, 100%);
  border-radius: 28px;
  border: 1px solid var(--line);
  background: var(--surface-elevated);
  box-shadow: var(--shadow-lg);
  display: grid;
  grid-template-columns: minmax(420px, 1.05fr) minmax(360px, 0.95fr);
  overflow: hidden;
  animation: rise-in 0.7s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.brand-column {
  position: relative;
  padding: 56px 58px;
  background:
    linear-gradient(128deg, rgba(243, 241, 237, 0.95), rgba(250, 248, 246, 0.96)),
    radial-gradient(circle at 20% 24%, rgba(255, 255, 255, 0.6), transparent 52%);
}

.brand-column::after {
  content: '';
  position: absolute;
  right: 36px;
  bottom: 32px;
  width: 170px;
  height: 170px;
  border-radius: 50%;
  border: 1px solid rgba(22, 22, 22, 0.08);
  background: radial-gradient(circle at 35% 32%, rgba(255, 255, 255, 0.95), rgba(237, 234, 229, 0.55));
}

.eyebrow {
  margin: 0;
  letter-spacing: 0.11em;
  text-transform: uppercase;
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
}

.headline {
  margin: var(--space-md) 0 var(--space-sm);
  font-size: clamp(2rem, 3.2vw, 2.9rem);
  line-height: 1.12;
  letter-spacing: -0.02em;
  color: var(--text);
  font-weight: 700;
}

.subtitle {
  margin: 0;
  max-width: 480px;
  color: var(--text-muted);
  line-height: 1.65;
  font-size: 15px;
}

.feature-list {
  margin: var(--space-xl) 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: var(--space-sm);
}

.feature-list li {
  position: relative;
  padding-left: 22px;
  color: #2f2f2f;
  line-height: 1.5;
}

.feature-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #2d2d2d;
}

.metric-grid {
  margin-top: 34px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  position: relative;
  z-index: 1;
}

.metric-card {
  border: 1px solid #e6e3df;
  border-radius: 14px;
  padding: 12px 12px 10px;
  background: rgba(255, 255, 255, 0.72);
}

.metric-card p {
  margin: 0;
  color: #7b756d;
  font-size: 12px;
}

.metric-card h3 {
  margin: 6px 0 0;
  font-size: 20px;
  letter-spacing: -0.02em;
  color: #1f1f1f;
}

.trust-row {
  margin-top: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  position: relative;
  z-index: 1;
}

.trust-row span {
  border: 1px solid #dedad4;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  color: #4e4a45;
  background: rgba(255, 255, 255, 0.68);
}

.form-column {
  padding: 58px 50px;
  border-left: 1px solid var(--line);
  background: var(--surface-elevated);
  display: flex;
  flex-direction: column;
}

.form-header h2 {
  margin: 0;
  font-size: clamp(1.65rem, 2.3vw, 2rem);
  letter-spacing: -0.01em;
  color: var(--text);
}

.form-header p {
  margin: var(--space-sm) 0 0;
  color: var(--text-muted);
  font-size: 14px;
}

.login-form {
  margin-top: 34px;
}

.field {
  display: grid;
  gap: 9px;
  margin-bottom: var(--space-md);
}

.field span {
  font-size: 13px;
  color: #3d3d3d;
  font-weight: 600;
}

.field input {
  width: 100%;
  height: 45px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: #fbfbfa;
  color: var(--text);
  font-size: 14px;
  padding: 0 14px;
  outline: none;
  transition: border-color 180ms cubic-bezier(0.22, 1, 0.36, 1), box-shadow 180ms cubic-bezier(0.22, 1, 0.36, 1), background-color 180ms cubic-bezier(0.22, 1, 0.36, 1);
}

.field input:focus {
  border-color: #97928a;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(34, 34, 34, 0.07);
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 10px;
}

.captcha-btn {
  border: 1px solid #ddd9d4;
  border-radius: 12px;
  background: #f4f2ef;
  color: #222;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: transform 160ms cubic-bezier(0.34, 1.56, 0.64, 1), background-color 180ms cubic-bezier(0.22, 1, 0.36, 1);
}

.captcha-btn:hover {
  transform: translateY(-1px);
  background: var(--accent-soft);
}

.captcha-btn:active {
  transform: translateY(0);
}

.assist-row {
  margin-top: -2px;
  margin-bottom: var(--space-md);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.remember {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #504d49;
  font-size: 13px;
}

.remember input {
  width: 14px;
  height: 14px;
  accent-color: #2e2e2e;
}

.link-btn {
  border: none;
  background: transparent;
  color: #45413d;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

.link-btn:hover {
  text-decoration: underline;
  text-underline-offset: 2px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 13px;
  margin-top: var(--space-xs);
  background: linear-gradient(136deg, #181818 0%, #292929 100%);
  color: #f8f8f8;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition: transform 160ms cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 180ms cubic-bezier(0.22, 1, 0.36, 1);
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.72;
  transform: none;
  box-shadow: none;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 22px rgba(24, 24, 24, 0.2);
}

.submit-btn:active {
  transform: translateY(0);
}

.error-tip {
  margin: 4px 0 0;
  color: var(--danger);
  font-size: 13px;
}

.form-note {
  margin: 10px 0 0;
  color: #79736b;
  font-size: 12px;
}

.form-footer {
  margin-top: auto;
  padding-top: 18px;
  color: #8e8880;
  font-size: 12px;
  border-top: 1px dashed #e9e4de;
}

@keyframes rise-in {
  from {
    transform: translateY(14px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@media (max-width: 980px) {
  .login-page {
    padding: 22px;
  }

  .hero-panel {
    grid-template-columns: 1fr;
  }

  .brand-column {
    padding: 34px 28px 30px;
  }

  .brand-column::after {
    width: 128px;
    height: 128px;
    right: 22px;
    bottom: 22px;
  }

  .form-column {
    padding: 34px 28px;
    border-left: none;
    border-top: 1px solid var(--line);
  }

  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .assist-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
