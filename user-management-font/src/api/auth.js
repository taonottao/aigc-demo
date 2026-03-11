import { http } from './http'

export function getCaptcha() {
  return http('/api/auth/captcha')
}

export function login(payload) {
  return http('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function me() {
  return http('/api/auth/me')
}

export function secondVerify(password) {
  return http('/api/auth/second-verify', {
    method: 'POST',
    body: JSON.stringify({ password })
  })
}

