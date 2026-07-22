import { FormEvent, useEffect, useMemo, useState } from 'react'
import { api, apiOptional, ApiError, currentMonth, getToken } from '../api/client'
import { LineChart, MonthBars, PieChart } from '../components/charts'
import { transactionTypeLabel } from '../api/labels'
import type { Budget, CsvImportResult, MonthCompare, Transaction, UserReport } from '../api/types'

function formatMonthLabel(month: string) {
  const [y, m] = month.split('-').map(Number)
  if (!y || !m) return month
  const label = new Date(y, m - 1, 1).toLocaleDateString('bs-BA', {
    month: 'long',
    year: 'numeric',
  })
  return label.charAt(0).toUpperCase() + label.slice(1)
}

function money(n: number) {
  return `${Number(n || 0).toFixed(2)} €`
}

function shiftMonth(yyyyMm: string, delta: number): string {
  const [y, m] = yyyyMm.split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

/** Prihod = totalAmount + additionalIncome iz budžeta. */
function incomeFromBudget(budgets: Budget[], monthKey: string): number {
  const b = budgets.find((x) => x.month === monthKey)
  if (!b) return 0
  return Number(b.totalAmount ?? 0) + Number(b.additionalIncome ?? 0)
}

function expensesFromTx(txs: Transaction[], monthKey: string): number {
  let expenses = 0
  for (const t of txs) {
    if (!t.date?.startsWith(monthKey) || t.type !== 'EXPENSE') continue
    expenses += Number(t.amount ?? 0)
  }
  return expenses
}

function buildTrend(txs: Transaction[], budgets: Budget[], endMonth: string, count = 6) {
  const points: { label: string; income: number; expenses: number }[] = []
  for (let i = count - 1; i >= 0; i--) {
    const m = shiftMonth(endMonth, -i)
    points.push({
      label: m,
      income: incomeFromBudget(budgets, m),
      expenses: expensesFromTx(txs, m),
    })
  }
  return points
}

export default function ReportsPage() {
  const [month, setMonth] = useState(currentMonth())
  const [summary, setSummary] = useState<UserReport | null>(null)
  const [stats, setStats] = useState<UserReport[]>([])
  const [allTransactions, setAllTransactions] = useState<Transaction[]>([])
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [showPreview, setShowPreview] = useState(false)
  const [importResult, setImportResult] = useState<CsvImportResult | null>(null)
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')
  const [loadingReport, setLoadingReport] = useState(false)
  const [compare, setCompare] = useState<MonthCompare | null>(null)

  async function load() {
    setError('')
    const [s, c, tx, buds, cmp] = await Promise.all([
      api<UserReport>(`/api/reports/summary?month=${month}`),
      api<UserReport[]>(`/api/reports/category?month=${month}`),
      api<Transaction[]>('/api/transactions'),
      api<Budget[]>('/api/budgets/me'),
      apiOptional<MonthCompare | null>(`/api/reports/compare?month=${month}`, null),
    ])
    setSummary(s)
    setStats(c)
    setAllTransactions(tx)
    setBudgets(buds)
    setCompare(cmp)
  }

  useEffect(() => {
    setShowPreview(false)
    load().catch((e) => setError(e.message))
  }, [month])

  const transactions = useMemo(
    () => allTransactions.filter((t) => t.date?.startsWith(month)),
    [allTransactions, month],
  )

  const trend = useMemo(
    () => buildTrend(allTransactions, budgets, month, 6),
    [allTransactions, budgets, month],
  )

  const monthActivity = useMemo(
    () => ({
      income: incomeFromBudget(budgets, month),
      expenses: expensesFromTx(allTransactions, month),
    }),
    [budgets, allTransactions, month],
  )

  const pieData = useMemo(() => {
    const map = new Map<string, number>()
    for (const t of transactions) {
      if (t.type !== 'EXPENSE') continue
      const name = t.category?.name || 'Ostalo'
      map.set(name, (map.get(name) || 0) + Number(t.amount ?? 0))
    }
    return [...map.entries()].map(([name, value]) => ({ name, value }))
  }, [transactions])

  const maxCategory = useMemo(
    () => Math.max(1, ...stats.map((s) => Number(s.amount ?? 0))),
    [stats],
  )

  const income = Number(summary?.totalIncome ?? 0)
  const expenses = Number(summary?.totalExpenses ?? 0)
  const savings = Number(summary?.totalSavings ?? 0)
  const savingsPositive = savings >= 0

  async function downloadBlob(path: string, filename: string) {
    const res = await fetch(path, {
      headers: {
        Authorization: `Bearer ${getToken()}`,
        Accept: 'text/csv,*/*',
      },
    })
    if (!res.ok) {
      const body = await res.json().catch(() => null)
      throw new ApiError(res.status, body, path)
    }
    const buffer = await res.arrayBuffer()
    if (!buffer || buffer.byteLength === 0) {
      throw new ApiError(500, { message: 'Preuzeti fajl je prazan.' }, path)
    }
    const blob = new Blob([buffer], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  }

  async function generate() {
    setError('')
    setMsg('')
    const m = month.trim()
    if (!/^\d{4}-\d{2}$/.test(m)) {
      setError('Mjesec mora biti u formatu YYYY-MM.')
      return
    }
    setLoadingReport(true)
    try {
      await api(`/api/reports/monthly?month=${encodeURIComponent(m)}`)
      await load()
      setShowPreview(true)
      setMsg('Mjesečni izvještaj je spreman.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška pri generisanju izvještaja')
    } finally {
      setLoadingReport(false)
    }
  }

  async function downloadReport() {
    setError('')
    try {
      await downloadBlob(
        `/api/reports/monthly/download?month=${encodeURIComponent(month.trim())}`,
        `mjesecni-izvjestaj-${month}.csv`,
      )
      setMsg('CSV izvještaj je preuzet.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška pri preuzimanju')
    }
  }

  async function exportCsv() {
    setError('')
    try {
      await downloadBlob(
        `/api/reports/export?month=${encodeURIComponent(month.trim())}`,
        `transakcije-${month}.csv`,
      )
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška pri exportu')
    }
  }

  async function downloadTemplate() {
    try {
      await downloadBlob('/api/reports/import-template', 'budgetflow-import-template.csv')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška pri preuzimanju templatea')
    }
  }

  async function importCsv(e: FormEvent) {
    e.preventDefault()
    if (!file) return
    setError('')
    const fd = new FormData()
    fd.append('file', file)
    try {
      const res = await fetch('/api/reports/import?confirmFromUnallocated=true', {
        method: 'POST',
        headers: { Authorization: `Bearer ${getToken()}` },
        body: fd,
      })
      const body = await res.json()
      if (!res.ok) throw new ApiError(res.status, body)
      setImportResult(body)
      setMsg(`Uvezeno: ${body.importedCount}, greške: ${body.failedCount}`)
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška pri importu')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Izvještaji</h1>
          <p>Pregled mjeseca, statistika i CSV</p>
        </div>
        <label>
          Mjesec
          <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} />
        </label>
      </div>

      {msg && <div className="alert ok">{msg}</div>}
      {error && <div className="alert error">{error}</div>}

      <div className="inline" style={{ marginBottom: '1rem' }}>
        <button className="btn" type="button" onClick={generate} disabled={loadingReport}>
          {loadingReport ? 'Generisanje…' : 'Generiši mjesečni izvještaj'}
        </button>
        <button className="btn secondary" type="button" onClick={downloadReport}>
          Preuzmi CSV
        </button>
        <button className="btn ghost" type="button" onClick={exportCsv}>
          Export transakcija
        </button>
        <button className="btn ghost" type="button" onClick={downloadTemplate}>
          CSV template
        </button>
      </div>

      <div className="grid two" style={{ marginBottom: '1rem' }}>
        <div className="panel">
          <h2>Izabrani mjesec: {formatMonthLabel(month)}</h2>
          <p className="muted" style={{ marginTop: '0.25rem', fontSize: '0.85rem' }}>
            Prihod = budžet (total) + dodatni prihod; trošak = transakcije.
          </p>
          <MonthBars
            key={`bars-${month}`}
            monthLabel={formatMonthLabel(month)}
            income={monthActivity.income}
            expenses={monthActivity.expenses}
          />
        </div>
        <div className="panel">
          <h2>Potrošnja po kategorijama</h2>
          <PieChart key={`pie-${month}`} data={pieData} />
        </div>
      </div>

      <div className="panel" style={{ marginBottom: '1rem' }}>
        <h2>Trend (6 mjeseci do {month})</h2>
        <p className="muted" style={{ marginTop: '0.25rem', fontSize: '0.85rem' }}>
          Prihod iz budžeta mjeseca; trošak iz transakcija. Bez budžeta prihod je 0.
        </p>
        <LineChart key={`trend-${month}`} data={trend} highlightLabel={month} />
      </div>

      {compare && (
        <div className="panel compare-panel" style={{ marginBottom: '1rem' }}>
          <h2>Poređenje: {compare.currentMonth} vs {compare.previousMonth}</h2>
          <div className="grid stats" style={{ marginTop: '0.8rem' }}>
            <div className="panel stat">
              <span>Prihodi</span>
              <strong>{money(compare.currentIncome)}</strong>
              <small className={Number(compare.incomeChangePercent) >= 0 ? 'tone-ok' : 'tone-danger'}>
                {Number(compare.incomeChangePercent) >= 0 ? '+' : ''}
                {Number(compare.incomeChangePercent).toFixed(1)}% vs prošli
              </small>
            </div>
            <div className="panel stat">
              <span>Rashodi</span>
              <strong>{money(compare.currentExpenses)}</strong>
              <small className={Number(compare.expensesChangePercent) <= 0 ? 'tone-ok' : 'tone-danger'}>
                {Number(compare.expensesChangePercent) >= 0 ? '+' : ''}
                {Number(compare.expensesChangePercent).toFixed(1)}% vs prošli
              </small>
            </div>
            <div className="panel stat">
              <span>Ušteda</span>
              <strong>{money(compare.currentSavings)}</strong>
              <small className={Number(compare.savingsChangePercent) >= 0 ? 'tone-ok' : 'tone-danger'}>
                {Number(compare.savingsChangePercent) >= 0 ? '+' : ''}
                {Number(compare.savingsChangePercent).toFixed(1)}% vs prošli
              </small>
            </div>
          </div>
        </div>
      )}

      {showPreview && (
        <section className="report-preview">
          <div className="report-preview__hero">
            <div>
              <p className="report-preview__eyebrow">Mjesečni izvještaj</p>
              <h2 className="report-preview__title">{formatMonthLabel(month)}</h2>
              <p>Sažetak prihoda, rashoda i potrošnje po kategorijama.</p>
            </div>
            <div className={`report-preview__badge ${savingsPositive ? 'ok' : 'warn'}`}>
              {savingsPositive ? 'Pozitivan bilans' : 'Negativan bilans'}
            </div>
          </div>

          <div className="report-preview__stats">
            <article>
              <span>Ukupni prihodi</span>
              <strong className="tone-ok">{money(income)}</strong>
              <small className="muted">Budžet + dodatni prihodi</small>
            </article>
            <article>
              <span>Rashodi</span>
              <strong className="tone-danger">{money(expenses)}</strong>
              <small className="muted">Svi troškovi u mjesecu</small>
            </article>
            <article>
              <span>Ušteda</span>
              <strong className={savingsPositive ? 'tone-ok' : 'tone-danger'}>{money(savings)}</strong>
              <small className="muted">Prihodi − rashodi</small>
            </article>
          </div>

          <div className="report-preview__grid">
            <div className="report-preview__block">
              <h3>Potrošnja po kategorijama</h3>
              {stats.length === 0 ? (
                <p className="muted">Nema rashoda za ovaj mjesec.</p>
              ) : (
                <div className="report-bars">
                  {stats.map((s, i) => {
                    const amount = Number(s.amount ?? 0)
                    const pct = Math.round((amount / maxCategory) * 100)
                    return (
                      <div key={`${s.categoryName}-${i}`} className="report-bar">
                        <div className="report-bar__meta">
                          <span>{s.categoryName}</span>
                          <strong>{money(amount)}</strong>
                        </div>
                        <div className="report-bar__track">
                          <div className="report-bar__fill" style={{ width: `${pct}%` }} />
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>

            <div className="report-preview__block">
              <h3>Transakcije ({transactions.length})</h3>
              {transactions.length === 0 ? (
                <p className="muted">Nema transakcija za odabrani mjesec.</p>
              ) : (
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
                    {transactions.map((t) => (
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
                        <td>{money(t.amount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </section>
      )}

      {!showPreview && (
        <div className="panel" style={{ marginBottom: '1rem' }}>
          <p className="muted">
            Klikni <strong>Generiši mjesečni izvještaj</strong> da vidiš pregled za {formatMonthLabel(month)}.
          </p>
        </div>
      )}

      <div className="grid two">
        <div className="panel">
          <h2>Brzi pregled</h2>
          <div className="grid stats" style={{ marginTop: '0.8rem' }}>
            <div className="panel stat"><span>Prihodi</span><strong>{money(income)}</strong></div>
            <div className="panel stat"><span>Rashodi</span><strong>{money(expenses)}</strong></div>
            <div className="panel stat"><span>Ušteda</span><strong>{money(savings)}</strong></div>
          </div>
        </div>

        <div className="panel">
          <h2>CSV import</h2>
          <form className="form" onSubmit={importCsv} style={{ marginTop: '0.8rem' }}>
            <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files?.[0] || null)} />
            <button className="btn" type="submit" disabled={!file}>Uvezi</button>
          </form>
          {importResult && importResult.errors?.length > 0 && (
            <div className="stack" style={{ marginTop: '0.8rem' }}>
              {importResult.errors.map((err, i) => (
                <div key={i} className="alert warn">{err}</div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
