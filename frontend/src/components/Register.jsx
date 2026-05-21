import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function Register() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', fullName: '', password: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [globalError, setGlobalError] = useState(null)

  const handleChange = (field) => (e) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }))
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: null }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setErrors({})
    setGlobalError(null)

    try {
      const res = await fetch('/api/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      })
      const data = await res.json()

      if (data.status === 'success') {
        navigate('/login?registered=true')
      } else {
        if (data.fieldErrors) setErrors(data.fieldErrors)
        else setGlobalError(data.message || 'Registration failed.')
      }
    } catch (err) {
      setGlobalError('Network error. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  const inputStyle = (field) => ({
    width: '100%', padding: '0.8rem 1rem', boxSizing: 'border-box',
    background: 'rgba(255,255,255,0.06)',
    border: `1px solid ${errors[field] ? 'rgba(239,68,68,0.5)' : 'rgba(255,255,255,0.12)'}`,
    borderRadius: 10, color: '#f1f5f9', fontSize: '0.9rem',
    outline: 'none', transition: 'border-color 0.2s',
  })

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #0f172a 100%)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
      padding: '1rem', position: 'relative', overflow: 'hidden',
    }}>
      <div style={{ position: 'absolute', top: '10%', right: '10%', width: 400, height: 400, borderRadius: '50%', background: 'radial-gradient(circle, rgba(99,102,241,0.15) 0%, transparent 70%)', pointerEvents: 'none' }} />
      <div style={{ position: 'absolute', bottom: '5%', left: '5%', width: 350, height: 350, borderRadius: '50%', background: 'radial-gradient(circle, rgba(14,165,233,0.1) 0%, transparent 70%)', pointerEvents: 'none' }} />

      <div style={{
        width: '100%', maxWidth: 480,
        background: 'rgba(255,255,255,0.04)', backdropFilter: 'blur(24px)',
        border: '1px solid rgba(255,255,255,0.1)', borderRadius: 24,
        padding: '2.5rem', boxShadow: '0 32px 80px rgba(0,0,0,0.5)',
        position: 'relative', zIndex: 1,
      }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            width: 56, height: 56, borderRadius: 16,
            background: 'linear-gradient(135deg, #6366f1, #4f46e5)',
            boxShadow: '0 8px 24px rgba(99,102,241,0.4)', marginBottom: '1rem',
          }}>
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
              <line x1="19" y1="8" x2="19" y2="14"/><line x1="22" y1="11" x2="16" y2="11"/>
            </svg>
          </div>
          <h1 style={{ margin: 0, fontSize: '1.6rem', fontWeight: 800, color: '#ffffff', letterSpacing: '-0.025em' }}>
            Create Account
          </h1>
          <p style={{ margin: '0.4rem 0 0', color: '#94a3b8', fontSize: '0.9rem' }}>
            Join the GATE MockAI platform
          </p>
        </div>

        {globalError && (
          <div style={{
            background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)',
            borderRadius: 10, padding: '0.75rem 1rem', marginBottom: '1.25rem',
            color: '#fca5a5', fontSize: '0.875rem', fontWeight: 600, textAlign: 'center',
          }}>
            ⚠️ {globalError}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* Full Name */}
          <div style={{ marginBottom: '1.1rem' }}>
            <label style={{ display: 'block', fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: '#94a3b8', marginBottom: '0.5rem' }}>
              Full Name
            </label>
            <input
              type="text" required placeholder="John Doe"
              value={form.fullName} onChange={handleChange('fullName')}
              style={inputStyle('fullName')}
              onFocus={e => e.target.style.borderColor = 'rgba(99,102,241,0.6)'}
              onBlur={e => e.target.style.borderColor = errors.fullName ? 'rgba(239,68,68,0.5)' : 'rgba(255,255,255,0.12)'}
            />
            {errors.fullName && <p style={{ color: '#f87171', fontSize: '0.78rem', marginTop: '0.3rem' }}>{errors.fullName}</p>}
          </div>

          {/* Email */}
          <div style={{ marginBottom: '1.1rem' }}>
            <label style={{ display: 'block', fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: '#94a3b8', marginBottom: '0.5rem' }}>
              Email Address
            </label>
            <input
              type="email" required placeholder="you@example.com"
              value={form.email} onChange={handleChange('email')}
              style={inputStyle('email')}
              onFocus={e => e.target.style.borderColor = 'rgba(99,102,241,0.6)'}
              onBlur={e => e.target.style.borderColor = errors.email ? 'rgba(239,68,68,0.5)' : 'rgba(255,255,255,0.12)'}
            />
            {errors.email && <p style={{ color: '#f87171', fontSize: '0.78rem', marginTop: '0.3rem' }}>{errors.email}</p>}
          </div>

          {/* Password */}
          <div style={{ marginBottom: '1.1rem' }}>
            <label style={{ display: 'block', fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: '#94a3b8', marginBottom: '0.5rem' }}>
              Password
            </label>
            <input
              type="password" required placeholder="At least 6 characters"
              value={form.password} onChange={handleChange('password')}
              style={inputStyle('password')}
              onFocus={e => e.target.style.borderColor = 'rgba(99,102,241,0.6)'}
              onBlur={e => e.target.style.borderColor = errors.password ? 'rgba(239,68,68,0.5)' : 'rgba(255,255,255,0.12)'}
            />
            {errors.password && <p style={{ color: '#f87171', fontSize: '0.78rem', marginTop: '0.3rem' }}>{errors.password}</p>}
          </div>

          {/* Confirm Password */}
          <div style={{ marginBottom: '1.75rem' }}>
            <label style={{ display: 'block', fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: '#94a3b8', marginBottom: '0.5rem' }}>
              Confirm Password
            </label>
            <input
              type="password" required placeholder="Repeat your password"
              value={form.confirmPassword} onChange={handleChange('confirmPassword')}
              style={inputStyle('confirmPassword')}
              onFocus={e => e.target.style.borderColor = 'rgba(99,102,241,0.6)'}
              onBlur={e => e.target.style.borderColor = errors.confirmPassword ? 'rgba(239,68,68,0.5)' : 'rgba(255,255,255,0.12)'}
            />
            {errors.confirmPassword && <p style={{ color: '#f87171', fontSize: '0.78rem', marginTop: '0.3rem' }}>{errors.confirmPassword}</p>}
          </div>

          <button
            type="submit" disabled={submitting}
            style={{
              width: '100%', padding: '0.875rem',
              background: submitting ? 'rgba(99,102,241,0.5)' : 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)',
              border: 'none', borderRadius: 10, color: '#ffffff',
              fontSize: '0.9rem', fontWeight: 700, textTransform: 'uppercase',
              letterSpacing: '0.06em', cursor: submitting ? 'not-allowed' : 'pointer',
              boxShadow: submitting ? 'none' : '0 4px 20px rgba(99,102,241,0.35)',
              transition: 'all 0.2s',
            }}
          >
            {submitting ? 'Creating Account…' : 'Create Account →'}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: '1.5rem', color: '#64748b', fontSize: '0.875rem' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: '#818cf8', fontWeight: 700, textDecoration: 'none' }}>
            Sign in
          </Link>
        </p>
      </div>

      <div style={{ position: 'fixed', bottom: '1.25rem', left: '50%', transform: 'translateX(-50%)', color: '#334155', fontSize: '0.7rem', letterSpacing: '0.05em' }}>
        GATE MockAI · Academic Assessment Platform
      </div>
    </div>
  )
}
