<template>
  <header class="topbar">
    <div>
      <div class="title">角色权限</div>
      <div class="meta">角色状态、菜单按钮权限、模块数据权限规则统一配置</div>
    </div>
    <div class="actions">
      <button class="btn">新增角色</button>
      <button class="btn">分配用户</button>
      <button class="btn primary">保存权限绑定</button>
    </div>
  </header>

  <section class="grid">
    <article class="card span-4">
      <h3>角色列表</h3>
      <table class="table">
        <thead><tr><th>角色名</th><th>状态</th><th>成员数</th></tr></thead>
        <tbody>
          <tr v-for="row in roles" :key="row.id">
            <td><strong>{{ row.name }}</strong></td>
            <td><span :class="['badge', row.status === 1 ? 'ok' : 'warn']">{{ row.status === 1 ? '启用' : '禁用' }}</span></td>
            <td>-</td>
          </tr>
          <tr v-if="!roles.length"><td colspan="3">暂无数据</td></tr>
        </tbody>
      </table>
    </article>

    <article class="card span-8">
      <h3>菜单与按钮权限</h3>
      <table class="table">
        <thead><tr><th>菜单</th><th>路由</th><th>权限标识</th></tr></thead>
        <tbody>
          <tr><td>用户管理</td><td>/users</td><td>user:add user:edit user:delete</td></tr>
          <tr><td>组织管理</td><td>/organizations</td><td>org:add org:edit org:sort</td></tr>
          <tr><td>角色权限</td><td>/role-permission</td><td>role:add role:bind perm:save</td></tr>
        </tbody>
      </table>
    </article>

    <article class="card span-12">
      <h3>数据权限规则（按模块）</h3>
      <table class="table">
        <thead><tr><th>模块</th><th>规则</th><th>后端条件示例</th></tr></thead>
        <tbody>
          <tr><td>用户管理</td><td>本部门及子部门</td><td>org_id in (...) </td></tr>
          <tr><td>订单模块</td><td>仅本部门</td><td>org_id = current_org</td></tr>
          <tr><td>报表模块</td><td>全部数据</td><td>无过滤</td></tr>
          <tr><td>我的申请</td><td>仅本人</td><td>creator_id = current_user</td></tr>
        </tbody>
      </table>
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listRoles } from '../api/roles'

const roles = ref([])

onMounted(async () => {
  try {
    roles.value = (await listRoles()) || []
  } catch (e) {
    // ignore
  }
})
</script>
