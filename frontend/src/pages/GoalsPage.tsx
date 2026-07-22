import { FormEvent, useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import type { SavingsGoal } from '../api/types'

export default function GoalsPage() {
  const [goals, setGoals] = useState<SavingsGoal[]>([])
  const [title, setTitle] = useState('')
  const [targetAmount, setTargetAmount] = useState('500')
  const [currentAmount, setCurrentAmount] = useState('0')
  const [deadline, setDeadline] = useState('')
  const [note, setNote] = useState('')
  const [addAmounts, setAddAmounts] = useState<Record<number, string>>({})
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  async function load() {
    setGoals(await api<SavingsGoal[]>('/api/goals'))
  }

  useEffect(() => {
    load().catch((e) => setError(e.message))
  }, [])

  async function create(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await api('/api/goals', {
        method: 'POST',
        body: JSON.stringify({
          title,
          targetAmount: Number(targetAmount),
          currentAmount: Number(currentAmount || 0),
          deadline,
          note: note || null,
        }),
      })
      setTitle('')
      setTargetAmount('500')
      setCurrentAmount('0')
      setDeadline('')
      setNote('')
      setMsg('Cilj kreiran.')
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  async function addToGoal(id: number) {
    const raw = addAmounts[id]
    const amount = Number(String(raw || '').replace(',', '.'))
    if (!amount || amount <= 0) {
      setError('Unesi pozitivan iznos za uplatu na cilj.')
      return
    }
    setError('')
    try {
      await api(`/api/goals/${id}/add`, {
        method: 'POST',
        body: JSON.stringify({ amount }),
      })
      setAddAmounts((prev) => ({ ...prev, [id]: '' }))
      setMsg('Iznos dodat na cilj.')
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  async function remove(id: number) {
    await api(`/api/goals/${id}`, { method: 'DELETE' })
    await load()
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Ciljevi štednje</h1>
          <p>Prati napredak ka željenom iznosu</p>
        </div>
      </div>

      {msg && <div className="alert ok">{msg}</div>}
      {error && <div className="alert error">{error}</div>}

      <div className="grid two">
        <div className="panel">
          <h2>Novi cilj</h2>
          <form className="form" onSubmit={create} style={{ marginTop: '0.8rem' }}>
            <label>
              Naslov
              <input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="npr. Uštedi za odmor"
                required
              />
            </label>
            <div className="form row">
              <label>
                Ciljni iznos (€)
                <input type="number" step="0.01" value={targetAmount} onChange={(e) => setTargetAmount(e.target.value)} required />
              </label>
              <label>
                Trenutno (€)
                <input type="number" step="0.01" value={currentAmount} onChange={(e) => setCurrentAmount(e.target.value)} />
              </label>
            </div>
            <label>
              Rok
              <input type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} required />
            </label>
            <label>
              Napomena
              <input value={note} onChange={(e) => setNote(e.target.value)} />
            </label>
            <button className="btn" type="submit">Sačuvaj cilj</button>
          </form>
        </div>

        <div className="panel stack">
          {goals.length === 0 && <p className="muted">Još nema ciljeva.</p>}
          {goals.map((g) => (
            <div key={g.id} className="panel" style={{ boxShadow: 'none' }}>
              <div className="inline" style={{ justifyContent: 'space-between' }}>
                <strong>{g.title}</strong>
                {g.completed && <span className="pill">Ostvareno</span>}
              </div>
              <p className="muted">Rok: {g.deadline}</p>
              <p>
                {Number(g.currentAmount).toFixed(2)} € / {Number(g.targetAmount).toFixed(2)} €
                {' '}({Number(g.progressPercent).toFixed(1)}%)
              </p>
              <div className="progress">
                <span style={{ width: `${Math.min(100, Number(g.progressPercent))}%` }} />
              </div>
              <div className="inline" style={{ marginTop: '0.7rem' }}>
                <input
                  style={{ maxWidth: 120 }}
                  placeholder="Dodaj €"
                  value={addAmounts[g.id] || ''}
                  onChange={(e) => setAddAmounts((prev) => ({ ...prev, [g.id]: e.target.value }))}
                />
                <button className="btn secondary" type="button" onClick={() => addToGoal(g.id)}>
                  Dodaj
                </button>
                <button className="btn danger" type="button" onClick={() => remove(g.id)}>
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
