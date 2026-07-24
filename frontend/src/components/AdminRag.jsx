import React, { useState, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'

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

      {draft.warningMessage && (
        <div style={{
          padding: '0.75rem 1rem',
          background: '#fffbeb',
          border: '1px solid #fcd34d',
          borderRadius: 8,
          color: '#92400e',
          fontSize: '0.875rem',
          fontWeight: 600,
          marginBottom: '1rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem'
        }} className="font-sans">
          <span>⚠</span>
          <span>{draft.warningMessage}</span>
        </div>
      )}

      <div style={{
        background: '#eef2ff',
        border: '1px solid #c7d2fe',
        borderRadius: 8,
        padding: '0.75rem 1rem',
        color: '#3730a3',
        fontSize: '0.875rem',
        fontWeight: 600,
        marginBottom: '1.5rem'
      }} className="font-sans">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: draft.tokenUsage ? '0.2rem' : '0' }}>
          <span>✦</span>
          <span>Gemini extracted {draft.totalExtracted || draft.questions.length} questions</span>
        </div>
        {draft.tokenUsage && (
          <div style={{ fontSize: '0.8rem', color: '#4f46e5', fontWeight: 500 }}>
            Tokens used: {draft.tokenUsage.totalTokens} · Est. cost: ${draft.tokenUsage.estimatedCostUsd ? draft.tokenUsage.estimatedCostUsd.toFixed(4) : '0.0000'}
          </div>
        )}
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
          
          <div style={{ marginTop: '0.5rem', textAlign: 'center', fontSize: '0.78rem', color: '#64748b', fontWeight: 500 }} className="font-sans">
            Powered by Gemini AI — direct PDF multimodal extraction
          </div>
        </div>
      </div>
    </div>
  )
}

// ─── Main AdminRag Component ─────────────────────────────────────────────────
const statusMessages = [
  "Sending PDF to Gemini AI...",
  "Extracting all questions, options and solving answers...",
  "Enriching questions with metadata and saving to database..."
];

export default function AdminRag() {
  const formRef = useRef(null)
  const [vectorCount, setVectorCount] = useState('—')
  const [uploading, setUploading] = useState(false)
  const [draft, setDraft] = useState(null)  // holds parsed paper for review
  const [alert, setAlert] = useState(null)  // { type: 'success'|'error', msg }
  
  // Custom Loading states
  const [elapsedSeconds, setElapsedSeconds] = useState(0)
  const [messageIndex, setMessageIndex] = useState(0)

  const refreshStatus = () => {
    setVectorCount('—')
    fetch('/api/admin/rag/vector-count')
      .then(r => r.json())
      .then(d => setVectorCount(d.count ?? '—'))
      .catch(() => setVectorCount('—'))
  }

  useEffect(() => {
    refreshStatus()
  }, [])

  // Timer useEffect
  useEffect(() => {
    let timer = null;
    if (uploading) {
      setElapsedSeconds(0);
      timer = setInterval(() => {
        setElapsedSeconds(prev => prev + 1);
      }, 1000);
    } else {
      setElapsedSeconds(0);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [uploading]);

  // Message Index useEffect
  useEffect(() => {
    let msgTimer = null;
    if (uploading) {
      setMessageIndex(0);
      msgTimer = setInterval(() => {
        setMessageIndex(prev => (prev + 1) % statusMessages.length);
      }, 4000);
    }
    return () => {
      if (msgTimer) clearInterval(msgTimer);
    };
  }, [uploading]);

  const handleFormSubmit = async (e) => {
    if (e && e.preventDefault) e.preventDefault()
    const form = formRef.current || (e && e.target)
    if (!form) return
    
    const rawFd = new FormData(form)
    const file = rawFd.get("file")
    const branchRaw = rawFd.get("subject") || "CSE"
    const topicRaw = rawFd.get("topic") || ""

    // Extract year digits from topic/year input
    const yearMatch = topicRaw.match(/\d{4}/)
    const year = yearMatch ? parseInt(yearMatch[0], 10) : new Date().getFullYear()

    // Normalize values
    const branch = branchRaw.trim().toUpperCase()
    const examName = topicRaw.includes(branch) ? topicRaw.trim() : `GATE ${branch} ${year}`
    const paperId = `gate_${branch.toLowerCase()}_${year}`

    // Construct the backend payload expected by PaperIngestionController
    const fd = new FormData()
    fd.append("questionPaper", file)
    fd.append("paperId", paperId)
    fd.append("examName", examName)
    fd.append("year", year)
    fd.append("branch", branch)

    setUploading(true)
    setAlert(null)

    try {
      const res = await fetch('/api/pipeline/ingest', { method: 'POST', body: fd })
      let data
      try {
        data = await res.json()
      } catch (jsonErr) {
        const err = new Error('Server returned an invalid response')
        err.status = res.status
        throw err
      }
      if (!res.ok) {
        const err = new Error(data.error || data.message || 'Ingestion failed')
        err.status = res.status
        throw err
      }
      
      setAlert({ 
        type: 'success', 
        msg: `Paper successfully ingested! "${examName}" — ${data.totalQuestions || 0} questions extracted and enriched by Gemini AI. Questions below the confidence threshold have been queued for admin review.`,
        showReviewLink: true,
      })
      refreshStatus()
      form.reset()
    } catch (err) {
      const status = err.status
      let msg = ""

      if (status === 429) {
        msg = "Daily token limit reached. Resets at midnight."
      } else if (status === 401 || status === 403) {
        msg = "Check that you are logged in and authorized."
      } else {
        msg = `Ingestion failed: ${err.message || 'Unknown error occurred.'}`
      }

      setAlert({ 
        type: 'error', 
        msg: msg
      })
    } finally {
      setUploading(false)
    }
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
            Upload official GATE past exam papers. Gemini AI extracts all questions in one fast pass — results are stored in PostgreSQL PGVector for semantic retrieval.
          </p>
        </div>

        {/* Alerts */}
        {alert && (
          <div style={{ 
            padding: '1rem 1.25rem', 
            borderRadius: 10, 
            marginBottom: '1.5rem', 
            fontWeight: 600, 
            fontSize: '0.9rem', 
            background: alert.type === 'success' ? '#f0fdf4' : '#fef2f2', 
            border: `1px solid ${alert.type === 'success' ? '#bbf7d0' : '#fecaca'}`, 
            color: alert.type === 'success' ? '#166534' : '#991b1b',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.75rem'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span>{alert.type === 'success' ? '✓ ' : '⚠ '}</span>
              <span style={{ lineHeight: 1.5 }}>{alert.msg}</span>
            </div>
            {alert.showReviewLink && (
              <a
                href="/admin/review-queue"
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: '0.4rem',
                  background: '#059669', color: '#fff', border: 'none',
                  borderRadius: 8, padding: '0.5rem 1rem',
                  fontSize: '0.8rem', fontWeight: 700, textDecoration: 'none',
                  cursor: 'pointer', alignSelf: 'flex-start',
                }}
              >
                <span>→ Review Flagged Questions</span>
              </a>
            )}

          </div>
        )}

        {/* Main grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: '2rem', alignItems: 'start' }} className="rag-grid">
          {/* Upload form card */}
          <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 16, padding: '2rem', boxShadow: '0 4px 6px rgba(0,0,0,0.05)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #e2e8f0', paddingBottom: '0.75rem', marginBottom: '1.5rem' }}>
              <h2 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700, color: '#0f172a' }}>Ingest Question Paper</h2>
              <span style={{ background: '#ecfdf5', color: '#047857', border: '1px solid #a7f3d0', borderRadius: 9999, padding: '0.25rem 0.75rem', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase' }}>
                {vectorCount} {vectorCount === '—' ? 'VECTORS' : 'VECTORS'}
              </span>
            </div>

            <form id="ragForm" ref={formRef} onSubmit={handleFormSubmit} encType="multipart/form-data">
              <div style={{
                opacity: uploading ? 0.5 : 1,
                pointerEvents: uploading ? 'none' : 'auto',
                transition: 'opacity 0.2s'
              }}>
                {/* Question Paper file */}
                <div style={{ marginBottom: '1.25rem' }}>
                  <label style={{ display: 'block', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#475569', marginBottom: '0.5rem' }}>
                    GATE Question Paper File (.pdf, .txt) *
                  </label>
                  <input
                    type="file" name="file" accept=".pdf,.txt" required disabled={uploading}
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
                      type="text" name="subject" required placeholder="e.g., CSE" disabled={uploading}
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
                      type="text" name="topic" required placeholder="e.g., GATE 2024" disabled={uploading}
                      style={{ width: '100%', padding: '0.75rem 1rem', boxSizing: 'border-box', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: '0.9rem', color: '#0f172a', background: '#fff', outline: 'none' }}
                      onFocus={e => e.target.style.borderColor = '#6366f1'}
                      onBlur={e => e.target.style.borderColor = '#cbd5e1'}
                    />
                  </div>
                </div>

                <p style={{ color: '#64748b', fontSize: '0.8rem', margin: '6px 0 0', fontStyle: 'italic' }}>
                  ✨ Answer keys are no longer required — Gemini reads and solves the paper directly.
                </p>
              </div>

              {uploading ? (
                <div style={{
                  background: '#f8fafc',
                  border: '2px dashed #cbd5e1',
                  borderRadius: 12,
                  padding: '1.5rem',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  textAlign: 'center',
                  marginTop: '1.5rem'
                }} className="animate-fade-in font-sans">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#4f46e5', fontWeight: 800, fontSize: '0.95rem' }}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" style={{ animation: 'spin 3s linear infinite' }}>
                      <path d="M12 2l2.4 7.2 7.2 2.4-7.2 2.4-2.4 7.2-2.4-7.2-7.2-2.4 7.2-2.4z"/>
                    </svg>
                    <span>✦ Gemini is reading your paper...</span>
                  </div>
                  
                  <div style={{ width: '100%', marginTop: '1rem', marginBottom: '0.5rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'center', fontSize: '0.8rem', fontWeight: 700, color: '#475569', marginBottom: '0.6rem' }}>
                      <span>{statusMessages[messageIndex]}</span>
                    </div>
                    
                    <div style={{ width: '100%', height: 8, background: '#e2e8f0', borderRadius: 4, overflow: 'hidden', position: 'relative' }}>
                      <div className="gemini-loading-bar" style={{ height: '100%', background: '#4f46e5', borderRadius: 4, width: '40%', position: 'absolute' }} />
                    </div>
                  </div>
                  
                  <div style={{ marginTop: '0.75rem', fontSize: '0.8rem', fontWeight: 700, color: '#64748b', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <span>⏱ {elapsedSeconds}s elapsed</span>
                  </div>
                </div>
              ) : (
                <button
                  type="submit"
                  disabled={uploading}
                  style={{
                    width: '100%', padding: '0.875rem 1.5rem',
                    background: 'linear-gradient(135deg,#4f46e5,#4338ca)',
                    border: 'none', borderRadius: 10, color: '#fff', cursor: 'pointer',
                    fontSize: '0.875rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em',
                    boxShadow: '0 4px 14px rgba(79,70,229,0.25)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
                  }}
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2l2.4 7.2 7.2 2.4-7.2 2.4-2.4 7.2-2.4-7.2-7.2-2.4 7.2-2.4z"/>
                  </svg>
                  Parse Paper with Gemini AI
                </button>
              )}
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
        @keyframes progressPulse {
          0% { left: -30%; width: 30%; }
          50% { left: 40%; width: 50%; }
          100% { left: 100%; width: 30%; }
        }
        .gemini-loading-bar {
          animation: progressPulse 1.8s ease-in-out infinite;
        }
      `}</style>
    </div>
  )
}
