<template>
  <header class="topbar">
    <div>
      <div class="title">组织管理</div>
      <div class="meta">组织树维护、父子关系调整；成员管理通过独立弹窗操作</div>
    </div>
    <div class="actions">
      <button v-if="hasPerm('org:add')" class="btn primary" @click="openCreate(selected.id)">新增子组织</button>
      <button v-if="hasPerm('org:edit')" class="btn" @click="openEdit(selected)">编辑当前组织</button>
      <button v-if="hasPerm('org:delete')" class="btn danger" @click="removeOrg(selected)">删除当前组织</button>
      <button v-if="hasPerm('user:edit')" class="btn" :disabled="!selected.id" @click="openMemberModal">成员管理</button>
      <button class="btn" @click="fetchOrgTree">刷新</button>
    </div>
  </header>

  <section class="mini-grid">
    <article class="mini-card">
      <p>组织总数</p>
      <h4>{{ orgNodeCount }}</h4>
    </article>
    <article class="mini-card">
      <p>当前子组织</p>
      <h4>{{ selectedChildren.length }}</h4>
    </article>
    <article class="mini-card">
      <p>当前组织成员</p>
      <h4>{{ orgMemberIdSet.size }}</h4>
    </article>
  </section>

  <section class="split-shell">
    <article class="tree-panel">
      <h3>组织树</h3>
      <OrgTree :nodes="orgTree" :selected-id="selected.id" @select="selectNode" />
      <p class="small">当前节点：{{ selected.name }}（CODE: {{ selected.code || '-' }}）</p>
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

  <section v-if="showMemberModal" class="modal" @click.self="closeMemberModal">
    <article class="modal-card member-modal-card">
      <h3>成员管理：{{ selected.name }}</h3>
      <div class="actions" style="margin-bottom: 10px">
        <input v-model.trim="memberKeyword" class="btn" placeholder="搜索用户名/姓名/手机号" @keyup.enter="searchMembers" />
        <button class="btn" @click="searchMembers">搜索</button>
        <button class="btn" @click="reloadMembers">刷新</button>
      </div>

      <div class="member-table-wrap">
        <table class="table member-modal-table">
          <thead><tr><th>用户名</th><th>姓名</th><th>手机号</th><th>归属状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-if="memberLoading"><td colspan="5">加载中...</td></tr>
            <tr v-for="u in memberUsers" :key="u.id">
              <td>{{ u.username }}</td>
              <td>{{ u.realName }}</td>
              <td>{{ u.phone || '-' }}</td>
              <td>
                <span v-if="isMember(u.id)" class="badge ok">已在本组织</span>
                <span v-else class="badge warn">未在本组织</span>
              </td>
              <td>
                <button
                  v-if="hasPerm('user:edit') && !isMember(u.id)"
                  class="btn"
                  :disabled="memberActionLoadingId === u.id"
                  @click="addMember(u.id)"
                >添加</button>
                <button
                  v-if="hasPerm('user:edit') && isMember(u.id)"
                  class="btn danger"
                  :disabled="memberActionLoadingId === u.id"
                  @click="removeMember(u.id)"
                >移除</button>
              </td>
            </tr>
            <tr v-if="!memberLoading && !memberUsers.length"><td colspan="5">暂无数据</td></tr>
          </tbody>
        </table>
      </div>

      <div class="actions" style="margin-top: 10px; justify-content: space-between;">
        <span class="small">共 {{ memberPagination.total }} 条，第 {{ memberPagination.page }} / {{ memberTotalPages }} 页</span>
        <div class="actions">
          <select v-model.number="memberPagination.size" @change="onMemberPageSizeChange">
            <option :value="10">10 / 页</option>
          </select>
          <button class="btn" :disabled="memberPagination.page <= 1" @click="changeMemberPage(memberPagination.page - 1)">上一页</button>
          <button
            v-for="page in memberPageNumbers"
            :key="`member-page-${page}`"
            :class="['btn', memberPagination.page === page ? 'primary' : '']"
            @click="changeMemberPage(page)"
          >{{ page }}</button>
          <button class="btn" :disabled="memberPagination.page >= memberTotalPages" @click="changeMemberPage(memberPagination.page + 1)">下一页</button>
        </div>
      </div>

      <div class="actions" style="margin-top: 10px">
        <button class="btn" @click="closeMemberModal">关闭</button>
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
import { pageUsers } from '../api/users'

const orgTree = ref([])
const selected = ref({ id: 1, name: '集团总部', code: 'HQ', children: [] })
const orgMemberIdSet = ref(new Set())
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

const showMemberModal = ref(false)
const memberKeyword = ref('')
const memberLoading = ref(false)
const memberActionLoadingId = ref(null)
const memberUsers = ref([])
const memberPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const selectedChildren = computed(() => selected.value?.children || [])
const orgNodeCount = computed(() => flattenOrgTree(orgTree.value, []).length)
const memberTotalPages = computed(() => Math.max(1, Math.ceil((memberPagination.total || 0) / memberPagination.size)))
const memberPageNumbers = computed(() => Array.from({ length: memberTotalPages.value }, (_, i) => i + 1))

const orgOptions = computed(() => {
  const list = [{ id: 0, name: '根节点' }]
  for (const node of flattenOrgTree(orgTree.value)) {
    if (!editingId.value || node.id !== editingId.value) {
      list.push({ id: node.id, name: node.name })
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
  fetchCurrentOrgMembers()
}

async function fetchOrgTree(preserveId = null) {
  errorMessage.value = ''
  try {
    const tree = await getOrgTree()
    orgTree.value = tree || []
    if (!orgTree.value.length) {
      selected.value = { id: null, name: '无组织', children: [] }
      orgMemberIdSet.value = new Set()
      return
    }

    const targetId = preserveId || selected.value?.id || orgTree.value[0].id
    const hit = flattenOrgTree(orgTree.value).find((x) => x.id === targetId)
    selected.value = hit || orgTree.value[0]
    await fetchCurrentOrgMembers()
  } catch (err) {
    errorMessage.value = err.message
  }
}

async function fetchCurrentOrgMembers() {
  if (!selected.value?.id) {
    orgMemberIdSet.value = new Set()
    return
  }
  try {
    const users = (await listOrgUsers(selected.value.id)) || []
    orgMemberIdSet.value = new Set(users.map((u) => u.id))
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

function isMember(userId) {
  return orgMemberIdSet.value.has(userId)
}

async function fetchMemberUsers() {
  memberLoading.value = true
  errorMessage.value = ''
  try {
    const data = await pageUsers({
      page: memberPagination.page,
      size: memberPagination.size,
      keyword: memberKeyword.value || undefined
    })
    memberUsers.value = data?.items || []
    memberPagination.total = data?.total || 0
    memberPagination.page = data?.page || memberPagination.page
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    memberLoading.value = false
  }
}

async function openMemberModal() {
  if (!selected.value?.id) {
    errorMessage.value = '请先选择组织'
    return
  }
  memberKeyword.value = ''
  memberPagination.page = 1
  await Promise.all([fetchCurrentOrgMembers(), fetchMemberUsers()])
  showMemberModal.value = true
}

function closeMemberModal() {
  showMemberModal.value = false
}

function searchMembers() {
  memberPagination.page = 1
  fetchMemberUsers()
}

function reloadMembers() {
  Promise.all([fetchCurrentOrgMembers(), fetchMemberUsers()])
}

function changeMemberPage(page) {
  if (page < 1 || page > memberTotalPages.value) return
  memberPagination.page = page
  fetchMemberUsers()
}

function onMemberPageSizeChange() {
  memberPagination.page = 1
  fetchMemberUsers()
}

async function addMember(userId) {
  if (!selected.value?.id) return
  memberActionLoadingId.value = userId
  errorMessage.value = ''
  try {
    await addUserToOrg(selected.value.id, userId)
    await fetchCurrentOrgMembers()
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    memberActionLoadingId.value = null
  }
}

async function removeMember(userId) {
  if (!selected.value?.id) return
  memberActionLoadingId.value = userId
  errorMessage.value = ''
  try {
    await removeUserFromOrg(selected.value.id, userId)
    await fetchCurrentOrgMembers()
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    memberActionLoadingId.value = null
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

<style scoped>
.split-shell {
  grid-template-columns: minmax(240px, 280px) 1fr;
}

.member-modal-card {
  width: min(1120px, 100%);
}

.member-table-wrap {
  border: 1px solid var(--line);
  border-radius: 12px;
  overflow: hidden;
}

.member-modal-table {
  width: 100%;
  table-layout: fixed;
  margin-top: 0;
}

.member-modal-table thead,
.member-modal-table tbody tr {
  display: table;
  width: 100%;
  table-layout: fixed;
}

.member-modal-table tbody {
  display: block;
  height: 440px;
  min-height: 440px;
  max-height: 440px;
  overflow-y: auto;
}

.member-modal-table thead tr,
.member-modal-table tbody tr {
  height: 44px;
}

.member-modal-table th,
.member-modal-table td {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1040px) {
  .split-shell {
    grid-template-columns: 1fr;
  }
}
</style>
