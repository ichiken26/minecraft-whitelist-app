import { createHash, timingSafeEqual } from 'node:crypto'
import { getRequestHeader, createError, setResponseHeader } from 'h3'

export default defineEventHandler((event) => {
  const password = process.env.ADMIN_PASSWORD
  if (!password || password === 'replace-with-long-random-secret') throw createError({ statusCode: 503, message: 'ADMIN_PASSWORD must be configured' })
  const header = getRequestHeader(event, 'authorization') || ''
  let supplied = ''
  if (header.startsWith('Basic ')) {
    try {
      const decoded = Buffer.from(header.slice(6), 'base64').toString('utf8')
      const separator = decoded.indexOf(':')
      if (separator >= 0) supplied = decoded.slice(separator + 1)
    } catch { /* reject */ }
  }
  const a = createHash('sha256').update(supplied).digest()
  const b = createHash('sha256').update(password).digest()
  if (!timingSafeEqual(a, b)) {
    setResponseHeader(event, 'WWW-Authenticate', 'Basic realm="Kokage Whitelist"')
    throw createError({ statusCode: 401, message: 'Authentication required' })
  }
  if (event.method !== 'GET' && event.method !== 'HEAD') {
    const host = getRequestHeader(event, 'host')
    const origin = getRequestHeader(event, 'origin')
    if (!origin || !host || new URL(origin).host !== host) throw createError({ statusCode: 403, message: 'Origin mismatch' })
  }
})
