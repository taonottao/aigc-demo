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

export function createRole(payload) {
  return http('/api/roles', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateRole(roleId, payload) {
  return http(`/api/roles/${roleId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function getRoleDataScopes(roleId) {
  return http(`/api/roles/${roleId}/data-scopes`)
}

export function replaceRoleDataScopes(roleId, scopes) {
  return http(`/api/roles/${roleId}/data-scopes`, {
    method: 'PUT',
    body: JSON.stringify(scopes)
  })
}
