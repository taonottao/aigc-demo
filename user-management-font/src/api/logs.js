import { http } from './http'

export function getLoginLogs() {
  return http('/api/logs/login')
}

export function getOperationLogs() {
  return http('/api/logs/operations')
}

