import { http } from './http'

export function getOrgTree() {
  return http('/api/orgs/tree')
}

