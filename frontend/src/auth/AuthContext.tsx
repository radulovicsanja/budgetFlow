import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { api, getToken, setToken } from '../api/client'
import type { User } from '../api/types'

const USER_KEY = 'bf_user'

function loadCachedUser(): User | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

function cacheUser(user: User | null) {
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
  else localStorage.removeItem(USER_KEY)
}

interface AuthState {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (username: string, email: string, password: string) => Promise<void>
  logout: () => void
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const applyUser = useCallback((u: User | null) => {
    setUser(u)
    cacheUser(u)
  }, [])

  const refreshUser = useCallback(async () => {
    if (!getToken()) {
      applyUser(null)
      return
    }
    try {
      const me = await api<User>('/api/users/me')
      applyUser(me)
    } catch {
      // zadrži keširanog usera ako /me ne uspije
      const cached = loadCachedUser()
      if (cached) setUser(cached)
      else {
        setToken(null)
        applyUser(null)
      }
    }
  }, [applyUser])

  useEffect(() => {
    ;(async () => {
      try {
        if (getToken()) {
          const cached = loadCachedUser()
          if (cached) setUser(cached)
          await refreshUser()
        }
      } catch {
        // ignore
      } finally {
        setLoading(false)
      }
    })()
  }, [refreshUser])

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await api<{ token: string; email: string; username?: string; role?: string }>(
        '/auth/login',
        {
          method: 'POST',
          body: JSON.stringify({ email: email.trim().toLowerCase(), password }),
        },
      )
      setToken(res.token)
      applyUser({
        id: 1,
        email: res.email,
        username: res.username || res.email,
        role: (res.role as User['role']) || 'USER',
      })
    },
    [applyUser],
  )

  const register = useCallback(
    async (username: string, email: string, password: string) => {
      const res = await api<{ token: string; email: string; username: string; role?: string }>(
        '/api/users/register',
        {
          method: 'POST',
          body: JSON.stringify({
            username,
            email: email.trim().toLowerCase(),
            password,
          }),
        },
      )
      setToken(res.token)
      applyUser({
        id: 1,
        email: res.email,
        username: res.username,
        role: (res.role as User['role']) || 'USER',
      })
    },
    [applyUser],
  )

  const logout = useCallback(() => {
    setToken(null)
    applyUser(null)
  }, [applyUser])

  const value = useMemo(
    () => ({ user, loading, login, register, logout, refreshUser }),
    [user, loading, login, register, logout, refreshUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
