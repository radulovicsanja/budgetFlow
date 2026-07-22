const TOKEN_KEY = 'bf_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

function friendlyMessage(status: number, body: unknown, path: string): string {
  if (status === 0) {
    return 'Backend nije pokrenut. Pokreni Spring Boot (port 8083), pa pokušaj opet.'
  }
  if (typeof body === 'object' && body && 'message' in body) {
    const m = String((body as { message?: string }).message || '').trim()
    if (m) return m
  }
  if (typeof body === 'string' && body.trim()) {
    return body.trim().slice(0, 300)
  }
  if (path.includes('/register')) return 'Greška pri registraciji'
  if (path.includes('/login')) return 'Pogrešan email ili lozinka.'
  if (status === 401) return 'Pogrešan email ili lozinka.'
  if (status === 403) return 'Nemate dozvolu za ovu akciju.'
  if (status === 404) {
    return `Endpoint nije pronađen (${path}). Restartuj Spring Boot da učita novi kod.`
  }
  if (status >= 500) return `Greška na serveru (${status}). Pogledaj konzolu Spring Boot-a.`
  return `Greška (${status || '?'})`
}

export class ApiError extends Error {
  status: number
  body: unknown

  constructor(status: number, body: unknown, path = '') {
    super(friendlyMessage(status, body, path))
    this.status = status
    this.body = body
  }
}

export async function api<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers || {})
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  let res: Response
  try {
    res = await fetch(path, { ...options, headers })
  } catch {
    throw new ApiError(0, null, path)
  }

  if (res.status === 204) return undefined as T

  const contentType = res.headers.get('content-type') || ''
  const isJson = contentType.includes('application/json')
  let body: unknown = null
  if (isJson) {
    body = await res.json().catch(() => null)
  } else {
    const text = await res.text().catch(() => '')
    body = text || null
  }

  if (!res.ok) throw new ApiError(res.status, body, path)
  return body as T
}

/** API poziv koji pri grešci vrati fallback umjesto throw. */
export async function apiOptional<T>(path: string, fallback: T): Promise<T> {
  try {
    return await api<T>(path)
  } catch {
    return fallback
  }
}

export function currentMonth() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
