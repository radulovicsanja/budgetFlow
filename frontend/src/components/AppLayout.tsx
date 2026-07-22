import { NavLink, Outlet } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { api } from '../api/client'

const baseLinks = [
  { to: '/', label: 'Pregled', end: true },
  { to: '/budgets', label: 'Budžet' },
  { to: '/categories', label: 'Kategorije' },
  { to: '/transactions', label: 'Transakcije' },
  { to: '/reports', label: 'Izvještaji' },
  { to: '/goals', label: 'Ciljevi' },
  { to: '/notifications', label: 'Bilješke' },
  { to: '/account', label: 'Nalog' },
]

export function AppLayout() {
  const { user, logout } = useAuth()
  const [unread, setUnread] = useState(0)
  const links =
    user?.role === 'ADMIN'
      ? [...baseLinks, { to: '/admin', label: 'Admin', end: false }]
      : baseLinks

  useEffect(() => {
    api<{ unreadCount: number }>('/api/notifications/unread-count')
      .then((r) => setUnread(r.unreadCount))
      .catch(() => setUnread(0))
  }, [])

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">BF</span>
          <div>
            <strong>BudgetFlow</strong>
            <p>{user?.username}</p>
          </div>
        </div>
        <nav>
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end} className={({ isActive }) => (isActive ? 'nav active' : 'nav')}>
              {l.label}
              {l.to === '/notifications' && unread > 0 && <span className="badge">{unread}</span>}
            </NavLink>
          ))}
        </nav>
        <button className="btn ghost logout" onClick={logout} type="button">
          Odjava
        </button>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
