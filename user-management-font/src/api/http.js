function buildHeaders(body, customHeaders = {}) {
  const token = localStorage.getItem('token')
  const headers = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...customHeaders
  }
  const hasContentType = Object.keys(headers).some((key) => key.toLowerCase() === 'content-type')
  if (!(body instanceof FormData) && !hasContentType) {
    headers['Content-Type'] = 'application/json'
  }
  return headers
}

async function parseError(response) {
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

export async function http(url, options = {}) {
  const { headers: customHeaders = {}, body, ...restOptions } = options
  const response = await fetch(url, {
    ...restOptions,
    body,
    headers: buildHeaders(body, customHeaders)
  })

  if (!response.ok) {
    await parseError(response)
  }

  if (response.status === 204) {
    return null
  }
  return response.json()
}

export async function httpBlob(url, options = {}) {
  const { headers: customHeaders = {}, body, ...restOptions } = options
  const response = await fetch(url, {
    ...restOptions,
    body,
    headers: buildHeaders(body, customHeaders)
  })
  if (!response.ok) {
    await parseError(response)
  }
  return response.blob()
}
