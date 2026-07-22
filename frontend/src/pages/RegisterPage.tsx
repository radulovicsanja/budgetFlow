import { FormEvent, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../api/client'

export default function RegisterPage() {
  const { register, user } = useAuth()
  const nav = useNavigate()
  const [username, setUsername] = useState('')
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
      await register(username, email, password)
      nav('/')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška pri registraciji')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <p className="muted">BudgetFlow</p>
        <h1 className="display">Registracija</h1>
        <p>Novi nalog automatski dobija predefinisane kategorije.</p>
        <form className="form" onSubmit={onSubmit} style={{ marginTop: '1.2rem' }}>
          <label>
            Korisničko ime
            <input value={username} onChange={(e) => setUsername(e.target.value)} required minLength={3} />
          </label>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Lozinka
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={6} />
          </label>
          {error && <div className="alert error">{error}</div>}
          <button className="btn" disabled={loading} type="submit">
            {loading ? 'Kreiranje…' : 'Kreiraj nalog'}
          </button>
        </form>
        <div className="auth-links">
          <Link to="/login">Već imaš nalog?</Link>
        </div>
      </div>
    </div>
  )
}
