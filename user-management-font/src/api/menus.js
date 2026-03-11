import { http } from './http'

export function getMenuTree() {
  return http('/api/menus/tree')
}

export function createMenu(payload) {
  return http('/api/menus', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateMenu(id, payload) {
  return http(`/api/menus/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function deleteMenu(id) {
  return http(`/api/menus/${id}`, {
    method: 'DELETE'
  })
}
