import { FormEvent, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../api/client'

export default function LoginPage() {
  const { login, user } = useAuth()
  const nav = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(email, password)
      nav('/')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška na serveru')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <p className="muted">BudgetFlow</p>
        <h1 className="display">Prijava</h1>
        <p>Unesi podatke za pristup svom budžetu.</p>
        <form className="form" onSubmit={onSubmit} style={{ marginTop: '1.2rem' }}>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Lozinka
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {error && <div className="alert error">{error}</div>}
          <button className="btn" disabled={loading} type="submit">
            {loading ? 'Prijavljivanje…' : 'Prijavi se'}
          </button>
        </form>
        <div className="auth-links">
          <Link to="/register">Registracija</Link>
          <Link to="/forgot-password">Zaboravljena lozinka</Link>
        </div>
      </div>
    </div>
  )
}
