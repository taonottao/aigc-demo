<template>
  <header class="topbar">
    <div>
      <div class="title">用户管理</div>
      <div class="meta">左侧选组织，右侧仅展示该组织直属用户（不含子组织）</div>
    </div>
    <div class="actions">
      <input v-model="keyword" class="btn" placeholder="关键字搜索用户名/姓名/手机号" />
      <button class="btn" @click="fetchUsers">搜索</button>
      <button v-if="hasPerm('user:add')" class="btn primary" @click="openCreate">新增用户</button>
    </div>
  </header>

  <section class="split-shell">
    <article class="tree-panel">
      <h3>组织树</h3>
      <OrgTree :nodes="orgTree" :selected-id="selectedOrg.id" @select="onSelectOrg" />
      <p class="small">当前节点：{{ selectedOrg.name }}，仅查询 org_id = {{ selectedOrg.id }}</p>
    </article>

    <article class="list-panel">
      <h3>{{ selectedOrg.name }} - 用户列表</h3>
      <p v-if="errorMessage" class="small" style="color: var(--danger)">{{ errorMessage }}</p>
      <table class="table">
        <thead>
          <tr>
            <th>ID</th><th>用户名</th><th>姓名</th><th>手机号</th><th>邮箱</th><th>组织ID</th><th>状态</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="8">加载中...</td></tr>
          <tr v-for="item in users" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.realName }}</td>
            <td>{{ item.phone || '-' }}</td>
            <td>{{ item.email || '-' }}</td>
            <td>{{ item.orgId }}</td>
            <td><span :class="['badge', item.status === 1 ? 'ok' : 'warn']">{{ item.status === 1 ? '启用' : '禁用' }}</span></td>
            <td>
              <button v-if="hasPerm('user:edit')" class="btn" @click="openEdit(item)">编辑</button>
              <button v-if="hasPerm('user:delete')" class="btn danger" @click="remove(item.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && !users.length"><td colspan="8">暂无数据</td></tr>
        </tbody>
      </table>
    </article>
  </section>

  <section v-if="showModal" class="modal" @click.self="closeModal">
    <article class="modal-card">
      <h3>{{ editingId ? '编辑用户' : '新增用户' }}</h3>
      <div class="form-grid">
        <div class="field"><label>用户名</label><input v-model="form.username" /></div>
        <div class="field"><label>密码</label><input v-model="form.password" type="password" /></div>
        <div class="field"><label>姓名</label><input v-model="form.realName" /></div>
        <div class="field"><label>手机号</label><input v-model="form.phone" /></div>
        <div class="field"><label>邮箱</label><input v-model="form.email" /></div>
        <div class="field"><label>头像URL</label><input v-model="form.avatar" /></div>
        <div class="field"><label>组织ID</label><input v-model.number="form.orgId" type="number" /></div>
        <div class="field"><label>状态(1启用/0禁用)</label><input v-model.number="form.status" type="number" /></div>
      </div>
      <div class="actions">
        <button class="btn" @click="closeModal">取消</button>
        <button class="btn primary" @click="submit">保存</button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import OrgTree from '../components/OrgTree.vue'
import { createUser, deleteUser, listUsers, updateUser } from '../api/users'
import { getOrgTree } from '../api/orgs'
import { secondVerify } from '../api/auth'

const orgTree = ref([])
const selectedOrg = ref({ id: 1, name: '组织' })
const keyword = ref('')
const users = ref([])
const loading = ref(false)
const errorMessage = ref('')
const showModal = ref(false)
const editingId = ref(null)
const permissions = ref([])

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  avatar: '',
  orgId: 1,
  status: 1
})

function onSelectOrg(node) {
  selectedOrg.value = node
  form.orgId = node.id
  fetchUsers()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    avatar: '',
    orgId: selectedOrg.value.id,
    status: 1
  })
  showModal.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    username: item.username,
    password: '',
    realName: item.realName,
    phone: item.phone || '',
    email: item.email || '',
    avatar: item.avatar || '',
    orgId: item.orgId,
    status: item.status
  })
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

function hasPerm(code) {
  return permissions.value.includes(code) || permissions.value.includes('ROLE_ADMIN')
}

async function fetchUsers() {
  loading.value = true
  errorMessage.value = ''
  try {
    users.value = await listUsers({
      orgId: selectedOrg.value.id,
      keyword: keyword.value.trim()
    })
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    loading.value = false
  }
}

async function submit() {
  errorMessage.value = ''
  try {
    if (editingId.value) {
      await updateUser(editingId.value, form)
    } else {
      await createUser(form)
    }
    showModal.value = false
    await fetchUsers()
  } catch (err) {
    errorMessage.value = err.message
  }
}

async function remove(id) {
  if (!window.confirm('确认删除该用户吗？')) {
    return
  }
  errorMessage.value = ''
  try {
    const password = window.prompt('敏感操作二次验证：请输入你的登录密码')
    if (!password) return
    const { token } = await secondVerify(password)
    await deleteUser(id, token)
    await fetchUsers()
  } catch (err) {
    errorMessage.value = err.message
  }
}

onMounted(async () => {
  try {
    permissions.value = JSON.parse(localStorage.getItem('permissions') || '[]')
  } catch (_) {
    permissions.value = []
  }
  try {
    const tree = await getOrgTree()
    orgTree.value = tree || []
    if (orgTree.value.length) {
      selectedOrg.value = orgTree.value[0]
      form.orgId = selectedOrg.value.id
    }
  } catch (err) {
    errorMessage.value = err.message
  }
  await fetchUsers()
})
</script>
