<template>
  <header class="topbar">
    <div>
      <div class="title">角色权限</div>
      <div class="meta">角色维护、菜单按钮权限绑定、数据权限规则（按模块）配置</div>
    </div>
    <div class="actions">
      <button v-if="hasPerm('role:add')" class="btn primary" @click="openRoleCreate">新增角色</button>
    </div>
  </header>

  <section class="mini-grid">
    <article class="mini-card">
      <p>角色总数</p>
      <h4>{{ rolePagination.total }}</h4>
    </article>
    <article class="mini-card">
      <p>菜单节点</p>
      <h4>{{ flatMenus.length }}</h4>
    </article>
    <article class="mini-card">
      <p>当前角色绑定</p>
      <h4>{{ selectedMenuIds.size }}</h4>
    </article>
  </section>

  <section class="grid">
    <article class="card span-12">
      <h3>角色列表</h3>
      <div class="actions" style="margin-bottom: 8px">
        <input v-model.trim="roleKeyword" class="btn" placeholder="搜索角色名/编码" />
        <button class="btn" @click="searchRoles">搜索</button>
      </div>
      <p v-if="errorMessage" class="small" style="color: var(--danger)">{{ errorMessage }}</p>
      <table class="table">
        <thead><tr><th>角色</th><th>编码</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="row in roles" :key="row.id" :style="{ background: selectedRole?.id === row.id ? 'rgba(0,168,204,0.08)' : 'transparent' }">
            <td>{{ row.name }}</td>
            <td>{{ row.code }}</td>
            <td><span :class="['badge', row.status === 1 ? 'ok' : 'warn']">{{ row.status === 1 ? '启用' : '禁用' }}</span></td>
            <td>
              <div class="actions">
                <button v-if="hasPerm('perm:save')" class="btn" @click="openRoleEdit(row)">编辑角色</button>
                <button v-if="hasPerm('perm:save')" class="btn" @click="openMenuPermissionSettings(row)">菜单权限</button>
                <button v-if="hasPerm('perm:save')" class="btn" @click="openDataScopeSettings(row)">数据权限</button>
              </div>
            </td>
          </tr>
          <tr v-if="!roles.length"><td colspan="4">暂无角色</td></tr>
        </tbody>
      </table>
      <div class="actions" style="margin-top: 10px; justify-content: space-between;">
        <span class="small">共 {{ rolePagination.total }} 条，第 {{ rolePagination.page }} / {{ roleTotalPages }} 页</span>
        <div class="actions">
          <select v-model.number="rolePagination.size" @change="onRolePageSizeChange">
            <option :value="10">10 / 页</option>
            <option :value="20">20 / 页</option>
            <option :value="50">50 / 页</option>
          </select>
          <button class="btn" :disabled="rolePagination.page <= 1" @click="changeRolePage(rolePagination.page - 1)">上一页</button>
          <button
            v-for="page in rolePageNumbers"
            :key="`role-page-${page}`"
            :class="['btn', rolePagination.page === page ? 'primary' : '']"
            @click="changeRolePage(page)"
          >
            {{ page }}
          </button>
          <button class="btn" :disabled="rolePagination.page >= roleTotalPages" @click="changeRolePage(rolePagination.page + 1)">下一页</button>
        </div>
      </div>
    </article>
  </section>

  <section v-if="showRoleModal" class="modal" @click.self="closeRoleModal">
    <article class="modal-card">
      <h3>{{ roleEditingId ? '编辑角色' : '新增角色' }}</h3>
      <div class="form-grid">
        <div class="field"><label>角色名称 *</label><input v-model.trim="roleForm.name" /></div>
        <div class="field"><label>角色编码 *</label><input v-model.trim="roleForm.code" /></div>
        <div class="field full"><label>描述</label><input v-model.trim="roleForm.description" /></div>
        <div class="field"><label>状态</label>
          <select v-model.number="roleForm.status">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>
      </div>
      <div class="actions">
        <button class="btn" @click="closeRoleModal">取消</button>
        <button class="btn primary" @click="submitRole">保存</button>
      </div>
    </article>
  </section>

  <section v-if="showMenuPermModal" class="modal" @click.self="closeMenuPermModal">
    <article class="modal-card permission-modal-card">
      <h3>菜单权限：{{ selectedRole ? `${selectedRole.name} (${selectedRole.code})` : '-' }}</h3>
      <div class="permission-body">
        <table class="table">
          <thead><tr><th>绑定</th><th>名称</th><th>类型</th><th>路由</th><th>权限标识</th></tr></thead>
          <tbody>
            <tr v-for="menu in flatMenus" :key="`menu-${menu.id}`">
              <td><input type="checkbox" :checked="selectedMenuIds.has(menu.id)" @change="toggleMenu(menu.id, $event.target.checked)" /></td>
              <td>{{ menu.indent }}{{ menu.name }}</td>
              <td>{{ menu.type }}</td>
              <td>{{ menu.path || '-' }}</td>
              <td>{{ menu.permCode || '-' }}</td>
            </tr>
            <tr v-if="!flatMenus.length"><td colspan="5">暂无菜单数据</td></tr>
          </tbody>
        </table>
      </div>
      <div class="actions">
        <button class="btn" @click="closeMenuPermModal">取消</button>
        <button v-if="hasPerm('perm:save')" class="btn primary" @click="saveRoleMenus">保存菜单权限</button>
      </div>
    </article>
  </section>

  <section v-if="showDataScopeModal" class="modal" @click.self="closeDataScopeModal">
    <article class="modal-card permission-modal-card">
      <h3>数据权限：{{ selectedRole ? `${selectedRole.name} (${selectedRole.code})` : '-' }}</h3>
      <div class="permission-body">
        <div class="actions" style="margin-bottom: 8px">
          <button v-if="hasPerm('perm:save')" class="btn" @click="addScopeRow">新增规则</button>
        </div>
        <table class="table">
          <thead><tr><th>模块编码</th><th>范围</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="(row, idx) in scopeRows" :key="idx">
              <td><input v-model.trim="row.moduleCode" placeholder="如 USER / ORDER" /></td>
              <td>
                <select v-model="row.scope">
                  <option value="ALL">ALL（全部）</option>
                  <option value="ORG_AND_CHILDREN">ORG_AND_CHILDREN（本部门及子部门）</option>
                  <option value="ORG_ONLY">ORG_ONLY（仅本部门）</option>
                  <option value="SELF_ONLY">SELF_ONLY（仅本人）</option>
                </select>
              </td>
              <td><button class="btn danger" @click="removeScopeRow(idx)">删除</button></td>
            </tr>
            <tr v-if="!scopeRows.length"><td colspan="3">暂无规则，点击“新增规则”</td></tr>
          </tbody>
        </table>
      </div>
      <div class="actions">
        <button class="btn" @click="closeDataScopeModal">取消</button>
        <button v-if="hasPerm('perm:save')" class="btn primary" @click="saveDataScopes">保存数据权限</button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getMenuTree } from '../api/menus'
import {
  createRole,
  getRoleDataScopes,
  getRoleMenuIds,
  pageRoles,
  replaceRoleDataScopes,
  replaceRoleMenuIds,
  updateRole
} from '../api/roles'

const roles = ref([])
const selectedRole = ref(null)
const roleKeyword = ref('')
const rolePagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const roleTotalPages = computed(() => Math.max(1, Math.ceil((rolePagination.total || 0) / rolePagination.size)))
const rolePageNumbers = computed(() => Array.from({ length: roleTotalPages.value }, (_, i) => i + 1))
const menuTree = ref([])
const selectedMenuIds = ref(new Set())
const scopeRows = ref([])
const errorMessage = ref('')
const permissions = ref([])

const showRoleModal = ref(false)
const showMenuPermModal = ref(false)
const showDataScopeModal = ref(false)
const roleEditingId = ref(null)
const roleForm = reactive({
  name: '',
  code: '',
  description: '',
  status: 1
})

const flatMenus = computed(() => flattenMenus(menuTree.value))

function hasPerm(code) {
  return permissions.value.includes(code) || permissions.value.includes('ROLE_ADMIN')
}

function flattenMenus(nodes, level = 0, output = []) {
  for (const node of nodes || []) {
    output.push({ ...node, level, indent: '  '.repeat(level) })
    flattenMenus(node.children || [], level + 1, output)
  }
  return output
}

async function loadRoles() {
  const data = await pageRoles({
    page: rolePagination.page,
    size: rolePagination.size,
    keyword: roleKeyword.value || undefined
  })
  roles.value = data?.items || []
  rolePagination.total = data?.total || 0
  rolePagination.page = data?.page || rolePagination.page
}

async function loadMenus() {
  menuTree.value = (await getMenuTree()) || []
}

async function selectRole(role) {
  selectedRole.value = role
  errorMessage.value = ''
  try {
    const [menuIds, scopes] = await Promise.all([
      getRoleMenuIds(role.id),
      getRoleDataScopes(role.id)
    ])
    selectedMenuIds.value = new Set(menuIds || [])
    scopeRows.value = (scopes || []).map((x) => ({
      moduleCode: x.moduleCode || '',
      scope: x.scope || 'SELF_ONLY'
    }))
  } catch (err) {
    errorMessage.value = err.message
  }
}

async function openMenuPermissionSettings(role) {
  if (!role) return
  await selectRole(role)
  showMenuPermModal.value = true
}

async function openDataScopeSettings(role) {
  if (!role) return
  await selectRole(role)
  showDataScopeModal.value = true
}

function closeMenuPermModal() {
  showMenuPermModal.value = false
}

function closeDataScopeModal() {
  showDataScopeModal.value = false
}

function toggleMenu(menuId, checked) {
  const set = new Set(selectedMenuIds.value)
  if (checked) {
    set.add(menuId)
  } else {
    set.delete(menuId)
  }
  selectedMenuIds.value = set
}

function searchRoles() {
  rolePagination.page = 1
  loadRoles()
}

function changeRolePage(page) {
  if (page < 1 || page > roleTotalPages.value) return
  rolePagination.page = page
  loadRoles()
}

function onRolePageSizeChange() {
  rolePagination.page = 1
  loadRoles()
}

async function saveRoleMenus() {
  if (!selectedRole.value) {
    errorMessage.value = '请先选择角色'
    return
  }
  errorMessage.value = ''
  try {
    await replaceRoleMenuIds(selectedRole.value.id, Array.from(selectedMenuIds.value))
    closeMenuPermModal()
  } catch (err) {
    errorMessage.value = err.message
  }
}

function addScopeRow() {
  scopeRows.value.push({ moduleCode: '', scope: 'SELF_ONLY' })
}

function removeScopeRow(idx) {
  scopeRows.value.splice(idx, 1)
}

async function saveDataScopes() {
  if (!selectedRole.value) {
    errorMessage.value = '请先选择角色'
    return
  }
  const payload = scopeRows.value
    .filter((x) => x.moduleCode)
    .map((x) => ({
      moduleCode: x.moduleCode.trim().toUpperCase(),
      scope: (x.scope || 'SELF_ONLY').toUpperCase()
    }))

  errorMessage.value = ''
  try {
    await replaceRoleDataScopes(selectedRole.value.id, payload)
    await selectRole(selectedRole.value)
    closeDataScopeModal()
  } catch (err) {
    errorMessage.value = err.message
  }
}

function resetRoleForm() {
  Object.assign(roleForm, {
    name: '',
    code: '',
    description: '',
    status: 1
  })
}

function openRoleCreate() {
  roleEditingId.value = null
  resetRoleForm()
  showRoleModal.value = true
}

function openRoleEdit(role = null) {
  const target = role || selectedRole.value
  if (!target) return
  selectedRole.value = target
  roleEditingId.value = target.id
  Object.assign(roleForm, {
    name: target.name || '',
    code: target.code || '',
    description: target.description || '',
    status: target.status ?? 1
  })
  showRoleModal.value = true
}

function closeRoleModal() {
  showRoleModal.value = false
}

async function submitRole() {
  if (!roleForm.name || !roleForm.code) {
    errorMessage.value = '角色名称和编码不能为空'
    return
  }

  const payload = {
    name: roleForm.name,
    code: roleForm.code,
    description: roleForm.description || null,
    status: roleForm.status ?? 1
  }

  errorMessage.value = ''
  try {
    if (roleEditingId.value) {
      await updateRole(roleEditingId.value, payload)
    } else {
      await createRole(payload)
    }
    showRoleModal.value = false
    await loadRoles()
    if (!roles.value.length) {
      selectedRole.value = null
      return
    }
    if (selectedRole.value) {
      const hit = roles.value.find((x) => x.id === selectedRole.value.id)
      if (hit) {
        await selectRole(hit)
        return
      }
    }
    await selectRole(roles.value[0])
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
    await Promise.all([loadRoles(), loadMenus()])
    if (roles.value.length) {
      await selectRole(roles.value[0])
    }
  } catch (err) {
    errorMessage.value = err.message
  }
})
</script>

<style scoped>
.permission-modal-card {
  width: min(1160px, 100%);
}

.permission-body {
  margin: 12px 0;
  max-height: 68vh;
  overflow: auto;
  padding-right: 4px;
}
</style>
