import { FormEvent, useEffect, useState } from 'react'
import { api, ApiError, currentMonth } from '../api/client'
import type { Budget, BudgetCategory } from '../api/types'

export default function BudgetsPage() {
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [selected, setSelected] = useState<Budget | null>(null)
  const [allocations, setAllocations] = useState<BudgetCategory[]>([])
  const [month, setMonth] = useState(currentMonth())
  const [totalAmount, setTotalAmount] = useState('1000')
  const [additionalIncome, setAdditionalIncome] = useState('0')
  const [categoryId, setCategoryId] = useState('')
  const [percentage, setPercentage] = useState('10')
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')
  const [categories, setCategories] = useState<{ id: number; name: string }[]>([])

  async function load() {
    setError('')
    try {
      const list = await api<Budget[]>('/api/budgets/me')
      setBudgets(list)
      if (list.length) {
        setSelected((prev) => prev ?? list[0])
      }
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Greška pri učitavanju budžeta')
    }

    try {
      const cats = await api<{ id: number; name: string }[]>('/api/categories')
      setCategories(cats)
    } catch (e) {
      setError((prev) =>
          prev
              ? prev
              : e instanceof ApiError
                  ? e.message
                  : 'Greška pri učitavanju kategorija',
      )
    }
  }

  useEffect(() => {
    load()
  }, [])

  useEffect(() => {
    if (!selected) return
    api<BudgetCategory[]>(`/api/budget-categories/budget/${selected.id}`)
        .then(setAllocations)
        .catch(() => setAllocations([]))
  }, [selected])

  async function createBudget(e: FormEvent) {
    e.preventDefault()
    setError('')
    setMsg('')
    try {
      const b = await api<Budget>('/api/budgets', {
        method: 'POST',
        body: JSON.stringify({
          month,
          totalAmount: Number(totalAmount),
          additionalIncome: Number(additionalIncome || 0),
        }),
      })
      setMsg('Budžet kreiran.')
      await load()
      setSelected(b)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  async function applySuggested() {
    if (!selected) return
    setError('')
    try {
      await api(`/api/budgets/${selected.id}/suggested`, { method: 'POST' })
      setMsg('Primijenjena 50/30/20 raspodjela.')
      const list = await api<BudgetCategory[]>(`/api/budget-categories/budget/${selected.id}`)
      setAllocations(list)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  async function addAllocation(e: FormEvent) {
    e.preventDefault()
    if (!selected) return

    setError('')
    setMsg('')

    const selectedCategoryId = Number(categoryId)
    const newPercentage = Number(percentage)

    if (!Number.isFinite(newPercentage) || newPercentage < 0 || newPercentage > 100) {
      setError('Procenat mora biti broj između 0 i 100.')
      return
    }

    // Ako kategorija već postoji, njena stara vrijednost se zamjenjuje novom.
    // Tako se ista kategorija ne računa dvaput pri provjeri ukupnog procenta.
    const totalWithoutSelectedCategory = allocations
        .filter((allocation) => allocation.category?.id !== selectedCategoryId)
        .reduce((sum, allocation) => sum + Number(allocation.percentage || 0), 0)

    const newTotalPercentage = totalWithoutSelectedCategory + newPercentage

    if (newTotalPercentage > 100.000001) {
      setError(
          `Ukupan procenat raspodjele ne može biti veći od 100%. ` +
          `Nakon ove izmjene iznosio bi ${newTotalPercentage.toFixed(2)}%. ` +
          'Smanjite neku od ostalih kategorija.',
      )
      return
    }

    try {
      await api(`/api/budgets/${selected.id}/categories`, {
        method: 'PUT',
        body: JSON.stringify({
          categoryId: selectedCategoryId,
          percentage: newPercentage,
        }),
      })
      setMsg('Raspodjela sačuvana.')
      const list = await api<BudgetCategory[]>(`/api/budget-categories/budget/${selected.id}`)
      setAllocations(list)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  return (
      <div className="page">
        <div className="page-header">
          <div>
            <h1>Budžet</h1>
            <p>Mjesečni budžet i raspodjela po kategorijama</p>
          </div>
        </div>

        {msg && <div className="alert ok">{msg}</div>}
        {error && <div className="alert error">{error}</div>}

        <div className="grid two">
          <div className="panel">
            <h2>Novi budžet</h2>
            <form className="form" onSubmit={createBudget} style={{ marginTop: '0.8rem' }}>
              <label>
                Mjesec
                <input value={month} onChange={(e) => setMonth(e.target.value)} placeholder="YYYY-MM" required />
              </label>
              <div className="form row">
                <label>
                  Ukupan iznos
                  <input type="number" step="0.01" value={totalAmount} onChange={(e) => setTotalAmount(e.target.value)} required />
                </label>
                <label>
                  Dodatni prihod
                  <input type="number" step="0.01" value={additionalIncome} onChange={(e) => setAdditionalIncome(e.target.value)} required />
                </label>
              </div>
              <button className="btn" type="submit">Sačuvaj budžet</button>
            </form>

            <h2 style={{ marginTop: '1.4rem' }}>Moji budžeti</h2>
            <div className="stack" style={{ marginTop: '0.6rem' }}>
              {budgets.length === 0 && <p className="muted">Još nema budžeta — kreiraj prvi iznad.</p>}
              {budgets.map((b) => (
                  <button
                      key={b.id}
                      type="button"
                      className={`btn ${selected?.id === b.id ? '' : 'ghost'}`}
                      onClick={() => setSelected(b)}
                  >
                    {b.month} — {Number(b.totalAmount).toFixed(2)} €
                  </button>
              ))}
            </div>
          </div>

          <div className="panel">
            <div className="inline" style={{ justifyContent: 'space-between' }}>
              <h2>Raspodjela {selected ? `(${selected.month})` : ''}</h2>
              <button className="btn secondary" type="button" disabled={!selected} onClick={applySuggested}>
                50/30/20
              </button>
            </div>

            <form className="form" onSubmit={addAllocation} style={{ marginTop: '0.8rem' }}>
              <label>
                Kategorija
                <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
                  <option value="">Odaberi…</option>
                  {categories.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </label>
              <label>
                Procenat
                <input type="number" step="0.01" value={percentage} onChange={(e) => setPercentage(e.target.value)} required />
              </label>
              <button className="btn" type="submit" disabled={!selected}>Dodaj / ažuriraj</button>
            </form>

            <table className="table" style={{ marginTop: '1rem' }}>
              <thead>
              <tr>
                <th>Kategorija</th>
                <th>%</th>
                <th>Iznos</th>
              </tr>
              </thead>
              <tbody>
              {allocations.map((a) => (
                  <tr key={a.id}>
                    <td>{a.category?.name}</td>
                    <td>{Number(a.percentage).toFixed(2)}</td>
                    <td>{Number(a.allocatedAmount).toFixed(2)} €</td>
                  </tr>
              ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
  )
}