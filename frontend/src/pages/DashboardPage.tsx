import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, currentMonth } from '../api/client'
import type { Notification, UserReport } from '../api/types'

function formatMonthLabel(month: string) {
  const [y, m] = month.split('-').map(Number)
  if (!y || !m) return month
  const label = new Date(y, m - 1, 1).toLocaleDateString('bs-BA', {
    month: 'long',
    year: 'numeric',
  })
  return label.charAt(0).toUpperCase() + label.slice(1)
}

export default function DashboardPage() {
  const month = currentMonth()
  const today = useMemo(() => new Date(), [])
  const [summary, setSummary] = useState<UserReport | null>(null)
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [error, setError] = useState('')

  const day = today.getDate()
  const weekday = today.toLocaleDateString('bs-BA', { weekday: 'long' })
  const monthShort = today.toLocaleDateString('bs-BA', { month: 'short' }).replace('.', '')
  const year = today.getFullYear()
  const fullDate = today.toLocaleDateString('bs-BA', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })

  useEffect(() => {
    Promise.all([
      api<UserReport>(`/api/reports/summary?month=${month}`),
      api<Notification[]>('/api/notifications?unreadOnly=true'),
    ])
      .then(([s, n]) => {
        setSummary(s)
        setNotifications(n.slice(0, 5))
      })
      .catch((e) => setError(e.message || 'Greška pri učitavanju.'))
  }, [month])

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Pregled</h1>
          <p>
            Finansijsko stanje za <strong>{formatMonthLabel(month)}</strong>
            <span className="muted"> — prati tekući mjesec</span>
          </p>
        </div>
        <div className="page-header__actions">
          <div className="date-cube" title={fullDate} aria-label={`Danas je ${fullDate}`}>
            <span className="date-cube__month">{monthShort}</span>
            <span className="date-cube__day">{day}</span>
            <span className="date-cube__weekday">{weekday}</span>
            <span className="date-cube__year">{year}</span>
          </div>
          <Link className="btn" to="/transactions">Nova transakcija</Link>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      <div className="grid stats">
        <div className="panel stat">
          <span>Prihodi</span>
          <strong>{Number(summary?.totalIncome ?? 0).toFixed(2)} €</strong>
        </div>
        <div className="panel stat">
          <span>Rashodi</span>
          <strong>{Number(summary?.totalExpenses ?? 0).toFixed(2)} €</strong>
        </div>
        <div className="panel stat">
          <span>Ušteda</span>
          <strong>{Number(summary?.totalSavings ?? 0).toFixed(2)} €</strong>
        </div>
      </div>

      <div className="grid two" style={{ marginTop: '1rem' }}>
        <div className="panel">
          <h2>Brzi linkovi</h2>
          <div className="stack" style={{ marginTop: '0.8rem' }}>
            <Link to="/budgets">Postavi / uredi mjesečni budžet</Link>
            <Link to="/categories">Pregled kategorija</Link>
            <Link to="/reports">Izvještaji i CSV</Link>
          </div>
        </div>
        <div className="panel">
          <div className="inline" style={{ justifyContent: 'space-between' }}>
            <h2>Nepročitane bilješke</h2>
            <Link to="/notifications">Sve</Link>
          </div>
          <div className="stack" style={{ marginTop: '0.8rem' }}>
            {notifications.length === 0 && <p className="muted">Nema nepročitanih.</p>}
            {notifications.map((n) => (
              <div key={n.id}>
                <strong>{n.title}</strong>
                <p>{n.message}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
