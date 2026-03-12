import { http } from './http'

export function getLoginLogs() {
  return http('/api/logs/login')
}

export function getOperationLogs() {
  return http('/api/logs/operations')
}

export function pageLoginLogs(params = {}) {
  const search = new URLSearchParams()
  if (params.page) search.set('page', String(params.page))
  if (params.size) search.set('size', String(params.size))
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return http(`/api/logs/login/page${suffix}`)
}

export function pageOperationLogs(params = {}) {
  const search = new URLSearchParams()
  if (params.page) search.set('page', String(params.page))
  if (params.size) search.set('size', String(params.size))
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return http(`/api/logs/operations/page${suffix}`)
}
