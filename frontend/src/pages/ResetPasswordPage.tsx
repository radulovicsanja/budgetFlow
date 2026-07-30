import { FormEvent, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'

export default function ResetPasswordPage() {
    const [searchParams] = useSearchParams()
    const token = searchParams.get('token') ?? ''

    const [newPassword, setNewPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [msg, setMsg] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    /**
     * Provjerava unesene podatke i šalje zahtjev
     * za postavljanje nove korisničke lozinke.
     */
    async function resetPassword(e: FormEvent) {
        e.preventDefault()

        setMsg('')
        setError('')

        if (!token) {
            setError(
                'Link za resetovanje lozinke nije validan ili ne sadrži token.'
            )
            return
        }

        if (newPassword !== confirmPassword) {
            setError('Lozinke se ne podudaraju.')
            return
        }

        setLoading(true)

        try {
            const response = await api<{ message: string }>(
                '/api/users/reset-password',
                {
                    method: 'POST',
                    body: JSON.stringify({
                        token,
                        newPassword,
                    }),
                }
            )

            setMsg(response.message)
            setNewPassword('')
            setConfirmPassword('')
        } catch (err) {
            setError(
                err instanceof ApiError
                    ? err.message
                    : 'Došlo je do greške prilikom resetovanja lozinke.'
            )
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="auth-screen">
            <div className="auth-card stack">
                <p className="muted">BudgetFlow</p>

                <h1 className="display">Nova lozinka</h1>

                <p className="muted">
                    Unesite novu lozinku koju ćete ubuduće koristiti za
                    prijavljivanje.
                </p>

                {!token && (
                    <div className="alert error">
                        Link za resetovanje lozinke nije validan ili ne sadrži token.
                    </div>
                )}

                {token && !msg && (
                    <form className="form" onSubmit={resetPassword}>
                        <label>
                            Nova lozinka
                            <input
                                type="password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                placeholder="Unesite novu lozinku"
                                minLength={8}
                                required
                            />
                        </label>

                        <label>
                            Potvrdite novu lozinku
                            <input
                                type="password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                placeholder="Ponovite novu lozinku"
                                minLength={8}
                                required
                            />
                        </label>

                        <button
                            className="btn"
                            type="submit"
                            disabled={loading}
                        >
                            {loading
                                ? 'Resetovanje...'
                                : 'Postavi novu lozinku'}
                        </button>
                    </form>
                )}

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