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

export function deleteUser(id, secondVerifyToken) {
  return http(`/api/users/${id}`, {
    method: 'DELETE',
    headers: secondVerifyToken ? { 'X-Second-Verify': secondVerifyToken } : {}
  })
}

export function listRoles() {
  return http('/api/roles/simple')
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

async function handleRawResponse(response) {
  if (!response.ok) {
    let message = `Request failed: ${response.status}`
    try {
      const data = await response.json()
      if (data?.message) {
        message = data.message
      }
    } catch (_) {
      // ignore parsing error
    }
    throw new Error(message)
  }
  return response
}

export async function importUsers(file, params = {}) {
  const form = new FormData()
  form.append('file', file)
  const response = await fetch(`/api/users/import${buildSearch(params)}`, {
    method: 'POST',
    body: form
  })
  const raw = await handleRawResponse(response)
  return raw.json()
}

export async function exportUsers(params = {}) {
  const response = await fetch(`/api/users/export${buildSearch(params)}`, {
    method: 'GET'
  })
  const raw = await handleRawResponse(response)
  return raw.blob()
}
