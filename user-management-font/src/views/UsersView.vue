<template>
  <header class="topbar">
    <div>
      <div class="title">用户管理</div>
      <div class="meta">左侧选组织，右侧展示该组织直属用户；支持角色分配、导入导出、重置密码</div>
    </div>
    <div class="actions">
      <input v-model="keyword" class="btn" placeholder="关键字搜索用户名/姓名/手机号" />
      <button class="btn" @click="fetchUsers">搜索</button>
      <button v-if="hasPerm('user:import')" class="btn" @click="triggerImport">导入 CSV</button>
      <button v-if="hasPerm('user:export')" class="btn" @click="handleExport">导出 CSV</button>
      <button v-if="hasPerm('user:add')" class="btn primary" @click="openCreate">新增用户</button>
      <input ref="fileInputRef" type="file" accept=".csv,text/csv" style="display: none" @change="handleImport" />
    </div>
  </header>

  <section class="split-shell">
    <article class="tree-panel">
      <h3>组织树</h3>
      <OrgTree :nodes="orgTree" :selected-id="selectedOrg.id" @select="onSelectOrg" />
      <p class="small">当前节点：{{ selectedOrg.name }}，查询参数：orgId={{ selectedOrg.id }}</p>
    </article>

    <article class="list-panel">
      <h3>{{ selectedOrg.name }} - 用户列表</h3>
      <p v-if="errorMessage" class="small" style="color: var(--danger)">{{ errorMessage }}</p>
      <table class="table">
        <thead>
          <tr>
            <th>ID</th><th>用户名</th><th>姓名</th><th>手机号</th><th>邮箱</th><th>组织ID</th><th>状态</th><th>角色</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="9">加载中...</td></tr>
          <tr v-for="item in users" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.realName }}</td>
            <td>{{ item.phone || '-' }}</td>
            <td>{{ item.email || '-' }}</td>
            <td>{{ item.orgId }}</td>
            <td><span :class="['badge', item.status === 1 ? 'ok' : 'warn']">{{ item.status === 1 ? '启用' : '禁用' }}</span></td>
            <td>{{ (userRoleNames[item.id] || []).join(' / ') || '-' }}</td>
            <td>
              <button v-if="hasPerm('user:edit')" class="btn" @click="openEdit(item)">编辑</button>
              <button v-if="hasPerm('user:edit')" class="btn" @click="openResetPassword(item)">重置密码</button>
              <button v-if="hasPerm('user:delete')" class="btn danger" @click="remove(item.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && !users.length"><td colspan="9">暂无数据</td></tr>
        </tbody>
      </table>
    </article>
  </section>

  <section v-if="showEditModal" class="modal" @click.self="closeEditModal">
    <article class="modal-card">
      <h3>{{ editingId ? '编辑用户' : '新增用户' }}</h3>
      <div class="form-grid">
        <div class="field"><label>用户名 *</label><input v-model.trim="form.username" /></div>
        <div class="field"><label>密码 {{ editingId ? '(留空则不修改)' : '*' }}</label><input v-model="form.password" type="password" /></div>
        <div class="field"><label>姓名 *</label><input v-model.trim="form.realName" /></div>
        <div class="field"><label>手机号</label><input v-model.trim="form.phone" /></div>
        <div class="field"><label>邮箱</label><input v-model.trim="form.email" /></div>
        <div class="field"><label>头像URL</label><input v-model.trim="form.avatar" /></div>
        <div class="field"><label>组织ID *</label><input v-model.number="form.orgId" type="number" min="1" /></div>
        <div class="field"><label>状态 *</label>
          <select v-model.number="form.status">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>
        <div class="field full">
          <label>角色分配</label>
          <div class="check-grid">
            <label v-for="role in roles" :key="role.id" class="check-item">
              <input type="checkbox" :checked="form.roleIds.includes(role.id)" @change="toggleRole(role.id, $event.target.checked)" />
              {{ role.name }}（{{ role.code }}）
            </label>
            <p v-if="!roles.length" class="small">暂无可选角色</p>
          </div>
        </div>
      </div>
      <div class="actions">
        <button class="btn" @click="closeEditModal">取消</button>
        <button class="btn primary" @click="submit">保存</button>
      </div>
    </article>
  </section>

  <section v-if="showResetModal" class="modal" @click.self="closeResetModal">
    <article class="modal-card">
      <h3>重置密码：{{ resetTarget?.username }}</h3>
      <div class="field">
        <label>新密码</label>
        <input v-model="resetPasswordValue" type="password" placeholder="请输入新密码" />
      </div>
      <div class="actions">
        <button class="btn" @click="closeResetModal">取消</button>
        <button class="btn primary" @click="confirmResetPassword">确认重置</button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import OrgTree from '../components/OrgTree.vue'
import { secondVerify } from '../api/auth'
import {
  createUser,
  deleteUser,
  exportUsers,
  importUsers,
  listUserRoleIds,
  listUsers,
  resetPassword,
  updateUser
} from '../api/users'
import { getOrgTree } from '../api/orgs'
import { listRoles } from '../api/roles'

const orgTree = ref([])
const selectedOrg = ref({ id: 1, name: '组织' })
const keyword = ref('')
const users = ref([])
const loading = ref(false)
const errorMessage = ref('')
const permissions = ref([])
const fileInputRef = ref(null)

const roles = ref([])
const userRoleNames = ref({})

const showEditModal = ref(false)
const editingId = ref(null)
const showResetModal = ref(false)
const resetTarget = ref(null)
const resetPasswordValue = ref('')

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  avatar: '',
  orgId: 1,
  status: 1,
  roleIds: []
})

function hasPerm(code) {
  return permissions.value.includes(code) || permissions.value.includes('ROLE_ADMIN')
}

function onSelectOrg(node) {
  selectedOrg.value = node
  form.orgId = node.id
  fetchUsers()
}

function buildPayload() {
  return {
    username: form.username,
    password: form.password,
    realName: form.realName,
    phone: form.phone || null,
    email: form.email || null,
    avatar: form.avatar || null,
    orgId: form.orgId,
    status: form.status,
    roleIds: form.roleIds
  }
}

function resetForm() {
  Object.assign(form, {
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    avatar: '',
    orgId: selectedOrg.value.id,
    status: 1,
    roleIds: []
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  showEditModal.value = true
}

async function openEdit(item) {
  editingId.value = item.id
  resetForm()
  Object.assign(form, {
    username: item.username,
    password: '',
    realName: item.realName,
    phone: item.phone || '',
    email: item.email || '',
    avatar: item.avatar || '',
    orgId: item.orgId,
    status: item.status,
    roleIds: []
  })
  try {
    form.roleIds = await listUserRoleIds(item.id)
  } catch (err) {
    errorMessage.value = err.message
  }
  showEditModal.value = true
}

function closeEditModal() {
  showEditModal.value = false
}

function openResetPassword(item) {
  resetTarget.value = item
  resetPasswordValue.value = ''
  showResetModal.value = true
}

function closeResetModal() {
  showResetModal.value = false
}

async function confirmResetPassword() {
  if (!resetTarget.value) return
  if (!resetPasswordValue.value.trim()) {
    errorMessage.value = '新密码不能为空'
    return
  }
  errorMessage.value = ''
  try {
    await resetPassword(resetTarget.value.id, resetPasswordValue.value.trim())
    showResetModal.value = false
  } catch (err) {
    errorMessage.value = err.message
  }
}

function toggleRole(roleId, checked) {
  const set = new Set(form.roleIds)
  if (checked) {
    set.add(roleId)
  } else {
    set.delete(roleId)
  }
  form.roleIds = Array.from(set)
}

async function fetchUsers() {
  loading.value = true
  errorMessage.value = ''
  try {
    users.value = await listUsers({
      orgId: selectedOrg.value.id,
      keyword: keyword.value.trim()
    })
    await refreshUserRoleNames()
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    loading.value = false
  }
}

async function refreshUserRoleNames() {
  const roleMap = new Map(roles.value.map((r) => [r.id, r.name]))
  const result = {}
  for (const item of users.value) {
    try {
      const ids = await listUserRoleIds(item.id)
      result[item.id] = (ids || []).map((id) => roleMap.get(id) || `#${id}`)
    } catch (_) {
      result[item.id] = []
    }
  }
  userRoleNames.value = result
}

async function submit() {
  if (!form.username || !form.realName || !form.orgId || form.status === null || form.status === undefined) {
    errorMessage.value = '请填写必填项'
    return
  }
  if (!editingId.value && !form.password) {
    errorMessage.value = '新增用户必须填写密码'
    return
  }

  errorMessage.value = ''
  try {
    const payload = buildPayload()
    if (editingId.value) {
      await updateUser(editingId.value, payload)
    } else {
      await createUser(payload)
    }
    showEditModal.value = false
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

function triggerImport() {
  fileInputRef.value?.click()
}

async function handleImport(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }
  errorMessage.value = ''
  try {
    const res = await importUsers(file)
    window.alert(`导入完成，新增 ${res.created || 0} 条`) 
    await fetchUsers()
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    event.target.value = ''
  }
}

async function handleExport() {
  errorMessage.value = ''
  try {
    const blob = await exportUsers({ orgId: selectedOrg.value.id })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `users-org-${selectedOrg.value.id}.csv`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
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
    roles.value = (await listRoles()) || []
  } catch (_) {
    roles.value = []
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
