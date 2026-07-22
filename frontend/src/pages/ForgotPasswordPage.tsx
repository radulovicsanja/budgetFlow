import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api/client'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [token, setToken] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')

  async function requestToken(e: FormEvent) {
    e.preventDefault()
    setError('')
    setMsg('')
    try {
      const res = await api<{ message: string; resetToken?: string }>('/api/users/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email }),
      })
      setMsg(res.message)
      if (res.resetToken) {
        setResetToken(res.resetToken)
        setToken(res.resetToken)
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška.')
    }
  }

  async function resetPassword(e: FormEvent) {
    e.preventDefault()
    setError('')
    setMsg('')
    try {
      const res = await api<{ message: string }>('/api/users/reset-password', {
        method: 'POST',
        body: JSON.stringify({ token, newPassword }),
      })
      setMsg(res.message)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Greška.')
    }
  }

  return (
    <div className="auth-screen">
      <div className="auth-card stack">
        <p className="muted">BudgetFlow</p>
        <h1 className="display">Oporavak lozinke</h1>
        <form className="form" onSubmit={requestToken}>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <button className="btn" type="submit">Pošalji reset token</button>
        </form>
        {resetToken && (
          <div className="alert warn">
            Demo token: <code>{resetToken}</code>
          </div>
        )}
        <form className="form" onSubmit={resetPassword}>
          <label>
            Token
            <input value={token} onChange={(e) => setToken(e.target.value)} required />
          </label>
          <label>
            Nova lozinka
            <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={6} />
          </label>
          <button className="btn secondary" type="submit">Resetuj lozinku</button>
        </form>
        {msg && <div className="alert ok">{msg}</div>}
        {error && <div className="alert error">{error}</div>}
        <Link to="/login">Nazad na prijavu</Link>
      </div>
    </div>
  )
}
