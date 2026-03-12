import { http, httpBlob } from './http'

export function listUsers(params = {}) {
  const search = new URLSearchParams()
  if (params.orgId) search.set('orgId', String(params.orgId))
  if (params.keyword) search.set('keyword', params.keyword)
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return http(`/api/users${suffix}`)
}

export function pageUsers(params = {}) {
  const search = new URLSearchParams()
  if (params.orgId) search.set('orgId', String(params.orgId))
  if (params.keyword) search.set('keyword', params.keyword)
  if (params.page) search.set('page', String(params.page))
  if (params.size) search.set('size', String(params.size))
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return http(`/api/users/page${suffix}`)
}

export function createUser(payload) {
  return http('/api/users', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateUser(id, payload) {
  return http(`/api/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function deleteUser(id, secondVerifyToken) {
  return http(`/api/users/${id}`, {
    method: 'DELETE',
    headers: secondVerifyToken ? { 'X-Second-Verify': secondVerifyToken } : {}
  })
}

export function resetPassword(id, newPassword) {
  return http(`/api/users/${id}/reset-password`, {
    method: 'POST',
    body: JSON.stringify({ newPassword })
  })
}

export function listUserRoleIds(id) {
  return http(`/api/users/${id}/roles`)
}

export function replaceUserRoleIds(id, ids) {
  return http(`/api/users/${id}/roles`, {
    method: 'PUT',
    body: JSON.stringify({ ids })
  })
}

function buildSearch(params = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value))
    }
  })
  return search.toString() ? `?${search.toString()}` : ''
}

export async function importUsers(file, params = {}) {
  const form = new FormData()
  form.append('file', file)
  return http(`/api/users/import${buildSearch(params)}`, {
    method: 'POST',
    body: form,
    headers: {}
  })
}

export async function exportUsers(params = {}) {
  return httpBlob(`/api/users/export${buildSearch(params)}`, { method: 'GET' })
}
