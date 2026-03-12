<template>
  <header class="topbar">
    <div>
      <div class="title">日志与安全中心</div>
      <div class="meta">登录日志、关键操作日志、敏感操作二次验证、密码策略</div>
    </div>
    <div class="actions">
      <button class="btn" @click="reload">刷新日志</button>
      <button class="btn primary">启用二次验证</button>
    </div>
  </header>

  <section class="mini-grid">
    <article class="mini-card">
      <p>登录日志总数</p>
      <h4>{{ loginPagination.total }}</h4>
    </article>
    <article class="mini-card">
      <p>操作日志总数</p>
      <h4>{{ opPagination.total }}</h4>
    </article>
    <article class="mini-card">
      <p>当前页失败登录</p>
      <h4>{{ failedLoginCount }}</h4>
    </article>
  </section>

  <section class="grid">
    <article class="card span-4">
      <h3>安全状态</h3>
      <p class="small">密码算法：BCrypt</p>
      <p class="small">登录防刷：验证码 + 失败锁定</p>
      <p class="small">会话机制：JWT</p>
      <div class="actions">
        <span class="badge ok">BCrypt 已启用</span>
        <span class="badge ok">JWT 正常</span>
        <span class="badge warn">二次验证部分接口未覆盖</span>
      </div>
    </article>

    <article class="card span-8">
      <h3>登录日志</h3>
      <table class="table">
        <thead><tr><th>时间</th><th>用户名</th><th>IP</th><th>结果</th><th>设备</th></tr></thead>
        <tbody>
          <tr v-for="row in loginLogs" :key="row.id">
            <td>{{ row.createdAt }}</td>
            <td>{{ row.username }}</td>
            <td>{{ row.ip || '-' }}</td>
            <td><span :class="['badge', row.success ? 'ok' : 'danger']">{{ row.success ? '成功' : '失败' }}</span></td>
            <td>{{ row.userAgent || '-' }}</td>
          </tr>
          <tr v-if="!loginLogs.length"><td colspan="5">暂无数据</td></tr>
        </tbody>
      </table>
      <div class="actions" style="margin-top: 10px; justify-content: space-between;">
        <span class="small">共 {{ loginPagination.total }} 条，第 {{ loginPagination.page }} / {{ loginTotalPages }} 页</span>
        <div class="actions">
          <select v-model.number="loginPagination.size" @change="onLoginSizeChange">
            <option :value="10">10 / 页</option>
            <option :value="20">20 / 页</option>
            <option :value="50">50 / 页</option>
          </select>
          <button class="btn" :disabled="loginPagination.page <= 1" @click="changeLoginPage(loginPagination.page - 1)">上一页</button>
          <button
            v-for="page in loginPageNumbers"
            :key="`login-page-${page}`"
            :class="['btn', loginPagination.page === page ? 'primary' : '']"
            @click="changeLoginPage(page)"
          >
            {{ page }}
          </button>
          <button class="btn" :disabled="loginPagination.page >= loginTotalPages" @click="changeLoginPage(loginPagination.page + 1)">下一页</button>
        </div>
      </div>
    </article>

    <article class="card span-12">
      <h3>关键操作日志</h3>
      <table class="table">
        <thead><tr><th>操作时间</th><th>操作人</th><th>模块</th><th>动作</th><th>详情</th></tr></thead>
        <tbody>
          <tr v-for="row in opLogs" :key="row.id">
            <td>{{ row.createdAt }}</td>
            <td>{{ row.username }}</td>
            <td>{{ row.module }}</td>
            <td>{{ row.action }}</td>
            <td>{{ row.detail || '-' }}</td>
          </tr>
          <tr v-if="!opLogs.length"><td colspan="5">暂无数据</td></tr>
        </tbody>
      </table>
      <div class="actions" style="margin-top: 10px; justify-content: space-between;">
        <span class="small">共 {{ opPagination.total }} 条，第 {{ opPagination.page }} / {{ opTotalPages }} 页</span>
        <div class="actions">
          <select v-model.number="opPagination.size" @change="onOpSizeChange">
            <option :value="10">10 / 页</option>
            <option :value="20">20 / 页</option>
            <option :value="50">50 / 页</option>
          </select>
          <button class="btn" :disabled="opPagination.page <= 1" @click="changeOpPage(opPagination.page - 1)">上一页</button>
          <button
            v-for="page in opPageNumbers"
            :key="`op-page-${page}`"
            :class="['btn', opPagination.page === page ? 'primary' : '']"
            @click="changeOpPage(page)"
          >
            {{ page }}
          </button>
          <button class="btn" :disabled="opPagination.page >= opTotalPages" @click="changeOpPage(opPagination.page + 1)">下一页</button>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { pageLoginLogs, pageOperationLogs } from '../api/logs'

const loginLogs = ref([])
const opLogs = ref([])

const loginPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const opPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const loginTotalPages = computed(() => Math.max(1, Math.ceil((loginPagination.total || 0) / loginPagination.size)))
const opTotalPages = computed(() => Math.max(1, Math.ceil((opPagination.total || 0) / opPagination.size)))
const failedLoginCount = computed(() => loginLogs.value.filter((x) => !x.success).length)
const loginPageNumbers = computed(() => Array.from({ length: loginTotalPages.value }, (_, i) => i + 1))
const opPageNumbers = computed(() => Array.from({ length: opTotalPages.value }, (_, i) => i + 1))

async function loadLoginLogs() {
  const data = await pageLoginLogs({
    page: loginPagination.page,
    size: loginPagination.size
  })
  loginLogs.value = data?.items || []
  loginPagination.total = data?.total || 0
  loginPagination.page = data?.page || loginPagination.page
}

async function loadOperationLogs() {
  const data = await pageOperationLogs({
    page: opPagination.page,
    size: opPagination.size
  })
  opLogs.value = data?.items || []
  opPagination.total = data?.total || 0
  opPagination.page = data?.page || opPagination.page
}

function changeLoginPage(page) {
  if (page < 1 || page > loginTotalPages.value) return
  loginPagination.page = page
  loadLoginLogs()
}

function changeOpPage(page) {
  if (page < 1 || page > opTotalPages.value) return
  opPagination.page = page
  loadOperationLogs()
}

function onLoginSizeChange() {
  loginPagination.page = 1
  loadLoginLogs()
}

function onOpSizeChange() {
  opPagination.page = 1
  loadOperationLogs()
}

function reload() {
  loadLoginLogs()
  loadOperationLogs()
}

onMounted(async () => {
  try {
    await Promise.all([loadLoginLogs(), loadOperationLogs()])
  } catch (e) {
    // ignore
  }
})
</script>
