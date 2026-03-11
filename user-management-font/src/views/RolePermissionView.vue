<template>
  <header class="topbar">
    <div>
      <div class="title">角色权限</div>
      <div class="meta">角色维护、菜单按钮权限绑定、数据权限规则（按模块）配置</div>
    </div>
    <div class="actions">
      <button v-if="hasPerm('role:add')" class="btn primary" @click="openRoleCreate">新增角色</button>
      <button v-if="hasPerm('perm:save')" class="btn" :disabled="!selectedRole" @click="openRoleEdit">编辑角色</button>
      <button v-if="hasPerm('perm:save')" class="btn" :disabled="!selectedRole" @click="saveRoleMenus">保存菜单绑定</button>
      <button v-if="hasPerm('perm:save')" class="btn" :disabled="!selectedRole" @click="saveDataScopes">保存数据权限</button>
    </div>
  </header>

  <section class="grid">
    <article class="card span-4">
      <h3>角色列表</h3>
      <p v-if="errorMessage" class="small" style="color: var(--danger)">{{ errorMessage }}</p>
      <table class="table">
        <thead><tr><th>角色</th><th>编码</th><th>状态</th></tr></thead>
        <tbody>
          <tr
            v-for="row in roles"
            :key="row.id"
            :style="{ background: selectedRole?.id === row.id ? 'rgba(0,168,204,0.08)' : 'transparent', cursor: 'pointer' }"
            @click="selectRole(row)"
          >
            <td>{{ row.name }}</td>
            <td>{{ row.code }}</td>
            <td><span :class="['badge', row.status === 1 ? 'ok' : 'warn']">{{ row.status === 1 ? '启用' : '禁用' }}</span></td>
          </tr>
          <tr v-if="!roles.length"><td colspan="3">暂无角色</td></tr>
        </tbody>
      </table>
    </article>

    <article class="card span-8">
      <h3>菜单与按钮权限绑定</h3>
      <p class="small">当前角色：{{ selectedRole ? `${selectedRole.name} (${selectedRole.code})` : '未选择' }}</p>
      <table class="table">
        <thead><tr><th>绑定</th><th>名称</th><th>类型</th><th>路由</th><th>权限标识</th></tr></thead>
        <tbody>
          <tr v-for="menu in flatMenus" :key="menu.id">
            <td><input type="checkbox" :checked="selectedMenuIds.has(menu.id)" @change="toggleMenu(menu.id, $event.target.checked)" /></td>
            <td>{{ menu.indent }}{{ menu.name }}</td>
            <td>{{ menu.type }}</td>
            <td>{{ menu.path || '-' }}</td>
            <td>{{ menu.permCode || '-' }}</td>
          </tr>
          <tr v-if="!flatMenus.length"><td colspan="5">暂无菜单数据</td></tr>
        </tbody>
      </table>
    </article>

    <article class="card span-6">
      <h3>数据权限规则（按模块）</h3>
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
    </article>

    <article class="card span-6">
      <h3>菜单管理（联调）</h3>
      <div class="actions" style="margin-bottom: 8px">
        <button v-if="hasPerm('perm:save')" class="btn" @click="openMenuCreate">新增菜单/按钮</button>
        <button class="btn" @click="loadMenus">刷新菜单</button>
      </div>
      <table class="table">
        <thead><tr><th>ID</th><th>名称</th><th>类型</th><th>父级</th><th>权限标识</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="m in flatMenus" :key="`manage-${m.id}`">
            <td>{{ m.id }}</td>
            <td>{{ m.indent }}{{ m.name }}</td>
            <td>{{ m.type }}</td>
            <td>{{ m.parentId }}</td>
            <td>{{ m.permCode || '-' }}</td>
            <td>
              <button v-if="hasPerm('perm:save')" class="btn" @click="openMenuEdit(m)">编辑</button>
              <button v-if="hasPerm('perm:save')" class="btn danger" @click="removeMenu(m)">删除</button>
            </td>
          </tr>
          <tr v-if="!flatMenus.length"><td colspan="6">暂无菜单</td></tr>
        </tbody>
      </table>
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

  <section v-if="showMenuModal" class="modal" @click.self="closeMenuModal">
    <article class="modal-card">
      <h3>{{ menuEditingId ? '编辑菜单/按钮' : '新增菜单/按钮' }}</h3>
      <div class="form-grid">
        <div class="field"><label>名称 *</label><input v-model.trim="menuForm.name" /></div>
        <div class="field"><label>类型 *</label>
          <select v-model="menuForm.type">
            <option value="MENU">MENU</option>
            <option value="BUTTON">BUTTON</option>
          </select>
        </div>
        <div class="field"><label>父级ID</label>
          <select v-model.number="menuForm.parentId">
            <option :value="0">根节点</option>
            <option v-for="opt in menuParentOptions" :key="opt.id" :value="opt.id">{{ opt.name }}</option>
          </select>
        </div>
        <div class="field"><label>状态</label>
          <select v-model.number="menuForm.status">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>
        <div class="field"><label>路由</label><input v-model.trim="menuForm.path" placeholder="MENU 可填，如 /users" /></div>
        <div class="field"><label>图标</label><input v-model.trim="menuForm.icon" /></div>
        <div class="field full"><label>权限标识</label><input v-model.trim="menuForm.permCode" placeholder="如 user:delete" /></div>
        <div class="field"><label>排序号</label><input v-model.number="menuForm.sortNo" type="number" /></div>
      </div>
      <div class="actions">
        <button class="btn" @click="closeMenuModal">取消</button>
        <button class="btn primary" @click="submitMenu">保存</button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { createMenu, deleteMenu, getMenuTree, updateMenu } from '../api/menus'
import {
  createRole,
  getRoleDataScopes,
  getRoleMenuIds,
  listRoles,
  replaceRoleDataScopes,
  replaceRoleMenuIds,
  updateRole
} from '../api/roles'

const roles = ref([])
const selectedRole = ref(null)
const menuTree = ref([])
const selectedMenuIds = ref(new Set())
const scopeRows = ref([])
const errorMessage = ref('')
const permissions = ref([])

const showRoleModal = ref(false)
const roleEditingId = ref(null)
const roleForm = reactive({
  name: '',
  code: '',
  description: '',
  status: 1
})

const showMenuModal = ref(false)
const menuEditingId = ref(null)
const menuForm = reactive({
  parentId: 0,
  name: '',
  path: '',
  icon: '',
  permCode: '',
  type: 'MENU',
  sortNo: 0,
  status: 1
})

const flatMenus = computed(() => flattenMenus(menuTree.value))
const menuParentOptions = computed(() => {
  return flatMenus.value
    .filter((x) => !menuEditingId.value || x.id !== menuEditingId.value)
    .map((x) => ({ id: x.id, name: `${x.name} (ID:${x.id})` }))
})

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
  roles.value = (await listRoles()) || []
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

function toggleMenu(menuId, checked) {
  const set = new Set(selectedMenuIds.value)
  if (checked) {
    set.add(menuId)
  } else {
    set.delete(menuId)
  }
  selectedMenuIds.value = set
}

async function saveRoleMenus() {
  if (!selectedRole.value) {
    errorMessage.value = '请先选择角色'
    return
  }
  errorMessage.value = ''
  try {
    await replaceRoleMenuIds(selectedRole.value.id, Array.from(selectedMenuIds.value))
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

function openRoleEdit() {
  if (!selectedRole.value) return
  roleEditingId.value = selectedRole.value.id
  Object.assign(roleForm, {
    name: selectedRole.value.name || '',
    code: selectedRole.value.code || '',
    description: selectedRole.value.description || '',
    status: selectedRole.value.status ?? 1
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
    if (selectedRole.value) {
      const hit = roles.value.find((x) => x.id === selectedRole.value.id)
      if (hit) {
        await selectRole(hit)
      }
    }
  } catch (err) {
    errorMessage.value = err.message
  }
}

function resetMenuForm() {
  Object.assign(menuForm, {
    parentId: 0,
    name: '',
    path: '',
    icon: '',
    permCode: '',
    type: 'MENU',
    sortNo: 0,
    status: 1
  })
}

function openMenuCreate() {
  menuEditingId.value = null
  resetMenuForm()
  showMenuModal.value = true
}

function openMenuEdit(menu) {
  menuEditingId.value = menu.id
  Object.assign(menuForm, {
    parentId: menu.parentId ?? 0,
    name: menu.name || '',
    path: menu.path || '',
    icon: menu.icon || '',
    permCode: menu.permCode || '',
    type: menu.type || 'MENU',
    sortNo: menu.sortNo ?? 0,
    status: menu.status ?? 1
  })
  showMenuModal.value = true
}

function closeMenuModal() {
  showMenuModal.value = false
}

async function submitMenu() {
  if (!menuForm.name || !menuForm.type) {
    errorMessage.value = '菜单名称和类型不能为空'
    return
  }

  const payload = {
    parentId: menuForm.parentId ?? 0,
    name: menuForm.name,
    path: menuForm.path || null,
    icon: menuForm.icon || null,
    permCode: menuForm.permCode || null,
    type: menuForm.type,
    sortNo: menuForm.sortNo ?? 0,
    status: menuForm.status ?? 1
  }

  errorMessage.value = ''
  try {
    if (menuEditingId.value) {
      await updateMenu(menuEditingId.value, payload)
    } else {
      await createMenu(payload)
    }
    showMenuModal.value = false
    await loadMenus()
  } catch (err) {
    errorMessage.value = err.message
  }
}

async function removeMenu(menu) {
  if (!window.confirm(`确认删除菜单 ${menu.name} 吗？`)) {
    return
  }
  errorMessage.value = ''
  try {
    await deleteMenu(menu.id)
    await loadMenus()
    if (selectedRole.value) {
      await selectRole(selectedRole.value)
    }
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
