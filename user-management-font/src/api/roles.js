import { http } from './http'

export function listRoles() {
  return http('/api/roles')
}

export function pageRoles(params = {}) {
  const search = new URLSearchParams()
  if (params.page) search.set('page', String(params.page))
  if (params.size) search.set('size', String(params.size))
  if (params.keyword) search.set('keyword', params.keyword)
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return http(`/api/roles/page${suffix}`)
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
