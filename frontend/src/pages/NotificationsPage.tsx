import { FormEvent, useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import type { Notification } from '../api/types'

function noteTypeLabel(type: string) {
  if (type === 'BILL_REMINDER') return 'Podsjetnik'
  if (type === 'BUDGET_WARNING') return 'Upozorenje'
  if (type === 'INFO') return 'Info'
  return type
}

export default function NotificationsPage() {
  const [items, setItems] = useState<Notification[]>([])
  const [title, setTitle] = useState('')
  const [message, setMessage] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  async function load() {
    setItems(await api<Notification[]>('/api/notifications'))
  }

  useEffect(() => {
    load().catch((e) => setError(e.message))
  }, [])

  async function create(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await api('/api/notifications', {
        method: 'POST',
        body: JSON.stringify({
          title,
          message,
          type: 'BILL_REMINDER',
          dueDate: dueDate || null,
        }),
      })
      setTitle('')
      setMessage('')
      setDueDate('')
      setMsg('Bilješka sačuvana.')
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  async function markRead(id: number) {
    await api(`/api/notifications/${id}/read`, { method: 'PUT' })
    await load()
  }

  async function markAll() {
    await api('/api/notifications/read-all', { method: 'PUT' })
    await load()
  }

  async function remove(id: number) {
    await api(`/api/notifications/${id}`, { method: 'DELETE' })
    await load()
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Bilješke</h1>
          <p>Podsjetnici za račune, bilješke i upozorenja o potrošnji</p>
        </div>
        <button className="btn ghost" type="button" onClick={markAll}>
          Označi sve pročitano
        </button>
      </div>

      {msg && <div className="alert ok">{msg}</div>}
      {error && <div className="alert error">{error}</div>}

      <div className="grid two">
        <div className="panel">
          <h2>Nova bilješka</h2>
          <form className="form" onSubmit={create} style={{ marginTop: '0.8rem' }}>
            <label>
              Naslov
              <input value={title} onChange={(e) => setTitle(e.target.value)} required />
            </label>
            <label>
              Tekst
              <textarea value={message} onChange={(e) => setMessage(e.target.value)} required rows={3} />
            </label>
            <label>
              Datum (opciono)
              <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
            </label>
            <button className="btn" type="submit">Sačuvaj</button>
          </form>
        </div>

        <div className="panel stack">
          {items.length === 0 && <p className="muted">Nema bilješki.</p>}
          {items.map((n) => (
            <div key={n.id} className="panel" style={{ boxShadow: 'none' }}>
              <div className="inline" style={{ justifyContent: 'space-between' }}>
                <strong>{n.title}</strong>
                <span className="pill">{noteTypeLabel(n.type)}</span>
              </div>
              <p>{n.message}</p>
              {n.dueDate && <p className="muted">Datum: {n.dueDate}</p>}
              <div className="inline">
                {!n.read && (
                  <button className="btn ghost" type="button" onClick={() => markRead(n.id)}>
                    Pročitano
                  </button>
                )}
                <button className="btn danger" type="button" onClick={() => remove(n.id)}>
                  Obriši
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
