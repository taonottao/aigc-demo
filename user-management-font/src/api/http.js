export async function http(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })

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

  if (response.status === 204) {
    return null
  }
  return response.json()
}
