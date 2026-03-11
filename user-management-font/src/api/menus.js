import { http } from './http'

export function getMenuTree() {
  return http('/api/menus/tree')
}

