import { http } from './http'

export function listUsers(params = {}) {
  const search = new URLSearchParams()
  if (params.orgId) search.set('orgId', String(params.orgId))
  if (params.keyword) search.set('keyword', params.keyword)
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return http(`/api/users${suffix}`)
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

export function deleteUser(id) {
  return http(`/api/users/${id}`, { method: 'DELETE' })
}
