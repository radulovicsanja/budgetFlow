import { FormEvent, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { api, ApiError } from '../api/client'

export default function AccountPage() {
  const { user, refreshUser } = useAuth()
  const [username, setUsername] = useState(user?.username || '')
  const [email, setEmail] = useState(user?.email || '')
  const [oldPassword, setOldPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')

  async function updateProfile(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await api('/api/users/me', {
        method: 'PUT',
        body: JSON.stringify({ username, email }),
      })
      await refreshUser()
      setMsg('Profil ažuriran.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  async function changePassword(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      const res = await api<{ message: string }>('/api/users/change-password', {
        method: 'POST',
        body: JSON.stringify({ oldPassword, newPassword }),
      })
      setMsg(res.message)
      setOldPassword('')
      setNewPassword('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Nalog</h1>
          <p>Profil i promjena lozinke</p>
        </div>
      </div>

      {msg && <div className="alert ok">{msg}</div>}
      {error && <div className="alert error">{error}</div>}

      <div className="grid two">
        <div className="panel">
          <h2>Profil</h2>
          <form className="form" onSubmit={updateProfile} style={{ marginTop: '0.8rem' }}>
            <label>
              Username
              <input value={username} onChange={(e) => setUsername(e.target.value)} required />
            </label>
            <label>
              Email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </label>
            <button className="btn" type="submit">Sačuvaj</button>
          </form>
        </div>

        <div className="panel">
          <h2>Promjena lozinke</h2>
          <form className="form" onSubmit={changePassword} style={{ marginTop: '0.8rem' }}>
            <label>
              Stara lozinka
              <input type="password" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} required />
            </label>
            <label>
              Nova lozinka
              <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={6} />
            </label>
            <button className="btn secondary" type="submit">Promijeni</button>
          </form>
        </div>
      </div>
    </div>
  )
}
