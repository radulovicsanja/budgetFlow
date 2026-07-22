import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { api, ApiError } from '../api/client'
import type { Role, User } from '../api/types'

export default function AdminPage() {
  const { user } = useAuth()
  const [users, setUsers] = useState<User[]>([])
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')
  const isAdmin = user?.role === 'ADMIN'

  useEffect(() => {
    if (!isAdmin) return
    load().catch((e) => setError(e.message))
  }, [isAdmin])

  async function load() {
    setUsers(await api<User[]>('/api/admin/users'))
  }

  if (!isAdmin) {
    return <Navigate to="/" replace />
  }

  async function remove(id: number) {
    if (!confirm('Obrisati korisnika i sve njegove podatke?')) return
    setError('')
    try {
      await api(`/api/admin/users/${id}`, { method: 'DELETE' })
      setMsg('Korisnik obrisan.')
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  async function setRole(id: number, role: Role) {
    setError('')
    try {
      await api(`/api/admin/users/${id}/role`, {
        method: 'PUT',
        body: JSON.stringify({ role }),
      })
      setMsg(`Uloga postavljena na ${role}.`)
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Admin panel</h1>
          <p>Upravljanje korisničkim nalozima</p>
        </div>
      </div>

      {msg && <div className="alert ok">{msg}</div>}
      {error && <div className="alert error">{error}</div>}

      <div className="panel">
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Email</th>
              <th>Uloga</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.id}</td>
                <td>{u.username}</td>
                <td>{u.email}</td>
                <td>
                  <select
                    value={u.role || 'USER'}
                    onChange={(e) => setRole(u.id, e.target.value as Role)}
                    disabled={u.id === user?.id}
                  >
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
                <td>
                  <button
                    className="btn danger"
                    type="button"
                    disabled={u.id === user?.id}
                    onClick={() => remove(u.id)}
                  >
                    Obriši
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
