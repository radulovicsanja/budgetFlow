import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api/client'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')

  /** Šalje zahtjev za slanje linka za resetovanje lozinke. */
  async function requestPasswordReset(e: FormEvent) {
    e.preventDefault()

    setError('')
    setMsg('')

    try {
      const response = await api<{ message: string }>(
          '/api/users/forgot-password',
          {
            method: 'POST',
            body: JSON.stringify({ email }),
          }
      )

      setMsg(response.message)
    } catch (err) {
      setError(
          err instanceof ApiError
              ? err.message
              : 'Došlo je do greške prilikom slanja zahtjeva.'
      )
    }
  }

  return (
      <div className="auth-screen">
        <div className="auth-card stack">
          <p className="muted">BudgetFlow</p>

          <h1 className="display">Oporavak lozinke</h1>

          <p className="muted">
            Unesite email adresu povezanu sa vašim nalogom.
            Poslaćemo vam link za postavljanje nove lozinke.
          </p>

          <form
              className="form"
              onSubmit={requestPasswordReset}
          >
            <label>
              Email
              <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Unesite email adresu"
                  required
              />
            </label>

            <button className="btn" type="submit">
              Pošalji link za resetovanje
            </button>
          </form>

          {msg && (
              <div className="alert ok">
                {msg}
              </div>
          )}

          {error && (
              <div className="alert error">
                {error}
              </div>
          )}

          <Link to="/login">
            Nazad na prijavu
          </Link>
        </div>
      </div>
  )
}