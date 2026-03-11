<template>
  <header class="topbar">
    <div>
      <div class="title">日志与安全中心</div>
      <div class="meta">登录日志、关键操作日志、敏感操作二次验证、密码策略</div>
    </div>
    <div class="actions">
      <button class="btn">导出审计日志</button>
      <button class="btn primary">启用二次验证</button>
    </div>
  </header>

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
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getLoginLogs, getOperationLogs } from '../api/logs'

const loginLogs = ref([])
const opLogs = ref([])

onMounted(async () => {
  try {
    loginLogs.value = (await getLoginLogs()) || []
    opLogs.value = (await getOperationLogs()) || []
  } catch (e) {
    // ignore
  }
})
</script>
