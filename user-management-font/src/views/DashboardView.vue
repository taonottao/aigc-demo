<template>
  <header class="topbar">
    <div>
      <div class="title">系统总览</div>
      <div class="meta">欢迎回来，smile-admin（超级管理员）</div>
    </div>
    <div class="actions">
      <button class="btn">导出日报</button>
      <RouterLink class="btn cyan" to="/users">新增用户</RouterLink>
    </div>
  </header>

  <section class="grid">
    <article class="card span-3"><h3>组织总数</h3><p class="kpi">36</p><span class="badge ok">树形结构正常</span></article>
    <article class="card span-3"><h3>启用用户</h3><p class="kpi">1284</p><span class="badge ok">在线 214</span></article>
    <article class="card span-3"><h3>角色数量</h3><p class="kpi">48</p><span class="badge warn">12 个高权限</span></article>
    <article class="card span-3"><h3>今日告警</h3><p class="kpi">5</p><span class="badge danger">2 个需复核</span></article>

    <article class="card span-8">
      <h3>快捷工作台</h3>
      <div class="actions">
        <RouterLink class="btn" to="/organizations">组织树维护</RouterLink>
        <RouterLink class="btn" to="/users">用户增删改查</RouterLink>
        <RouterLink class="btn" to="/role-permission">角色权限绑定</RouterLink>
        <RouterLink class="btn" to="/security">查看登录日志</RouterLink>
      </div>
    </article>

    <article class="card span-4">
      <h3>数据权限规则快照</h3>
      <table class="table">
        <thead><tr><th>模块</th><th>规则</th></tr></thead>
        <tbody>
          <tr><td>用户管理</td><td>本部门及子部门</td></tr>
          <tr><td>订单模块</td><td>仅本部门</td></tr>
          <tr><td>报表模块</td><td>全部数据</td></tr>
        </tbody>
      </table>
    </article>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>
