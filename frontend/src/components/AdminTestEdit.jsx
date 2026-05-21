import React, { useEffect, useState, useCallback, useRef } from 'react'
import { useParams, Link } from 'react-router-dom'

// ─── Debounce helper ─────────────────────────────────────────────────────────
function useDebounce(fn, delay) {
  const timer = useRef(null)
  return useCallback((...args) => {
    clearTimeout(timer.current)
    timer.current = setTimeout(() => fn(...args), delay)
  }, [fn, delay])
}

// ─── Question type badge ─────────────────────────────────────────────────────
function TypeBadge({ type }) {
  const cfg = {
    MCQ: { bg: 'rgba(99,102,241,0.1)', color: '#4f46e5' },
    MSQ: { bg: 'rgba(5,150,105,0.1)', color: '#059669' },
    NAT: { bg: 'rgba(217,119,6,0.1)', color: '#d97706' },
  }[type] || { bg: '#f1f5f9', color: '#475569' }
  return (
    <span style={{ background: cfg.bg, color: cfg.color, fontSize: '0.7rem', fontWeight: 700, padding: '0.2rem 0.65rem', borderRadius: 6, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
      {type}
    </span>
  )
}

// ─── Save status indicator ───────────────────────────────────────────────────
function SaveStatus({ saving, saved }) {
  if (saving) return <span style={{ fontSize: '0.72rem', color: '#f59e0b', fontWeight: 600 }}>💾 Saving…</span>
  if (saved) return <span style={{ fontSize: '0.72rem', color: '#059669', fontWeight: 600 }}>✓ Saved</span>
  return null
}

// ─── Image Upload Zone ───────────────────────────────────────────────────────
function ImageUploadZone({ testId, entityType, entityId, currentPath, onUploaded }) {
  const inputRef = useRef(null)
  const [uploading, setUploading] = useState(false)

  const handleFile = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    const fd = new FormData()
    fd.append('file', file)
    try {
      const res = await fetch('/api/admin/upload/image', { method: 'POST', body: fd })
      const data = await res.json()
      if (!data.url) throw new Error(data.error || 'Upload failed')
      const endpoint = entityType === 'q'
        ? `/api/admin/tests/${testId}/questions/${entityId}`
        : `/api/admin/tests/${testId}/options/${entityId}`
      await fetch(endpoint, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ imagePath: data.url }) })
      onUploaded(data.url)
    } catch (err) {
      alert('Image upload failed: ' + err.message)
    } finally {
      setUploading(false)
    }
  }

  return (
    <div
      onClick={() => inputRef.current?.click()}
      style={{
        border: '1.5px dashed #cbd5e1', borderRadius: 10, minHeight: 72, cursor: 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: '#f8fafc', transition: 'border-color 0.2s', position: 'relative', overflow: 'hidden',
      }}
      onMouseEnter={e => e.currentTarget.style.borderColor = '#6366f1'}
      onMouseLeave={e => e.currentTarget.style.borderColor = '#cbd5e1'}
    >
      {currentPath ? (
        <img src={currentPath} alt="" style={{ maxHeight: 120, maxWidth: '100%', objectFit: 'contain', borderRadius: 8 }} />
      ) : (
        <div style={{ textAlign: 'center', padding: '0.75rem', color: '#94a3b8', fontSize: '0.8rem' }}>
          {uploading ? '⏳ Uploading…' : '📷 Click to upload image'}
        </div>
      )}
      <input ref={inputRef} type="file" accept="image/*" onChange={handleFile} style={{ display: 'none' }} />
    </div>
  )
}

// ─── Option Row ───────────────────────────────────────────────────────────────
function OptionRow({ opt, testId, questionId, onUpdate }) {
  const [text, setText] = useState(opt.optionText ?? '')
  const [correct, setCorrect] = useState(opt.isCorrect ?? false)
  const [imgPath, setImgPath] = useState(opt.imagePath ?? '')
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  const save = async (overrides = {}) => {
    setSaving(true)
    const payload = { optionText: text, isCorrect: correct, imagePath: imgPath, ...overrides }
    await fetch(`/api/admin/tests/${testId}/options/${opt.id}`, {
      method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload),
    })
    setSaving(false); setSaved(true)
    setTimeout(() => setSaved(false), 2000)
    onUpdate?.(opt.id, payload)
  }

  const debouncedSave = useDebounce(save, 900)

  return (
    <div style={{
      background: correct ? 'rgba(5,150,105,0.04)' : '#f8fafc',
      border: `1px solid ${correct ? 'rgba(5,150,105,0.2)' : '#e2e8f0'}`,
      borderRadius: 12, padding: '0.9rem 1rem',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
        {/* Correct toggle */}
        <button
          onClick={() => { const nc = !correct; setCorrect(nc); save({ isCorrect: nc }) }}
          title={correct ? 'Mark incorrect' : 'Mark correct'}
          style={{
            width: 28, height: 28, borderRadius: '50%', flexShrink: 0, cursor: 'pointer', border: 'none',
            background: correct ? '#059669' : '#e2e8f0', color: correct ? '#fff' : '#475569',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.8rem', fontWeight: 800,
            transition: 'all 0.15s',
          }}
        >
          {opt.optionLabel}
        </button>
        {/* Text input */}
        <input
          type="text" value={text}
          onChange={e => { setText(e.target.value); debouncedSave({ optionText: e.target.value }) }}
          style={{ flex: 1, padding: '0.5rem 0.75rem', border: '1px solid #e2e8f0', borderRadius: 8, fontSize: '0.88rem', color: '#0f172a', background: '#fff', outline: 'none', boxSizing: 'border-box' }}
          onFocus={e => e.target.style.borderColor = '#6366f1'}
          onBlur={e => { e.target.style.borderColor = '#e2e8f0'; save({ optionText: text }) }}
        />
        <SaveStatus saving={saving} saved={saved} />
        {correct && <span style={{ fontSize: '0.7rem', color: '#059669', fontWeight: 700, textTransform: 'uppercase' }}>✓ Correct</span>}
      </div>
      {/* Option image */}
      <ImageUploadZone testId={testId} entityType="opt" entityId={opt.id} currentPath={imgPath} onUploaded={url => setImgPath(url)} />
    </div>
  )
}

// ─── Question Card ────────────────────────────────────────────────────────────
function QuestionCard({ q, testId, onDelete }) {
  const [text, setText] = useState(q.questionText ?? '')
  const [marks, setMarks] = useState(q.marks ?? 1)
  const [negMarks, setNegMarks] = useState(q.negativeMarks ?? 0.33)
  const [explanation, setExplanation] = useState(q.explanation ?? '')
  const [natVal, setNatVal] = useState(q.correctNatValue ?? '')
  const [imgPath, setImgPath] = useState(q.imagePath ?? '')
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [deleting, setDeleting] = useState(false)

  const save = async (overrides = {}) => {
    setSaving(true)
    await fetch(`/api/admin/tests/${testId}/questions/${q.id}`, {
      method: 'PATCH', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ questionText: text, marks, negativeMarks: negMarks, explanation, correctNatValue: natVal || null, imagePath: imgPath, ...overrides }),
    })
    setSaving(false); setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  const debouncedSave = useDebounce(save, 900)

  const handleDelete = async () => {
    setDeleting(true)
    const res = await fetch(`/api/admin/tests/${testId}/questions/${q.id}`, { method: 'DELETE' })
    if (res.ok) onDelete(q.id)
    else setDeleting(false)
  }

  const borderColor = { MCQ: '#6366f1', MSQ: '#059669', NAT: '#d97706' }[q.type] || '#cbd5e1'

  return (
    <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderLeft: `5px solid ${borderColor}`, borderRadius: 14, padding: '1.75rem', boxShadow: '0 2px 8px rgba(0,0,0,0.04)', marginBottom: '2rem', position: 'relative', transition: 'box-shadow 0.2s' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem', borderBottom: '1px dashed #e2e8f0', paddingBottom: '1rem', flexWrap: 'wrap' }}>
        <div style={{ width: 38, height: 38, borderRadius: 10, background: `linear-gradient(135deg, ${borderColor}, ${borderColor}cc)`, color: '#fff', fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.95rem', flexShrink: 0 }}>
          {q.sequenceNo}
        </div>
        <TypeBadge type={q.type} />
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginLeft: 'auto' }}>
          <SaveStatus saving={saving} saved={saved} />
          {!confirmDelete ? (
            <button onClick={() => setConfirmDelete(true)} title="Delete question" style={{ background: 'none', border: '1px solid #fecaca', borderRadius: 6, padding: '0.3rem 0.6rem', cursor: 'pointer', color: '#ef4444', fontSize: '0.75rem', fontWeight: 700 }}>
              Delete
            </button>
          ) : (
            <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
              <span style={{ fontSize: '0.75rem', color: '#ef4444', fontWeight: 600 }}>Confirm?</span>
              <button onClick={handleDelete} disabled={deleting} style={{ background: '#ef4444', border: 'none', borderRadius: 6, padding: '0.3rem 0.6rem', cursor: 'pointer', color: '#fff', fontSize: '0.72rem', fontWeight: 700 }}>
                {deleting ? '…' : 'Yes'}
              </button>
              <button onClick={() => setConfirmDelete(false)} style={{ background: '#f1f5f9', border: 'none', borderRadius: 6, padding: '0.3rem 0.6rem', cursor: 'pointer', color: '#475569', fontSize: '0.72rem', fontWeight: 700 }}>
                No
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Question Text */}
      <label style={{ display: 'block', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: '#94a3b8', marginBottom: '0.4rem' }}>Question Text</label>
      <textarea
        value={text}
        onChange={e => { setText(e.target.value); debouncedSave({ questionText: e.target.value }) }}
        onBlur={() => save({ questionText: text })}
        rows={3}
        style={{ width: '100%', boxSizing: 'border-box', padding: '0.75rem 1rem', border: '1px solid #e2e8f0', borderRadius: 10, fontSize: '0.93rem', color: '#0f172a', resize: 'vertical', fontFamily: 'inherit', outline: 'none', lineHeight: 1.6 }}
        onFocus={e => e.target.style.borderColor = '#6366f1'}
      />

      {/* Marks row */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', margin: '1rem 0' }}>
        {[['Marks', marks, setMarks], ['Negative Marks', negMarks, setNegMarks]].map(([lbl, val, setter]) => (
          <div key={lbl}>
            <label style={{ display: 'block', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: '#94a3b8', marginBottom: '0.35rem' }}>{lbl}</label>
            <input
              type="number" step="0.01" value={val}
              onChange={e => { setter(e.target.value); debouncedSave({ [lbl === 'Marks' ? 'marks' : 'negativeMarks']: e.target.value }) }}
              onBlur={() => save()}
              style={{ width: '100%', boxSizing: 'border-box', padding: '0.6rem 0.9rem', border: '1px solid #e2e8f0', borderRadius: 8, fontSize: '0.9rem', color: '#0f172a', outline: 'none' }}
              onFocus={e => e.target.style.borderColor = '#6366f1'}
            />
          </div>
        ))}
      </div>

      {/* Question Image */}
      <label style={{ display: 'block', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: '#94a3b8', marginBottom: '0.4rem' }}>Question Image (Optional)</label>
      <ImageUploadZone testId={testId} entityType="q" entityId={q.id} currentPath={imgPath} onUploaded={url => { setImgPath(url); save({ imagePath: url }) }} />

      {/* Options for MCQ / MSQ */}
      {q.type !== 'NAT' && q.options?.length > 0 && (
        <div style={{ marginTop: '1.25rem' }}>
          <label style={{ display: 'block', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: '#94a3b8', marginBottom: '0.6rem' }}>Options — Click the label button to toggle correct answer</label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            {q.options.map(opt => (
              <OptionRow key={opt.id} opt={opt} testId={testId} questionId={q.id} />
            ))}
          </div>
        </div>
      )}

      {/* NAT value */}
      {q.type === 'NAT' && (
        <div style={{ marginTop: '1.25rem' }}>
          <label style={{ display: 'block', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: '#94a3b8', marginBottom: '0.4rem' }}>Correct NAT Value</label>
          <input
            type="number" step="any" value={natVal}
            onChange={e => { setNatVal(e.target.value); debouncedSave({ correctNatValue: e.target.value }) }}
            onBlur={() => save({ correctNatValue: natVal })}
            placeholder="e.g. 42.5"
            style={{ width: '100%', boxSizing: 'border-box', padding: '0.6rem 0.9rem', border: '1px solid #e2e8f0', borderRadius: 8, fontSize: '0.9rem', color: '#0f172a', outline: 'none' }}
            onFocus={e => e.target.style.borderColor = '#d97706'}
          />
        </div>
      )}

      {/* Explanation */}
      <div style={{ marginTop: '1.25rem' }}>
        <label style={{ display: 'block', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: '#94a3b8', marginBottom: '0.4rem' }}>Explanation / Solution</label>
        <textarea
          value={explanation}
          onChange={e => { setExplanation(e.target.value); debouncedSave({ explanation: e.target.value }) }}
          onBlur={() => save({ explanation })}
          rows={2}
          placeholder="Detailed explanation for the correct answer…"
          style={{ width: '100%', boxSizing: 'border-box', padding: '0.65rem 1rem', border: '1px solid #e2e8f0', borderRadius: 10, fontSize: '0.88rem', color: '#475569', fontFamily: 'inherit', resize: 'vertical', outline: 'none', lineHeight: 1.6 }}
          onFocus={e => e.target.style.borderColor = '#6366f1'}
        />
      </div>
    </div>
  )
}

// ─── Main AdminTestEdit Component ─────────────────────────────────────────────
export default function AdminTestEdit() {
  const { testId } = useParams()
  const [test, setTest] = useState(null)
  const [questions, setQuestions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [adding, setAdding] = useState(false)

  useEffect(() => {
    fetch(`/api/admin/tests/${testId}`)
      .then(r => { if (!r.ok) throw new Error('Failed to load test'); return r.json() })
      .then(data => { setTest(data); setQuestions(data.questions || []); setLoading(false) })
      .catch(err => { setError(err.message); setLoading(false) })
  }, [testId])

  const handleDeleteQuestion = (qId) => {
    setQuestions(prev => prev.filter(q => q.id !== qId))
  }

  const handleAddQuestion = async () => {
    setAdding(true)
    try {
      const res = await fetch(`/api/admin/tests/${testId}/questions`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ questionText: 'New Question' }),
      })
      const data = await res.json()
      if (data.questionId) {
        // Reload full test to get fresh question + options
        const r2 = await fetch(`/api/admin/tests/${testId}`)
        const updated = await r2.json()
        setQuestions(updated.questions || [])
      }
    } catch (err) {
      alert('Failed to add question: ' + err.message)
    } finally {
      setAdding(false)
    }
  }

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f8fafc', fontFamily: "'Inter', sans-serif" }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ width: 40, height: 40, borderRadius: '50%', border: '3px solid rgba(99,102,241,0.2)', borderTop: '3px solid #6366f1', animation: 'spin 0.9s linear infinite', margin: '0 auto 1rem' }} />
          <p style={{ color: '#64748b', fontWeight: 600 }}>Loading editor…</p>
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f8fafc', fontFamily: "'Inter', sans-serif" }}>
        <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: 12, padding: '2rem', maxWidth: 400, textAlign: 'center' }}>
          <p style={{ color: '#991b1b', fontWeight: 700 }}>Error: {error}</p>
          <Link to="/admin/dashboard" style={{ color: '#6366f1', fontWeight: 600, textDecoration: 'none' }}>← Back to Dashboard</Link>
        </div>
      </div>
    )
  }

  return (
    <div style={{ minHeight: '100vh', background: '#f1f5f9', fontFamily: "'Inter', -apple-system, sans-serif", overflowY: 'auto', color: '#1e293b' }}>
      {/* Top toolbar */}
      <div style={{ background: '#fff', borderBottom: '1px solid #e2e8f0', borderTop: '3px solid #6366f1', padding: '1rem 2rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', position: 'sticky', top: 0, zIndex: 100 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <Link to="/admin/dashboard" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', color: '#64748b', textDecoration: 'none', fontSize: '0.85rem', fontWeight: 600 }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><polyline points="15 18 9 12 15 6"/></svg>
            Dashboard
          </Link>
          <span style={{ color: '#cbd5e1' }}>/</span>
          <span style={{ fontSize: '0.95rem', fontWeight: 800, color: '#0f172a' }}>{test?.title}</span>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <a href={`/admin/tests/${testId}/export`} target="_blank" rel="noreferrer" style={{ background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: 8, padding: '0.5rem 1rem', textDecoration: 'none', color: '#475569', fontSize: '0.8rem', fontWeight: 700 }}>
            🖨 Export PDF
          </a>
          <button
            onClick={handleAddQuestion}
            disabled={adding}
            style={{ background: 'linear-gradient(135deg,#6366f1,#4f46e5)', border: 'none', borderRadius: 8, padding: '0.5rem 1.1rem', cursor: adding ? 'not-allowed' : 'pointer', color: '#fff', fontSize: '0.8rem', fontWeight: 700, opacity: adding ? 0.7 : 1 }}
          >
            {adding ? '…' : '+ Add Question'}
          </button>
          <span style={{ fontSize: '0.8rem', color: '#94a3b8', fontWeight: 600 }}>{questions.length} Questions</span>
        </div>
      </div>

      {/* Questions list */}
      <div style={{ maxWidth: 900, margin: '0 auto', padding: '2.5rem 1.5rem', boxSizing: 'border-box' }}>
        {questions.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '5rem 2rem', color: '#94a3b8' }}>
            <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📝</div>
            <h3 style={{ fontWeight: 700, color: '#64748b' }}>No questions yet</h3>
            <p style={{ fontSize: '0.9rem' }}>Click "+ Add Question" in the toolbar to get started.</p>
          </div>
        ) : (
          questions.map(q => (
            <QuestionCard key={q.id} q={q} testId={testId} onDelete={handleDeleteQuestion} />
          ))
        )}

        {questions.length > 0 && (
          <button
            onClick={handleAddQuestion}
            disabled={adding}
            style={{ width: '100%', padding: '0.9rem', background: '#fff', border: '2px dashed #cbd5e1', borderRadius: 12, cursor: adding ? 'not-allowed' : 'pointer', color: '#64748b', fontSize: '0.88rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', transition: 'all 0.2s' }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = '#6366f1'; e.currentTarget.style.color = '#6366f1' }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = '#cbd5e1'; e.currentTarget.style.color = '#64748b' }}
          >
            {adding ? '⏳ Adding…' : '+ Add Another Question'}
          </button>
        )}
      </div>
    </div>
  )
}
