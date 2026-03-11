import { http } from './http'

export function getOrgTree() {
  return http('/api/orgs/tree')
}

export function createOrg(payload) {
  return http('/api/orgs', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateOrg(id, payload) {
  return http(`/api/orgs/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function deleteOrg(id, secondVerifyToken) {
  return http(`/api/orgs/${id}`, {
    method: 'DELETE',
    headers: secondVerifyToken ? { 'X-Second-Verify': secondVerifyToken } : {}
  })
}

export function listOrgUsers(orgId) {
  return http(`/api/orgs/${orgId}/users`)
}

export function addUserToOrg(orgId, userId) {
  return http(`/api/orgs/${orgId}/users/${userId}`, {
    method: 'PUT'
  })
}

export function removeUserFromOrg(orgId, userId) {
  return http(`/api/orgs/${orgId}/users/${userId}`, {
    method: 'DELETE'
  })
}
