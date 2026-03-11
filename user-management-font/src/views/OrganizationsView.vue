<template>
  <header class="topbar">
    <div>
      <div class="title">组织管理</div>
      <div class="meta">组织树维护、父子关系调整、成员增减；所有操作直连后端接口</div>
    </div>
    <div class="actions">
      <button v-if="hasPerm('org:add')" class="btn primary" @click="openCreate(selected.id)">新增子组织</button>
      <button v-if="hasPerm('org:edit')" class="btn" @click="openEdit(selected)">编辑当前组织</button>
      <button v-if="hasPerm('org:delete')" class="btn danger" @click="removeOrg(selected)">删除当前组织</button>
      <button class="btn" @click="fetchOrgTree">刷新</button>
    </div>
  </header>

  <section class="split-shell">
    <article class="tree-panel">
      <h3>组织树</h3>
      <OrgTree :nodes="orgTree" :selected-id="selected.id" @select="selectNode" />
      <p class="small">当前节点：{{ selected.name }}（ID: {{ selected.id || '-' }} / CODE: {{ selected.code || '-' }}）</p>
      <p v-if="errorMessage" class="small" style="color: var(--danger)">{{ errorMessage }}</p>
    </article>

    <article class="list-panel">
      <h3>{{ selected.name }} - 直接子组织</h3>
      <table class="table">
        <thead><tr><th>名称</th><th>编码</th><th>负责人</th><th>排序</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="(row, idx) in selectedChildren" :key="row.id">
            <td>{{ row.name }}</td>
            <td>{{ row.code }}</td>
            <td>{{ row.leader || '-' }}</td>
            <td>{{ row.sortNo ?? 0 }}</td>
            <td><span :class="['badge', row.status === 1 ? 'ok' : 'warn']">{{ row.status === 1 ? '启用' : '禁用' }}</span></td>
            <td>
              <button v-if="hasPerm('org:edit')" class="btn" @click="openEdit(row)">编辑</button>
              <button v-if="hasPerm('org:add')" class="btn" @click="openCreate(row.id)">加子级</button>
              <button v-if="hasPerm('org:sort')" class="btn" :disabled="idx===0" @click="moveChild(row, -1)">上移</button>
              <button v-if="hasPerm('org:sort')" class="btn" :disabled="idx===selectedChildren.length-1" @click="moveChild(row, 1)">下移</button>
              <button v-if="hasPerm('org:delete')" class="btn danger" @click="removeOrg(row)">删除</button>
            </td>
          </tr>
          <tr v-if="!selectedChildren.length"><td colspan="6">当前节点暂无子组织</td></tr>
        </tbody>
      </table>

      <h3 style="margin-top: 16px">{{ selected.name }} - 直属成员</h3>
      <div class="actions" style="margin-bottom: 8px">
        <input v-model.number="memberUserId" class="btn" type="number" placeholder="输入用户ID后回车添加" @keyup.enter="addMemberById" />
        <button v-if="hasPerm('user:edit')" class="btn" @click="addMemberById">添加成员</button>
        <button class="btn" @click="fetchOrgUsers">刷新成员</button>
      </div>
      <table class="table">
        <thead><tr><th>ID</th><th>用户名</th><th>姓名</th><th>手机号</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="u in orgUsers" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td>{{ u.realName }}</td>
            <td>{{ u.phone || '-' }}</td>
            <td>
              <button v-if="hasPerm('user:edit')" class="btn danger" @click="removeMember(u.id)">移除</button>
            </td>
          </tr>
          <tr v-if="!orgUsers.length"><td colspan="5">当前组织暂无直属成员</td></tr>
        </tbody>
      </table>
    </article>
  </section>

  <section v-if="showModal" class="modal" @click.self="closeModal">
    <article class="modal-card">
      <h3>{{ editingId ? '编辑组织' : '新增组织' }}</h3>
      <div class="form-grid">
        <div class="field"><label>名称 *</label><input v-model.trim="form.name" /></div>
        <div class="field"><label>编码 *</label><input v-model.trim="form.code" /></div>
        <div class="field"><label>负责人</label><input v-model.trim="form.leader" /></div>
        <div class="field"><label>排序号</label><input v-model.number="form.sortNo" type="number" /></div>
        <div class="field full"><label>描述</label><input v-model.trim="form.description" /></div>
        <div class="field"><label>父级组织</label>
          <select v-model.number="form.parentId">
            <option v-for="opt in orgOptions" :key="opt.id" :value="opt.id">{{ opt.name }}</option>
          </select>
        </div>
        <div class="field"><label>状态</label>
          <select v-model.number="form.status">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>
      </div>
      <div class="actions">
        <button class="btn" @click="closeModal">取消</button>
        <button class="btn primary" @click="submitOrg">保存</button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import OrgTree from '../components/OrgTree.vue'
import { secondVerify } from '../api/auth'
import {
  addUserToOrg,
  createOrg,
  deleteOrg,
  getOrgTree,
  listOrgUsers,
  removeUserFromOrg,
  updateOrg
} from '../api/orgs'

const orgTree = ref([])
const selected = ref({ id: 1, name: '集团总部', code: 'HQ', children: [] })
const orgUsers = ref([])
const memberUserId = ref(null)
const errorMessage = ref('')
const permissions = ref([])

const showModal = ref(false)
const editingId = ref(null)
const form = reactive({
  parentId: 0,
  name: '',
  code: '',
  leader: '',
  description: '',
  sortNo: 0,
  status: 1
})

const selectedChildren = computed(() => selected.value?.children || [])

const orgOptions = computed(() => {
  const list = [{ id: 0, name: '根节点' }]
  for (const node of flattenOrgTree(orgTree.value)) {
    if (!editingId.value || node.id !== editingId.value) {
      list.push({ id: node.id, name: `${node.name} (ID:${node.id})` })
    }
  }
  return list
})

function hasPerm(code) {
  return permissions.value.includes(code) || permissions.value.includes('ROLE_ADMIN')
}

function flattenOrgTree(nodes, output = []) {
  for (const node of nodes || []) {
    output.push(node)
    flattenOrgTree(node.children || [], output)
  }
  return output
}

function selectNode(node) {
  selected.value = node
  fetchOrgUsers()
}

async function fetchOrgTree(preserveId = null) {
  errorMessage.value = ''
  try {
    const tree = await getOrgTree()
    orgTree.value = tree || []
    if (!orgTree.value.length) {
      selected.value = { id: null, name: '无组织', children: [] }
      orgUsers.value = []
      return
    }

    const targetId = preserveId || selected.value?.id || orgTree.value[0].id
    const hit = flattenOrgTree(orgTree.value).find((x) => x.id === targetId)
    selected.value = hit || orgTree.value[0]
    await fetchOrgUsers()
  } catch (err) {
    errorMessage.value = err.message
  }
}

async function fetchOrgUsers() {
  if (!selected.value?.id) {
    orgUsers.value = []
    return
  }
  try {
    orgUsers.value = (await listOrgUsers(selected.value.id)) || []
  } catch (err) {
    errorMessage.value = err.message
  }
}

function resetForm() {
  Object.assign(form, {
    parentId: 0,
    name: '',
    code: '',
    leader: '',
    description: '',
    sortNo: 0,
    status: 1
  })
}

function openCreate(parentId = 0) {
  editingId.value = null
  resetForm()
  form.parentId = parentId || 0
  showModal.value = true
}

function openEdit(node) {
  editingId.value = node.id
  Object.assign(form, {
    parentId: node.parentId ?? 0,
    name: node.name || '',
    code: node.code || '',
    leader: node.leader || '',
    description: node.description || '',
    sortNo: node.sortNo ?? 0,
    status: node.status ?? 1
  })
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function submitOrg() {
  if (!form.name || !form.code) {
    errorMessage.value = '组织名称和编码不能为空'
    return
  }

  errorMessage.value = ''
  const payload = {
    parentId: form.parentId,
    name: form.name,
    code: form.code,
    leader: form.leader || null,
    description: form.description || null,
    sortNo: form.sortNo ?? 0,
    status: form.status ?? 1
  }

  try {
    if (editingId.value) {
      await updateOrg(editingId.value, payload)
    } else {
      await createOrg(payload)
    }
    showModal.value = false
    await fetchOrgTree(selected.value?.id)
  } catch (err) {
    errorMessage.value = err.message
  }
}

async function removeOrg(node) {
  if (!node?.id) return
  if (!window.confirm(`确认删除组织 ${node.name} 吗？`)) {
    return
  }

  errorMessage.value = ''
  try {
    const password = window.prompt('敏感操作二次验证：请输入你的登录密码')
    if (!password) return
    const { token } = await secondVerify(password)
    await deleteOrg(node.id, token)
    const fallbackId = node.parentId && node.parentId > 0 ? node.parentId : null
    await fetchOrgTree(fallbackId)
  } catch (err) {
    errorMessage.value = err.message
  }
}

async function moveChild(row, delta) {
  const siblings = selectedChildren.value.slice()
  const index = siblings.findIndex((x) => x.id === row.id)
  if (index < 0) return
  const targetIndex = index + delta
  if (targetIndex < 0 || targetIndex >= siblings.length) return

  const target = siblings[targetIndex]
  const aSort = row.sortNo ?? 0
  const bSort = target.sortNo ?? 0

  try {
    await updateOrg(row.id, {
      parentId: row.parentId ?? 0,
      name: row.name,
      code: row.code,
      leader: row.leader || null,
      description: row.description || null,
      sortNo: bSort,
      status: row.status ?? 1
    })
    await updateOrg(target.id, {
      parentId: target.parentId ?? 0,
      name: target.name,
      code: target.code,
      leader: target.leader || null,
      description: target.description || null,
      sortNo: aSort,
      status: target.status ?? 1
    })
    await fetchOrgTree(selected.value.id)
  } catch (err) {
    errorMessage.value = err.message
  }
}

async function addMemberById() {
  if (!selected.value?.id || !memberUserId.value) {
    errorMessage.value = '请先选择组织并输入用户ID'
    return
  }
  errorMessage.value = ''
  try {
    await addUserToOrg(selected.value.id, memberUserId.value)
    memberUserId.value = null
    await fetchOrgUsers()
  } catch (err) {
    errorMessage.value = err.message
  }
}

async function removeMember(userId) {
  if (!selected.value?.id) return
  errorMessage.value = ''
  try {
    await removeUserFromOrg(selected.value.id, userId)
    await fetchOrgUsers()
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
  await fetchOrgTree()
})
</script>
