import { FormEvent, useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import { categoryTypeLabel } from '../api/labels'
import type { Category, CategoryType } from '../api/types'

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [types, setTypes] = useState<CategoryType[]>([])
  const [name, setName] = useState('')
  const [typeId, setTypeId] = useState('')
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  async function load() {
    const [cats, t] = await Promise.all([
      api<Category[]>('/api/categories'),
      api<CategoryType[]>('/api/category-types'),
    ])
    setCategories(cats)
    setTypes(t)
    if (!typeId && t[0]) setTypeId(String(t[0].id))
  }

  useEffect(() => {
    load().catch((e) => setError(e.message))
  }, [])

  async function seedDefaults() {
    setError('')
    try {
      const cats = await api<Category[]>('/api/categories/seed-defaults', { method: 'POST' })
      setCategories(cats)
      setMsg('Predefinisane kategorije su spremne.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  async function create(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await api('/api/categories', {
        method: 'POST',
        body: JSON.stringify({ name, typeId: Number(typeId) }),
      })
      setName('')
      setMsg('Kategorija dodata.')
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  async function remove(id: number) {
    setError('')
    try {
      await api(`/api/categories/${id}`, { method: 'DELETE' })
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Kategorije</h1>
          <p>Predefinisane i tvoje custom kategorije</p>
        </div>
        <button className="btn ghost" type="button" onClick={seedDefaults}>Dodaj osnovne kategorije</button>
      </div>

      {msg && <div className="alert ok">{msg}</div>}
      {error && <div className="alert error">{error}</div>}

      <div className="grid two">
        <div className="panel">
          <h2>Nova kategorija</h2>
          <form className="form" onSubmit={create} style={{ marginTop: '0.8rem' }}>
            <label>
              Naziv
              <input value={name} onChange={(e) => setName(e.target.value)} required />
            </label>
            <label>
              Tip
              <select value={typeId} onChange={(e) => setTypeId(e.target.value)} required>
                {types.map((t) => (
                  <option key={t.id} value={t.id}>{categoryTypeLabel(t)}</option>
                ))}
              </select>
            </label>
            <button className="btn" type="submit">Dodaj</button>
          </form>
        </div>

        <div className="panel">
          <table className="table">
            <thead>
              <tr>
                <th>Naziv</th>
                <th>Tip</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {categories.map((c) => (
                <tr key={c.id}>
                  <td>
                    {c.name} {c.isDefault && <span className="pill">sistemska</span>}
                  </td>
                  <td>{categoryTypeLabel(c.type)}</td>
                  <td>
                    {!c.isDefault && (
                      <button className="btn danger" type="button" onClick={() => remove(c.id)}>Obriši</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
