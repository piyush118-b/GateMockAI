import React, { useState, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'

// ─── Loading Overlay ────────────────────────────────────────────────────────
function LoadingOverlay({ active, step, stepIndex }) {
  if (!active) return null
  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 9999,
      background: 'rgba(15,23,42,0.95)', backdropFilter: 'blur(12px)',
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
      color: '#fff', padding: '2rem', boxSizing: 'border-box',
    }}>
      <div style={{
        width: 72, height: 72, borderRadius: '50%',
        border: '4px solid rgba(99,102,241,0.15)',
        borderTop: '4px solid #6366f1',
        animation: 'spin 1s linear infinite',
        marginBottom: '2rem',
      }} />
      <div style={{ fontSize: '1.5rem', fontWeight: 800, textAlign: 'center', letterSpacing: '-0.025em', marginBottom: '0.75rem' }}>
        {step}
      </div>
      <div style={{ color: '#64748b', maxWidth: 420, textAlign: 'center', fontSize: '0.95rem', lineHeight: 1.6 }}>
        Local Qwen parsing engine active. Chunking PDF layout blocks, extracting questions, and aligning with answer keys…
      </div>
      <div style={{ marginTop: '2rem', display: 'flex', gap: '0.5rem' }}>
        {[0,1,2,3,4].map(i => (
          <div key={i} style={{
            width: 8, height: 8, borderRadius: '50%',
            background: i <= stepIndex ? '#6366f1' : 'rgba(99,102,241,0.2)',
            transition: 'background 0.4s',
          }} />
        ))}
      </div>
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  )
}

// ─── Similarity Search Playground ───────────────────────────────────────────
function SearchPlayground() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState(null)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState(null)

  const handleSearch = async () => {
    if (!query.trim()) return
    setSearching(true); setResults(null); setError(null)
    try {
      const res = await fetch(`/api/admin/rag/test?query=${encodeURIComponent(query)}&topK=3`, { method: 'POST' })
      if (!res.ok) throw new Error('Search failed')
      const data = await res.json()
      setResults(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setSearching(false)
    }
  }

  return (
    <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 16, padding: '2rem', boxShadow: '0 4px 6px rgba(0,0,0,0.05)' }}>
      <div style={{ borderBottom: '1px solid #e2e8f0', paddingBottom: '0.75rem', marginBottom: '1.5rem' }}>
        <h2 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: '#0f172a' }}>
          PGVector Similarity Search
        </h2>
        <p style={{ margin: '0.3rem 0 0', fontSize: '0.82rem', color: '#64748b' }}>
          Query your vector store to verify retrieval quality.
        </p>
      </div>

      <div style={{ display: 'flex', gap: '0.75rem' }}>
        <input
          type="text"
          value={query}
          onChange={e => setQuery(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSearch()}
          placeholder="e.g. Virtual Memory, CPU Scheduling, SQL JOIN…"
          style={{
            flex: 1, padding: '0.75rem 1rem',
            border: '1px solid #cbd5e1', borderRadius: 10, fontSize: '0.9rem',
            color: '#0f172a', background: '#fff', outline: 'none', boxSizing: 'border-box',
          }}
        />
        <button
          onClick={handleSearch}
          disabled={searching}
          style={{
            padding: '0 1.25rem', borderRadius: 10, border: 'none', cursor: 'pointer',
            background: searching ? '#94a3b8' : 'linear-gradient(135deg,#0f172a,#1e293b)',
            color: '#fff', fontWeight: 700, fontSize: '0.8rem', textTransform: 'uppercase',
            letterSpacing: '0.05em', whiteSpace: 'nowrap',
          }}
        >
          {searching ? '…' : 'Search'}
        </button>
      </div>

      <div style={{ marginTop: '1.25rem', maxHeight: 420, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
        {!results && !error && !searching && (
          <div style={{ color: '#94a3b8', textAlign: 'center', padding: '3rem 0', fontSize: '0.9rem' }}>
            Type a query and hit Search or Enter to test vector retrieval.
          </div>
        )}
        {searching && (
          <div style={{ textAlign: 'center', padding: '3rem 0', color: '#6366f1' }}>
            <div style={{ width: 36, height: 36, borderRadius: '50%', border: '3px solid rgba(99,102,241,0.2)', borderTop: '3px solid #6366f1', animation: 'spin 0.8s linear infinite', margin: '0 auto 1rem' }} />
            Searching PGVector…
          </div>
        )}
        {error && (
          <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: 10, padding: '0.9rem 1rem', color: '#991b1b', fontSize: '0.9rem' }}>
            Error: {error}
          </div>
        )}
        {results && results.length === 0 && (
          <div style={{ color: '#94a3b8', textAlign: 'center', padding: '2rem 0', fontSize: '0.9rem' }}>
            No matching vectors found. Upload a past paper first!
          </div>
        )}
        {results && results.map((item, idx) => (
          <div key={idx} style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 10, padding: '1rem 1.25rem', fontSize: '0.88rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: '#64748b', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.6rem', borderBottom: '1px solid #e2e8f0', paddingBottom: '0.5rem' }}>
              <span>Rank {idx + 1} | {item.metadata?.subject || 'Unknown'} | {item.metadata?.topic || 'General'}</span>
              <span style={{ color: '#6366f1' }}>Score: {item.metadata?.distance ? (1 - parseFloat(item.metadata.distance)).toFixed(4) : 'N/A'}</span>
            </div>
            <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', margin: 0, color: '#1e293b', lineHeight: 1.6 }}>{item.content}</pre>
          </div>
        ))}
      </div>
    </div>
  )
}

// ─── Review Panel ────────────────────────────────────────────────────────────
function ReviewPanel({ draft, onConfirm, onCancel }) {
  const [confirming, setConfirming] = useState(false)
  const [result, setResult] = useState(null)

  const handleConfirm = async () => {
    setConfirming(true)
    try {
      const res = await fetch('/api/admin/rag/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(draft),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Failed to ingest')
      setResult({ success: true, message: data.message })
      setTimeout(() => onConfirm(), 2000)
    } catch (err) {
      setResult({ success: false, message: err.message })
    } finally {
      setConfirming(false)
    }
  }

  return (
    <div style={{ fontFamily: "'Inter', sans-serif" }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ margin: 0, fontSize: '1.4rem', fontWeight: 800, color: '#0f172a' }}>
            Review Extracted Paper
          </h2>
          <p style={{ margin: '0.25rem 0 0', color: '#64748b', fontSize: '0.9rem' }}>
            {draft.questions.length} questions extracted · Verify before committing to PGVector
          </p>
        </div>
        <button onClick={onCancel} style={{ background: 'none', border: '1px solid #e2e8f0', borderRadius: 8, padding: '0.5rem 1rem', cursor: 'pointer', color: '#475569', fontSize: '0.82rem', fontWeight: 600 }}>
          ← Cancel, Go Back
        </button>
      </div>

      {result && (
        <div style={{
          padding: '1rem 1.25rem', borderRadius: 10, marginBottom: '1.5rem', fontWeight: 600, fontSize: '0.9rem',
          background: result.success ? '#f0fdf4' : '#fef2f2',
          border: `1px solid ${result.success ? '#bbf7d0' : '#fecaca'}`,
          color: result.success ? '#166534' : '#991b1b',
        }}>
          {result.success ? '✓ ' : '⚠ '}{result.message}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem', alignItems: 'start' }}>
        {/* Questions list */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {draft.questions.map((q, idx) => {
            const typeColor = q.type === 'MCQ' ? '#4f46e5' : q.type === 'MSQ' ? '#059669' : '#d97706'
            const typeBg = q.type === 'MCQ' ? 'rgba(79,70,229,0.08)' : q.type === 'MSQ' ? 'rgba(5,150,105,0.08)' : 'rgba(217,119,6,0.08)'
            return (
              <div key={idx} style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 14, padding: '1.5rem', boxShadow: '0 2px 4px rgba(0,0,0,0.04)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.9rem' }}>
                  <span style={{ background: typeBg, color: typeColor, fontSize: '0.72rem', fontWeight: 700, padding: '0.25rem 0.75rem', borderRadius: 6, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Q{q.sequenceNo} | {q.type}
                  </span>
                  <span style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>
                    {q.marks} M | -{q.negativeMarks} Neg
                  </span>
                </div>
                <p style={{ margin: '0 0 1rem', fontSize: '0.95rem', lineHeight: 1.65, color: '#0f172a', fontWeight: 600 }}>
                  {q.questionText}
                </p>
                {q.options && q.options.length > 0 && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', marginBottom: '0.9rem' }}>
                    {q.options.map((opt, oi) => (
                      <div key={oi} style={{
                        display: 'flex', alignItems: 'center', gap: '0.75rem',
                        padding: '0.6rem 1rem', borderRadius: 8,
                        background: (opt.isCorrect || opt.is_correct) ? 'rgba(5,150,105,0.07)' : '#f8fafc',
                        border: `1px solid ${(opt.isCorrect || opt.is_correct) ? 'rgba(5,150,105,0.25)' : '#e2e8f0'}`,
                      }}>
                        <span style={{
                          width: 22, height: 22, borderRadius: '50%', flexShrink: 0,
                          display: 'flex', alignItems: 'center', justifyContent: 'center',
                          fontSize: '0.72rem', fontWeight: 700,
                          background: (opt.isCorrect || opt.is_correct) ? '#059669' : 'transparent',
                          border: `1px solid ${(opt.isCorrect || opt.is_correct) ? '#059669' : '#cbd5e1'}`,
                          color: (opt.isCorrect || opt.is_correct) ? '#fff' : '#475569',
                        }}>
                          {opt.label || opt.optionLabel}
                        </span>
                        <span style={{ fontSize: '0.88rem', color: '#1e293b' }}>{opt.text || opt.optionText}</span>
                      </div>
                    ))}
                  </div>
                )}
                {q.type === 'NAT' && (
                  <div style={{ background: 'rgba(217,119,6,0.06)', border: '1px dashed rgba(217,119,6,0.3)', borderRadius: 8, padding: '0.75rem 1rem', marginBottom: '0.9rem' }}>
                    <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#d97706', textTransform: 'uppercase', marginBottom: '0.25rem' }}>NAT Correct Value</div>
                    <div style={{ fontSize: '1rem', fontWeight: 700, color: '#0f172a' }}>{q.correctNatValue} (±{q.natTolerance ?? 0})</div>
                  </div>
                )}
                {q.explanation && (
                  <div style={{ borderLeft: '3px solid #6366f1', paddingLeft: '0.9rem', fontSize: '0.83rem', color: '#475569', lineHeight: 1.6 }}>
                    <strong>Explanation:</strong> {q.explanation}
                  </div>
                )}
              </div>
            )
          })}
        </div>

        {/* Summary Sidebar */}
        <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 16, padding: '1.5rem', boxShadow: '0 4px 6px rgba(0,0,0,0.05)', position: 'sticky', top: '1.5rem' }}>
          <h3 style={{ margin: '0 0 1rem', fontSize: '1rem', fontWeight: 700, color: '#0f172a', borderBottom: '1px solid #e2e8f0', paddingBottom: '0.75rem' }}>
            Ingestion Summary
          </h3>
          {[
            ['Title', draft.title],
            ['Subject Domain', draft.subject],
            ['Topic / Year', draft.topic],
            ['Total Questions', draft.questions.length],
            ['Duration', `${draft.durationMinutes} minutes`],
            ['MCQ Count', draft.questions.filter(q => q.type === 'MCQ').length],
            ['MSQ Count', draft.questions.filter(q => q.type === 'MSQ').length],
            ['NAT Count', draft.questions.filter(q => q.type === 'NAT').length],
          ].map(([label, val]) => (
            <div key={label} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid #f1f5f9', fontSize: '0.88rem' }}>
              <span style={{ color: '#64748b' }}>{label}</span>
              <strong style={{ color: '#0f172a', maxWidth: '55%', textAlign: 'right', wordBreak: 'break-word' }}>{val}</strong>
            </div>
          ))}

          <button
            onClick={handleConfirm}
            disabled={confirming || !!result?.success}
            style={{
              width: '100%', marginTop: '1.5rem', padding: '0.9rem',
              background: confirming || result?.success ? '#94a3b8' : 'linear-gradient(135deg,#4f46e5,#4338ca)',
              border: 'none', borderRadius: 10, color: '#fff',
              fontSize: '0.9rem', fontWeight: 700, textTransform: 'uppercase',
              letterSpacing: '0.05em', cursor: confirming || result?.success ? 'not-allowed' : 'pointer',
              boxShadow: '0 4px 14px rgba(79,70,229,0.3)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
            }}
          >
            {confirming ? '⏳ Ingesting…' : result?.success ? '✓ Committed!' : '✓ Confirm & Ingest to PGVector'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ─── Main AdminRag Component ─────────────────────────────────────────────────
const UPLOAD_STEPS = [
  'Reading raw text blocks using Apache PDFBox…',
  'Analyzing layouts and isolating individual question elements…',
  'Extracting and aligning options with the Answer Key Map…',
  'Generating comprehensive explanations with Qwen 2.5 Coder…',
  'Synthesizing MSQ and NAT structures… Almost complete!',
]

export default function AdminRag() {
  const [vectorCount, setVectorCount] = useState('—')
  const [uploading, setUploading] = useState(false)
  const [uploadStep, setUploadStep] = useState(UPLOAD_STEPS[0])
  const [uploadStepIdx, setUploadStepIdx] = useState(0)
  const [draft, setDraft] = useState(null)  // holds parsed paper for review
  const [alert, setAlert] = useState(null)  // { type: 'success'|'error', msg }
  const stepIntervalRef = useRef(null)

  useEffect(() => {
    fetch('/api/admin/rag/status')
      .then(r => r.json())
      .then(d => setVectorCount(d.vectorCount ?? '?'))
      .catch(() => {})
  }, [])

  const startStepAnimation = () => {
    let idx = 0
    stepIntervalRef.current = setInterval(() => {
      idx = Math.min(idx + 1, UPLOAD_STEPS.length - 1)
      setUploadStep(UPLOAD_STEPS[idx])
      setUploadStepIdx(idx)
    }, 2500)
  }

  const stopStepAnimation = () => {
    if (stepIntervalRef.current) {
      clearInterval(stepIntervalRef.current)
      stepIntervalRef.current = null
    }
  }

  const handleFormSubmit = async (e) => {
    e.preventDefault()
    const form = e.target
    const fd = new FormData(form)
    setUploading(true)
    setUploadStep(UPLOAD_STEPS[0])
    setUploadStepIdx(0)
    startStepAnimation()

    try {
      const res = await fetch('/api/admin/rag/upload', { method: 'POST', body: fd })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Upload failed')
      setDraft(data)
    } catch (err) {
      setAlert({ type: 'error', msg: err.message })
    } finally {
      stopStepAnimation()
      setUploading(false)
    }
  }

  const refreshStatus = () => {
    fetch('/api/admin/rag/status')
      .then(r => r.json())
      .then(d => setVectorCount(d.vectorCount ?? '?'))
      .catch(() => {})
  }

  if (draft) {
    return (
      <div style={{ minHeight: '100vh', background: '#f8fafc', fontFamily: "'Inter', sans-serif", padding: '2rem', boxSizing: 'border-box', overflowY: 'auto' }}>
        <div style={{ maxWidth: 1200, margin: '0 auto' }}>
          {alert && (
            <div style={{ padding: '0.9rem 1.25rem', borderRadius: 10, marginBottom: '1.5rem', fontWeight: 600, fontSize: '0.9rem', background: alert.type === 'success' ? '#f0fdf4' : '#fef2f2', border: `1px solid ${alert.type === 'success' ? '#bbf7d0' : '#fecaca'}`, color: alert.type === 'success' ? '#166534' : '#991b1b' }}>
              {alert.msg}
            </div>
          )}
          <ReviewPanel
            draft={draft}
            onConfirm={() => { setDraft(null); setAlert({ type: 'success', msg: 'Paper successfully committed to PGVector!' }); refreshStatus() }}
            onCancel={() => setDraft(null)}
          />
        </div>
      </div>
    )
  }

  return (
    <div style={{ minHeight: '100vh', background: '#f8fafc', fontFamily: "'Inter', sans-serif", overflowY: 'auto', color: '#1e293b' }}>
      <LoadingOverlay active={uploading} step={uploadStep} stepIndex={uploadStepIdx} />

      <div style={{ maxWidth: 1200, margin: '0 auto', padding: '2rem', boxSizing: 'border-box' }}>
        {/* Back nav */}
        <div style={{ marginBottom: '1.5rem' }}>
          <Link to="/admin/dashboard" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', color: '#64748b', textDecoration: 'none', fontSize: '0.9rem', fontWeight: 600 }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><polyline points="15 18 9 12 15 6"/></svg>
            Back to Dashboard
          </Link>
        </div>

        {/* Hero */}
        <div style={{ background: 'linear-gradient(135deg,#0f172a 0%,#1e293b 100%)', padding: '2.5rem', borderRadius: 16, color: '#fff', marginBottom: '2rem', boxShadow: '0 10px 15px rgba(0,0,0,0.1)' }}>
          <h1 style={{ margin: 0, fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.025em' }}>RAG Document Ingestion & PGVector</h1>
          <p style={{ margin: '0.6rem 0 0', color: '#94a3b8', fontSize: '0.95rem', lineHeight: 1.6 }}>
            Upload official GATE syllabus guides, past exam papers, and align them with answer keys. Parsed question blocks are stored persistently in Postgres PGVector.
          </p>
        </div>

        {/* Alerts */}
        {alert && (
          <div style={{ padding: '0.9rem 1.25rem', borderRadius: 10, marginBottom: '1.5rem', fontWeight: 600, fontSize: '0.9rem', background: alert.type === 'success' ? '#f0fdf4' : '#fef2f2', border: `1px solid ${alert.type === 'success' ? '#bbf7d0' : '#fecaca'}`, color: alert.type === 'success' ? '#166534' : '#991b1b' }}>
            {alert.type === 'success' ? '✓ ' : '⚠ '}{alert.msg}
          </div>
        )}

        {/* Main grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: '2rem', alignItems: 'start' }}>
          {/* Upload form card */}
          <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 16, padding: '2rem', boxShadow: '0 4px 6px rgba(0,0,0,0.05)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #e2e8f0', paddingBottom: '0.75rem', marginBottom: '1.5rem' }}>
              <h2 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700, color: '#0f172a' }}>Ingest Question Paper & Key</h2>
              <span style={{ background: '#ecfdf5', color: '#047857', border: '1px solid #a7f3d0', borderRadius: 9999, padding: '0.25rem 0.75rem', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase' }}>
                {vectorCount} Vectors
              </span>
            </div>

            <form id="ragForm" onSubmit={handleFormSubmit} encType="multipart/form-data">
              {/* Question Paper file */}
              <div style={{ marginBottom: '1.25rem' }}>
                <label style={{ display: 'block', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#475569', marginBottom: '0.5rem' }}>
                  GATE Question Paper File (.pdf, .txt) *
                </label>
                <input
                  type="file" name="file" accept=".pdf,.txt" required
                  style={{ width: '100%', padding: '0.65rem 1rem', boxSizing: 'border-box', background: '#f8fafc', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: '0.88rem', color: '#0f172a', cursor: 'pointer' }}
                />
              </div>

              {/* Answer Key File */}
              <div style={{ marginBottom: '1.25rem' }}>
                <label style={{ display: 'block', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#475569', marginBottom: '0.5rem' }}>
                  Official Answer Key PDF File (.pdf, .txt) — Optional
                </label>
                <input
                  type="file" name="answerKeyFile" accept=".pdf,.txt"
                  style={{ width: '100%', padding: '0.65rem 1rem', boxSizing: 'border-box', background: '#f8fafc', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: '0.88rem', color: '#0f172a', cursor: 'pointer' }}
                />
              </div>

              {/* Subject + Topic row */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.25rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#475569', marginBottom: '0.5rem' }}>
                    Domain / Branch *
                  </label>
                  <input
                    type="text" name="subject" required placeholder="e.g., CSE"
                    style={{ width: '100%', padding: '0.75rem 1rem', boxSizing: 'border-box', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: '0.9rem', color: '#0f172a', background: '#fff', outline: 'none' }}
                    onFocus={e => e.target.style.borderColor = '#6366f1'}
                    onBlur={e => e.target.style.borderColor = '#cbd5e1'}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#475569', marginBottom: '0.5rem' }}>
                    Exam Year / Code *
                  </label>
                  <input
                    type="text" name="topic" required placeholder="e.g., GATE 2024"
                    style={{ width: '100%', padding: '0.75rem 1rem', boxSizing: 'border-box', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: '0.9rem', color: '#0f172a', background: '#fff', outline: 'none' }}
                    onFocus={e => e.target.style.borderColor = '#6366f1'}
                    onBlur={e => e.target.style.borderColor = '#cbd5e1'}
                  />
                </div>
              </div>

              {/* Answer Key Text */}
              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#475569', marginBottom: '0.25rem' }}>
                  Manual Answer Key Entry — Optional but Recommended
                </label>
                <p style={{ margin: '0 0 0.5rem', fontSize: '0.78rem', color: '#94a3b8' }}>
                  Format as <code>question_number: value</code> (one per line)
                </p>
                <textarea
                  name="answerKeyText"
                  rows={6}
                  placeholder={"1: B\n2: A, C\n3: 15.5\n4: D"}
                  style={{ width: '100%', padding: '0.75rem 1rem', boxSizing: 'border-box', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: '0.88rem', color: '#0f172a', background: '#fff', fontFamily: 'monospace', resize: 'vertical', outline: 'none', lineHeight: 1.6 }}
                  onFocus={e => e.target.style.borderColor = '#6366f1'}
                  onBlur={e => e.target.style.borderColor = '#cbd5e1'}
                />
              </div>

              <button
                type="submit"
                disabled={uploading}
                style={{
                  width: '100%', padding: '0.875rem 1.5rem',
                  background: uploading ? '#94a3b8' : 'linear-gradient(135deg,#4f46e5,#4338ca)',
                  border: 'none', borderRadius: 10, color: '#fff', cursor: uploading ? 'not-allowed' : 'pointer',
                  fontSize: '0.875rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em',
                  boxShadow: uploading ? 'none' : '0 4px 14px rgba(79,70,229,0.25)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
                }}
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="17 8 12 3 7 8"/>
                  <line x1="12" y1="3" x2="12" y2="15"/>
                </svg>
                {uploading ? 'Parsing with Local AI…' : 'Parse Paper with Local Ollama AI'}
              </button>
            </form>
          </div>

          {/* Search Playground */}
          <SearchPlayground />
        </div>
      </div>

      <style>{`
        @media (max-width: 968px) {
          .rag-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  )
}
