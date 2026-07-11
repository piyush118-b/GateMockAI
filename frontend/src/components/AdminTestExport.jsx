import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'

export default function AdminTestExport() {
  const { testId } = useParams()
  const navigate = useNavigate()
  const [test, setTest] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetch(`/api/admin/tests/${testId}`)
      .then(r => { if (!r.ok) throw new Error('Test not found'); return r.json() })
      .then(data => { setTest(data); setLoading(false) })
      .catch(err => { setError(err.message); setLoading(false) })
  }, [testId])

  if (loading) return <div style={{ fontFamily: 'Inter, Arial, sans-serif', padding: '2rem', textAlign: 'center', color: '#475569' }}>Loading…</div>
  if (error) return <div style={{ fontFamily: 'Inter, Arial, sans-serif', padding: '2rem', textAlign: 'center', color: '#ef4444' }}>Error: {error}</div>

  const mcq = test.questions.filter(q => q.type === 'MCQ')
  const msq = test.questions.filter(q => q.type === 'MSQ')
  const nat = test.questions.filter(q => q.type === 'NAT')

  return (
    <div style={{ fontFamily: "'Inter', Arial, sans-serif", background: '#ffffff', color: '#0f172a', fontSize: '11pt', lineHeight: 1.55, padding: '2rem 3rem', maxWidth: 860, margin: '0 auto', boxSizing: 'border-box' }}>
      {/* Print controls — hidden in print */}
      <div className="no-print" style={{ position: 'fixed', top: '1rem', right: '1rem', display: 'flex', gap: '0.5rem', zIndex: 100 }}>
        <button
          onClick={() => window.print()}
          style={{ background: '#1e3a8a', color: '#fff', border: 'none', padding: '0.6rem 1.5rem', borderRadius: 8, fontWeight: 700, cursor: 'pointer', fontSize: '0.95rem' }}
        >
          🖨 Save as PDF
        </button>
        <button
          onClick={() => {
            if (window.history.state && window.history.state.idx > 0) {
              navigate(-1)
            } else {
              navigate(`/admin/tests/${testId}/edit`)
            }
          }}
          style={{ background: '#f1f5f9', color: '#1e3a8a', border: '1px solid #cbd5e1', padding: '0.6rem 1rem', borderRadius: 8, cursor: 'pointer' }}
        >
          ← Back
        </button>
      </div>

      {/* Exam header */}
      <div style={{ textAlign: 'center', borderBottom: '3px double #1e3a8a', paddingBottom: '1.25rem', marginBottom: '1.5rem' }}>
        <div style={{ fontSize: '2rem', fontWeight: 900, color: '#1e3a8a', letterSpacing: '0.1em' }}>GATE</div>
        <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#1e40af', marginTop: '0.25rem' }}>{test.title}</div>
        <div style={{ fontSize: '0.85rem', color: '#475569', marginTop: '0.5rem' }}>
          Duration: {test.durationMinutes} minutes &nbsp;|&nbsp;
          Total Questions: {test.questions.length} &nbsp;|&nbsp;
          Maximum Marks: {test.totalMarks}
        </div>
      </div>

      {/* Instructions */}
      <div style={{ background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: 4, padding: '0.75rem 1rem', fontSize: '0.8rem', marginBottom: '1.5rem' }}>
        <strong style={{ color: '#1e3a8a' }}>General Instructions: </strong>
        MCQ: Only one option is correct. Wrong answer: <strong>-1/3</strong> negative marking for 1-mark, <strong>-2/3</strong> for 2-mark.
        MSQ: One or more options correct. No negative marking.
        NAT: Enter numerical value. No options given. No negative marking.
      </div>

      {/* Sections */}
      {[
        { label: 'Section A — Multiple Choice Questions (MCQ)', qs: mcq },
        { label: 'Section B — Multiple Select Questions (MSQ)', qs: msq },
        { label: 'Section C — Numerical Answer Type (NAT)', qs: nat },
      ].filter(s => s.qs.length > 0).map(section => (
        <div key={section.label} style={{ marginBottom: '2rem' }}>
          <div style={{ background: '#1e3a8a', color: '#fff', padding: '0.5rem 1rem', fontWeight: 700, fontSize: '0.9rem', borderRadius: 4, marginBottom: '1rem' }}>
            {section.label}
          </div>
          {section.qs.map(q => (
            <div key={q.id} style={{ marginBottom: '1.5rem', breakInside: 'avoid' }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', marginBottom: '0.4rem' }}>
                <span style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 28, height: 28, background: '#1e3a8a', color: '#fff', borderRadius: '50%', fontSize: '0.75rem', fontWeight: 800, flexShrink: 0 }}>
                  {q.sequenceNo}
                </span>
                <div style={{ flex: 1, fontSize: '10.5pt' }}>{q.questionText}</div>
                <span style={{ fontSize: '0.7rem', color: '#64748b', border: '1px solid #cbd5e1', borderRadius: 3, padding: '0.1rem 0.5rem', whiteSpace: 'nowrap' }}>
                  {q.type} | {q.marks} M
                </span>
              </div>
              {q.imagePath && <img src={q.imagePath} alt="" style={{ maxWidth: 280, margin: '0.5rem 0 0.5rem 2.2rem', border: '1px solid #e2e8f0', borderRadius: 4 }} />}
              {q.type !== 'NAT' && q.options?.length > 0 && (
                <div style={{ marginLeft: '2.2rem' }}>
                  {q.options.map(opt => (
                    <div key={opt.id} style={{ display: 'flex', alignItems: 'flex-start', gap: '0.6rem', marginBottom: '0.3rem' }}>
                      <div style={{ width: 22, height: 22, border: '1.5px solid #94a3b8', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.7rem', fontWeight: 700, flexShrink: 0, color: '#334155' }}>
                        {opt.optionLabel}
                      </div>
                      <div style={{ fontSize: '10pt', flex: 1 }}>{opt.optionText}</div>
                      {opt.imagePath && <img src={opt.imagePath} alt="" style={{ maxHeight: 36, borderRadius: 3 }} />}
                    </div>
                  ))}
                </div>
              )}
              {q.type === 'NAT' && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginTop: '0.5rem', marginLeft: '2.2rem' }}>
                  <span style={{ fontSize: '0.85rem', color: '#475569' }}>Enter your answer:</span>
                  <div style={{ width: 160, height: 28, border: '1.5px solid #334155', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.8rem', color: '#94a3b8' }}>_ _ _ _ _</div>
                </div>
              )}
            </div>
          ))}
        </div>
      ))}

      <div style={{ textAlign: 'center', fontSize: '0.7rem', color: '#94a3b8', borderTop: '1px solid #e2e8f0', paddingTop: '0.75rem', marginTop: '2rem' }}>
        Generated by GATE MockAI &nbsp;|&nbsp; {test.title} &nbsp;|&nbsp; For examination use only.
      </div>

      <style>{`
        @media print {
          .no-print { display: none !important; }
          @page { margin: 1.5cm; }
        }
      `}</style>
    </div>
  )
}
