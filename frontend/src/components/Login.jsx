import React, { useState } from 'react'
import { useLocation, Link } from 'react-router-dom'

export default function Login() {
  const location = useLocation()
  const params = new URLSearchParams(location.search)
  const hasError = params.get('error') === 'true'
  const hasLogout = params.get('logout') === 'true'
  const hasRegistered = params.get('registered') === 'true'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #0f172a 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
      padding: '1rem',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Background decorative orbs */}
      <div style={{
        position: 'absolute', top: '15%', left: '10%',
        width: 400, height: 400, borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(99,102,241,0.15) 0%, transparent 70%)',
        pointerEvents: 'none',
      }} />
      <div style={{
        position: 'absolute', bottom: '10%', right: '5%',
        width: 350, height: 350, borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(14,165,233,0.12) 0%, transparent 70%)',
        pointerEvents: 'none',
      }} />

      <div style={{
        width: '100%', maxWidth: 440,
        background: 'rgba(255,255,255,0.04)',
        backdropFilter: 'blur(24px)',
        border: '1px solid rgba(255,255,255,0.1)',
        borderRadius: 24,
        padding: '2.5rem',
        boxShadow: '0 32px 80px rgba(0,0,0,0.5)',
        position: 'relative',
        zIndex: 1,
      }}>
        {/* Logo + Brand */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            width: 56, height: 56, borderRadius: 16,
            background: 'linear-gradient(135deg, #6366f1, #4f46e5)',
            boxShadow: '0 8px 24px rgba(99,102,241,0.4)',
            marginBottom: '1rem',
          }}>
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
          </div>
          <h1 style={{ margin: 0, fontSize: '1.6rem', fontWeight: 800, color: '#ffffff', letterSpacing: '-0.025em' }}>
            GATE MockAI
          </h1>
          <p style={{ margin: '0.4rem 0 0', color: '#94a3b8', fontSize: '0.9rem' }}>
            Sign in to your portal
          </p>
        </div>

        {/* Alerts */}
        {hasError && (
          <div style={{
            background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)',
            borderRadius: 10, padding: '0.75rem 1rem', marginBottom: '1.25rem',
            color: '#fca5a5', fontSize: '0.875rem', fontWeight: 600, textAlign: 'center',
          }}>
            ⚠️ Invalid email or password. Please try again.
          </div>
        )}
        {hasLogout && (
          <div style={{
            background: 'rgba(34,197,94,0.12)', border: '1px solid rgba(34,197,94,0.3)',
            borderRadius: 10, padding: '0.75rem 1rem', marginBottom: '1.25rem',
            color: '#86efac', fontSize: '0.875rem', fontWeight: 600, textAlign: 'center',
          }}>
            ✓ You have been signed out successfully.
          </div>
        )}
        {hasRegistered && (
          <div style={{
            background: 'rgba(99,102,241,0.12)', border: '1px solid rgba(99,102,241,0.3)',
            borderRadius: 10, padding: '0.75rem 1rem', marginBottom: '1.25rem',
            color: '#a5b4fc', fontSize: '0.875rem', fontWeight: 600, textAlign: 'center',
          }}>
            ✓ Account created! Please sign in below.
          </div>
        )}

        {/*
          Login Form — native browser POST to /login.
          Spring Security's formLogin() processor intercepts POST /login.
          We include the CSRF token from the XSRF-TOKEN cookie as a hidden field.
        */}
        <form
          method="POST"
          action="/login"
          onSubmit={() => setSubmitting(true)}
        >


          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{
              display: 'block', fontSize: '0.7rem', fontWeight: 700,
              textTransform: 'uppercase', letterSpacing: '0.08em',
              color: '#94a3b8', marginBottom: '0.5rem',
            }}>Email Address</label>
            <input
              type="email"
              name="username"
              required
              autoComplete="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="you@example.com"
              style={{
                width: '100%', padding: '0.8rem 1rem', boxSizing: 'border-box',
                background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
                borderRadius: 10, color: '#f1f5f9', fontSize: '0.9rem',
                outline: 'none', transition: 'border-color 0.2s',
              }}
              onFocus={e => e.target.style.borderColor = 'rgba(99,102,241,0.6)'}
              onBlur={e => e.target.style.borderColor = 'rgba(255,255,255,0.12)'}
            />
          </div>

          <div style={{ marginBottom: '1.75rem' }}>
            <label style={{
              display: 'block', fontSize: '0.7rem', fontWeight: 700,
              textTransform: 'uppercase', letterSpacing: '0.08em',
              color: '#94a3b8', marginBottom: '0.5rem',
            }}>Password</label>
            <input
              type="password"
              name="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              style={{
                width: '100%', padding: '0.8rem 1rem', boxSizing: 'border-box',
                background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
                borderRadius: 10, color: '#f1f5f9', fontSize: '0.9rem',
                outline: 'none', transition: 'border-color 0.2s',
              }}
              onFocus={e => e.target.style.borderColor = 'rgba(99,102,241,0.6)'}
              onBlur={e => e.target.style.borderColor = 'rgba(255,255,255,0.12)'}
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            style={{
              width: '100%', padding: '0.875rem',
              background: submitting
                ? 'rgba(99,102,241,0.5)'
                : 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)',
              border: 'none', borderRadius: 10, color: '#ffffff',
              fontSize: '0.9rem', fontWeight: 700, textTransform: 'uppercase',
              letterSpacing: '0.06em', cursor: submitting ? 'not-allowed' : 'pointer',
              boxShadow: submitting ? 'none' : '0 4px 20px rgba(99,102,241,0.35)',
              transition: 'all 0.2s',
            }}
          >
            {submitting ? 'Signing in…' : 'Sign In →'}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: '1.5rem', color: '#64748b', fontSize: '0.875rem' }}>
          Don't have an account?{' '}
          <Link to="/register" style={{ color: '#818cf8', fontWeight: 700, textDecoration: 'none' }}>
            Create one
          </Link>
        </p>
      </div>

      {/* Subtle version badge */}
      <div style={{
        position: 'fixed', bottom: '1.25rem', left: '50%', transform: 'translateX(-50%)',
        color: '#334155', fontSize: '0.7rem', letterSpacing: '0.05em',
      }}>
        GATE MockAI · Academic Assessment Platform
      </div>
    </div>
  )
}
