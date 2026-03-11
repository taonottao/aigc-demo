import { http } from './http'

export function listRoles() {
  return http('/api/roles')
}

export function getRoleMenuIds(roleId) {
  return http(`/api/roles/${roleId}/menus`)
}

export function replaceRoleMenuIds(roleId, ids) {
  return http(`/api/roles/${roleId}/menus`, {
    method: 'PUT',
    body: JSON.stringify({ ids })
  })
}

