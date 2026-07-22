import { FormEvent, useEffect, useState } from 'react'
import { api, apiOptional, ApiError, currentMonth } from '../api/client'
import { transactionTypeLabel } from '../api/labels'
import type { Category, RecurringTransaction, Transaction, TransactionType } from '../api/types'

function parseAmount(raw: string): number | null {
  const cleaned = raw.trim().replace(/\s/g, '').replace(',', '.')
  if (!cleaned || !/^\d+(\.\d{1,2})?$/.test(cleaned)) return null
  const n = Number(cleaned)
  if (!Number.isFinite(n) || n <= 0) return null
  return Math.round(n * 100) / 100
}

export default function TransactionsPage() {
  const [items, setItems] = useState<Transaction[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [recurring, setRecurring] = useState<RecurringTransaction[]>([])
  const [amount, setAmount] = useState('')
  const [type, setType] = useState<TransactionType>('EXPENSE')
  const [categoryId, setCategoryId] = useState('')
  const [description, setDescription] = useState('')
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))
  const [confirm, setConfirm] = useState(false)
  const [pendingConfirm, setPendingConfirm] = useState<unknown>(null)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  const [filterType, setFilterType] = useState('')
  const [filterCategory, setFilterCategory] = useState('')
  const [filterFrom, setFilterFrom] = useState('')
  const [filterTo, setFilterTo] = useState('')
  const [filterMin, setFilterMin] = useState('')
  const [filterMax, setFilterMax] = useState('')
  const [filterQ, setFilterQ] = useState('')

  const [rAmount, setRAmount] = useState('100')
  const [rType, setRType] = useState<TransactionType>('EXPENSE')
  const [rCategoryId, setRCategoryId] = useState('')
  const [rDesc, setRDesc] = useState('Stanarina')
  const [rDay, setRDay] = useState('1')

  const isExpense = type === 'EXPENSE'
  const expenseCategories = categories.filter((c) => c.name.toLowerCase() !== 'prihod')

  async function load() {
    const params = new URLSearchParams()
    if (filterType) params.set('type', filterType)
    if (filterCategory) params.set('categoryId', filterCategory)
    if (filterFrom) params.set('from', filterFrom)
    if (filterTo) params.set('to', filterTo)
    if (filterMin) params.set('minAmount', filterMin.replace(',', '.'))
    if (filterMax) params.set('maxAmount', filterMax.replace(',', '.'))
    if (filterQ.trim()) params.set('q', filterQ.trim())

    const qs = params.toString()
    const [tx, cats, rec] = await Promise.all([
      api<Transaction[]>(`/api/transactions${qs ? `?${qs}` : ''}`),
      api<Category[]>('/api/categories'),
      apiOptional<RecurringTransaction[]>('/api/recurring', []),
    ])
    setItems(tx)
    setCategories(cats)
    setRecurring(rec)
    if (!categoryId && cats[0]) setCategoryId(String(cats.find((c) => c.name.toLowerCase() !== 'prihod')?.id || cats[0].id))
    if (!rCategoryId && cats[0]) {
      const first = cats.find((c) => c.name.toLowerCase() !== 'prihod')
      if (first) setRCategoryId(String(first.id))
    }
  }

  useEffect(() => {
    load().catch((e) => setError(e.message))
  }, [])

  async function applyFilters(e?: FormEvent) {
    e?.preventDefault()
    setError('')
    try {
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška pri filteru')
    }
  }

  async function submit(e: FormEvent, forceConfirm = false) {
    e.preventDefault()
    setError('')
    setMsg('')
    setPendingConfirm(null)

    const parsed = parseAmount(amount)
    if (parsed == null) {
      setError('Unesi ispravan iznos, npr. 56,6 ili 56.60')
      return
    }
    if (isExpense && !categoryId) {
      setError('Za trošak moraš odabrati kategoriju.')
      return
    }

    try {
      const body: Record<string, unknown> = {
        amount: parsed,
        type,
        description: description || null,
        date,
        confirmFromUnallocated: isExpense ? forceConfirm || confirm : false,
      }
      if (isExpense) body.categoryId = Number(categoryId)

      await api('/api/transactions', { method: 'POST', body: JSON.stringify(body) })
      setMsg('Transakcija sačuvana.')
      setAmount('')
      setDescription('')
      await load()
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setPendingConfirm(err.body)
        setError(err.message)
      } else {
        setError(err instanceof ApiError ? err.message : 'Greška')
      }
    }
  }

  async function createRecurring(e: FormEvent) {
    e.preventDefault()
    setError('')
    const parsed = parseAmount(rAmount)
    if (parsed == null) {
      setError('Iznos ponavljajuće transakcije nije ispravan.')
      return
    }
    try {
      await api('/api/recurring', {
        method: 'POST',
        body: JSON.stringify({
          amount: parsed,
          type: rType,
          description: rDesc,
          categoryId: rType === 'EXPENSE' ? Number(rCategoryId) : null,
          dayOfMonth: Number(rDay),
          active: true,
        }),
      })
      setMsg('Ponavljajuća transakcija sačuvana.')
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  async function removeRecurring(id: number) {
    await api(`/api/recurring/${id}`, { method: 'DELETE' })
    await load()
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Transakcije</h1>
          <p>Prihodi, rashodi, filteri i ponavljajuće stavke</p>
        </div>
      </div>

      {msg && <div className="alert ok">{msg}</div>}
      {error && <div className="alert error">{error}</div>}
      {pendingConfirm && isExpense && (
        <div className="alert warn stack">
          <p>Potrebna je potvrda uzimanja iz neraspoređenog.</p>
          <button className="btn secondary" type="button" onClick={(e) => submit(e as unknown as FormEvent, true)}>
            Potvrdi i snimi
          </button>
        </div>
      )}

      <div className="panel" style={{ marginBottom: '1rem' }}>
        <h2>Filteri / pretraga</h2>
        <form className="form" onSubmit={applyFilters} style={{ marginTop: '0.8rem' }}>
          <div className="form row">
            <label>
              Tip
              <select value={filterType} onChange={(e) => setFilterType(e.target.value)}>
                <option value="">Svi</option>
                <option value="EXPENSE">Trošak</option>
                <option value="INCOME">Prihod</option>
              </select>
            </label>
            <label>
              Kategorija
              <select value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)}>
                <option value="">Sve</option>
                {expenseCategories.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="form row">
            <label>
              Od datuma
              <input type="date" value={filterFrom} onChange={(e) => setFilterFrom(e.target.value)} />
            </label>
            <label>
              Do datuma
              <input type="date" value={filterTo} onChange={(e) => setFilterTo(e.target.value)} />
            </label>
          </div>
          <div className="form row">
            <label>
              Min iznos
              <input value={filterMin} onChange={(e) => setFilterMin(e.target.value)} placeholder="npr. 10" />
            </label>
            <label>
              Max iznos
              <input value={filterMax} onChange={(e) => setFilterMax(e.target.value)} placeholder="npr. 200" />
            </label>
          </div>
          <label>
            Pretraga (opis / kategorija)
            <input value={filterQ} onChange={(e) => setFilterQ(e.target.value)} placeholder="npr. market" />
          </label>
          <div className="inline">
            <button className="btn" type="submit">Primijeni</button>
            <button
              className="btn ghost"
              type="button"
              onClick={() => {
                setFilterType('')
                setFilterCategory('')
                setFilterFrom('')
                setFilterTo('')
                setFilterMin('')
                setFilterMax('')
                setFilterQ('')
                setTimeout(() => load().catch((e) => setError(e.message)), 0)
              }}
            >
              Reset
            </button>
          </div>
        </form>
      </div>

      <div className="grid two">
        <div className="panel">
          <h2>Nova transakcija</h2>
          <form className="form" onSubmit={(e) => submit(e)} style={{ marginTop: '0.8rem' }}>
            <div className="form row">
              <label>
                Iznos (€)
                <input type="text" inputMode="decimal" placeholder="npr. 56,6" value={amount} onChange={(e) => setAmount(e.target.value)} required />
              </label>
              <label>
                Tip
                <select value={type} onChange={(e) => setType(e.target.value as TransactionType)}>
                  <option value="EXPENSE">Trošak</option>
                  <option value="INCOME">Prihod</option>
                </select>
              </label>
            </div>
            {isExpense && (
              <label>
                Kategorija
                <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
                  <option value="">Odaberi…</option>
                  {expenseCategories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </label>
            )}
            <label>
              Datum
              <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
            </label>
            <label>
              Opis
              <input value={description} onChange={(e) => setDescription(e.target.value)} placeholder={isExpense ? `npr. ${currentMonth()}` : 'npr. plata'} />
            </label>
            {isExpense && (
              <label className="inline">
                <input type="checkbox" checked={confirm} onChange={(e) => setConfirm(e.target.checked)} />
                Dozvoli uzimanje iz neraspoređenog
              </label>
            )}
            <button className="btn" type="submit">Sačuvaj</button>
          </form>

          <h2 style={{ marginTop: '1.4rem' }}>Ponavljajuće</h2>
          <form className="form" onSubmit={createRecurring} style={{ marginTop: '0.8rem' }}>
            <div className="form row">
              <label>
                Iznos
                <input value={rAmount} onChange={(e) => setRAmount(e.target.value)} required />
              </label>
              <label>
                Dan u mjesecu
                <input type="number" min={1} max={28} value={rDay} onChange={(e) => setRDay(e.target.value)} required />
              </label>
            </div>
            <label>
              Tip
              <select value={rType} onChange={(e) => setRType(e.target.value as TransactionType)}>
                <option value="EXPENSE">Trošak (npr. stanarina)</option>
                <option value="INCOME">Prihod (npr. plata)</option>
              </select>
            </label>
            {rType === 'EXPENSE' && (
              <label>
                Kategorija
                <select value={rCategoryId} onChange={(e) => setRCategoryId(e.target.value)} required>
                  {expenseCategories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </label>
            )}
            <label>
              Opis
              <input value={rDesc} onChange={(e) => setRDesc(e.target.value)} />
            </label>
            <button className="btn secondary" type="submit">Dodaj ponavljajuću</button>
          </form>
          <div className="stack" style={{ marginTop: '0.8rem' }}>
            {recurring.map((r) => (
              <div key={r.id} className="inline" style={{ justifyContent: 'space-between' }}>
                <span>
                  {transactionTypeLabel(r.type)} {Number(r.amount).toFixed(2)} € — dan {r.dayOfMonth}
                  {r.description ? ` (${r.description})` : ''} · sljedeće: {r.nextRunDate}
                </span>
                <button className="btn danger" type="button" onClick={() => removeRecurring(r.id)}>Obriši</button>
              </div>
            ))}
          </div>
        </div>

        <div className="panel">
          <h2>Lista ({items.length})</h2>
          <table className="table">
            <thead>
              <tr>
                <th>Datum</th>
                <th>Tip</th>
                <th>Kategorija</th>
                <th>Iznos</th>
              </tr>
            </thead>
            <tbody>
              {items.map((t) => (
                <tr key={t.id}>
                  <td>{t.date}</td>
                  <td>
                    <span className={`pill ${t.type === 'EXPENSE' ? 'expense' : ''}`}>
                      {transactionTypeLabel(t.type)}
                    </span>
                  </td>
                  <td>
                    {t.type === 'INCOME'
                      ? '—'
                      : t.category?.name && t.category.name.toLowerCase() !== 'prihod'
                        ? t.category.name
                        : '—'}
                  </td>
                  <td>{Number(t.amount).toFixed(2)} €</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
