import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell
} from 'recharts'

// ─── Colour palette ──────────────────────────────────────────────────────────
const COLORS = ['#6366f1', '#22d3ee', '#f59e0b', '#10b981', '#f43f5e', '#a78bfa']

// ─── Small metric card ────────────────────────────────────────────────────────
function MetricCard({ label, value, sub, color = '#6366f1' }) {
  return (
    <div style={{
      background: 'linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%)',
      border: `1px solid ${color}33`,
      borderRadius: 16,
      padding: '20px 24px',
      flex: '1 1 180px',
      minWidth: 160,
    }}>
      <p style={{ margin: 0, fontSize: 12, color: '#94a3b8', letterSpacing: '0.06em', textTransform: 'uppercase' }}>
        {label}
      </p>
      <p style={{ margin: '8px 0 4px', fontSize: 32, fontWeight: 700, color, lineHeight: 1 }}>
        {value}
      </p>
      {sub && <p style={{ margin: 0, fontSize: 12, color: '#64748b' }}>{sub}</p>}
    </div>
  )
}

// ─── Collapsible weak-question row ───────────────────────────────────────────
function WeakQuestion({ wq, index }) {
  const [open, setOpen] = useState(false)
  return (
    <div style={{
      background: '#0f172a',
      border: '1px solid #1e293b',
      borderRadius: 12,
      marginBottom: 10,
      overflow: 'hidden',
      transition: 'border-color 0.2s',
    }}>
      <button
        onClick={() => setOpen(o => !o)}
        style={{
          width: '100%', display: 'flex', alignItems: 'center', gap: 12,
          background: 'none', border: 'none', cursor: 'pointer',
          padding: '14px 18px', textAlign: 'left',
        }}
      >
        {/* Sequence badge */}
        <span style={{
          background: '#f43f5e22', color: '#f43f5e', borderRadius: 8,
          padding: '2px 10px', fontSize: 12, fontWeight: 700, flexShrink: 0,
        }}>Q{wq.sequenceNo}</span>

        {/* Question preview */}
        <span style={{ flex: 1, color: '#e2e8f0', fontSize: 14, lineHeight: 1.5 }}>
          {wq.questionText?.length > 120
            ? wq.questionText.slice(0, 120) + '…'
            : wq.questionText}
        </span>

        {/* Marks badge */}
        <span style={{
          background: '#dc262620', color: '#ef4444', borderRadius: 6,
          padding: '2px 8px', fontSize: 12, fontWeight: 600, flexShrink: 0,
        }}>{wq.marks}M</span>

        {/* Type badge */}
        <span style={{
          background: '#1e293b', color: '#94a3b8', borderRadius: 6,
          padding: '2px 8px', fontSize: 11, flexShrink: 0,
        }}>{wq.type}</span>

        {/* Chevron */}
        <span style={{ color: '#475569', fontSize: 18, transition: 'transform 0.2s',
          transform: open ? 'rotate(180deg)' : 'rotate(0deg)' }}>▾</span>
      </button>

      {open && (
        <div style={{
          padding: '0 18px 18px',
          borderTop: '1px solid #1e293b',
        }}>
          {/* Full question */}
          <p style={{ color: '#cbd5e1', fontSize: 14, lineHeight: 1.7, marginTop: 14 }}>
            <strong style={{ color: '#94a3b8' }}>Question: </strong>{wq.questionText}
          </p>
          {/* Explanation */}
          {wq.explanation ? (
            <div style={{
              background: '#0d1526', borderLeft: '3px solid #6366f1',
              borderRadius: '0 8px 8px 0', padding: '10px 14px', marginTop: 10,
            }}>
              <p style={{ margin: 0, fontSize: 13, color: '#a5b4fc', lineHeight: 1.7 }}>
                <strong style={{ color: '#818cf8' }}>Explanation: </strong>{wq.explanation}
              </p>
            </div>
          ) : (
            <p style={{ color: '#475569', fontSize: 13, fontStyle: 'italic', marginTop: 10 }}>
              No explanation available for this question.
            </p>
          )}
        </div>
      )}
    </div>
  )
}

// ─── Custom bar chart tooltip ─────────────────────────────────────────────────
function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return (
    <div style={{
      background: '#1e293b', border: '1px solid #334155',
      borderRadius: 10, padding: '10px 14px', fontSize: 13,
    }}>
      <p style={{ margin: '0 0 4px', color: '#94a3b8', fontWeight: 600 }}>{label}</p>
      <p style={{ margin: 0, color: '#6366f1' }}>Marks: <strong>{payload[0].value}</strong></p>
    </div>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Main component
// ─────────────────────────────────────────────────────────────────────────────
export default function AttemptAnalytics() {
  const { attemptId } = useParams()
  const navigate = useNavigate()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!attemptId) return
    fetch(`/api/student/attempts/${attemptId}/analytics`, { credentials: 'include' })
      .then(r => {
        if (!r.ok) throw new Error(`Server responded ${r.status}`)
        return r.json()
      })
      .then(setData)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [attemptId])

  // ── Loading / error states ─────────────────────────────────────────────────
  if (loading) return (
    <div style={pageStyle}>
      <div style={{ textAlign: 'center', paddingTop: 120 }}>
        <div style={spinnerStyle} />
        <p style={{ color: '#64748b', marginTop: 20 }}>Loading analytics…</p>
      </div>
    </div>
  )

  if (error) return (
    <div style={pageStyle}>
      <div style={{ textAlign: 'center', paddingTop: 120 }}>
        <p style={{ color: '#f43f5e', fontSize: 18 }}>⚠ {error}</p>
        <button onClick={() => navigate(-1)} style={btnStyle}>← Go Back</button>
      </div>
    </div>
  )

  if (!data) return null

  const {
    testTitle, totalMarks, scored, accuracy, timeTakenSeconds,
    questionsAttempted, questionsCorrect, bySubject, byType, weakQuestions,
  } = data

  const minutes = Math.floor(timeTakenSeconds / 60)
  const seconds = timeTakenSeconds % 60
  const timeStr = `${minutes}m ${seconds}s`

  // Determine score colour
  const scoreRatio = totalMarks > 0 ? (Number(scored) / Number(totalMarks)) : 0
  const scoreColor = scoreRatio >= 0.6 ? '#10b981' : scoreRatio >= 0.4 ? '#f59e0b' : '#f43f5e'

  return (
    <div style={pageStyle}>
      {/* ── Header ─────────────────────────────────────────────────────────── */}
      <div style={{ maxWidth: 900, margin: '0 auto', padding: '40px 20px 0' }}>
        <button onClick={() => navigate(-1)} style={btnStyle}>← Back</button>
        <h1 style={{ color: '#e2e8f0', margin: '20px 0 4px', fontSize: 24, fontWeight: 700 }}>
          Performance Analytics
        </h1>
        <p style={{ color: '#64748b', marginBottom: 32 }}>{testTitle}</p>

        {/* ── Metric cards ──────────────────────────────────────────────── */}
        <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 40 }}>
          <MetricCard
            label="Score"
            value={`${Number(scored).toFixed(1)} / ${Number(totalMarks).toFixed(0)}`}
            sub={`${(scoreRatio * 100).toFixed(1)}% of total`}
            color={scoreColor}
          />
          <MetricCard
            label="Accuracy"
            value={`${accuracy.toFixed(1)}%`}
            sub={`${questionsCorrect} correct of ${questionsAttempted} attempted`}
            color="#22d3ee"
          />
          <MetricCard
            label="Time Taken"
            value={timeStr}
            sub="from start to submit"
            color="#f59e0b"
          />
          <MetricCard
            label="Attempted"
            value={questionsAttempted}
            sub={`out of questions in paper`}
            color="#a78bfa"
          />
        </div>

        {/* ── By-Type breakdown ─────────────────────────────────────────── */}
        <section style={sectionStyle}>
          <h2 style={sectionTitle}>By Question Type</h2>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            {byType.map((t, i) => (
              <div key={t.type} style={{
                background: '#0f172a', border: `1px solid ${COLORS[i % COLORS.length]}44`,
                borderRadius: 12, padding: '14px 20px', flex: '1 1 140px',
              }}>
                <p style={{ margin: '0 0 6px', color: COLORS[i % COLORS.length], fontWeight: 700, fontSize: 16 }}>
                  {t.type}
                </p>
                <p style={{ margin: 0, color: '#94a3b8', fontSize: 13 }}>
                  {t.correct} correct / {t.attempted} attempted
                </p>
              </div>
            ))}
          </div>
        </section>

        {/* ── Marks by subject bar chart ─────────────────────────────────── */}
        {bySubject.length > 0 && (
          <section style={sectionStyle}>
            <h2 style={sectionTitle}>Marks Earned by Subject</h2>
            <div style={{ background: '#0f172a', borderRadius: 16, padding: 20 }}>
              <ResponsiveContainer width="100%" height={240}>
                <BarChart
                  data={bySubject.map(s => ({ name: s.subject, marks: Number(s.marks) }))}
                  margin={{ top: 10, right: 20, left: 0, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis
                    dataKey="name"
                    tick={{ fill: '#94a3b8', fontSize: 12 }}
                    axisLine={{ stroke: '#334155' }}
                    tickLine={false}
                  />
                  <YAxis
                    tick={{ fill: '#94a3b8', fontSize: 12 }}
                    axisLine={{ stroke: '#334155' }}
                    tickLine={false}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Bar dataKey="marks" radius={[6, 6, 0, 0]} maxBarSize={60}>
                    {bySubject.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </section>
        )}

        {/* ── Weak questions list ────────────────────────────────────────── */}
        <section style={{ ...sectionStyle, marginBottom: 60 }}>
          <h2 style={sectionTitle}>
            Questions to Review
            <span style={{ color: '#64748b', fontWeight: 400, fontSize: 14, marginLeft: 10 }}>
              ({weakQuestions.length} wrong — highest value first)
            </span>
          </h2>
          {weakQuestions.length === 0 ? (
            <div style={{
              background: '#0f172a', borderRadius: 16, padding: '32px',
              textAlign: 'center', color: '#10b981', fontSize: 18,
            }}>
              🎉 No wrong answers to review! Perfect score.
            </div>
          ) : (
            weakQuestions.map((wq, i) => <WeakQuestion key={wq.questionId} wq={wq} index={i} />)
          )}
        </section>
      </div>
    </div>
  )
}

// ─── Shared styles ────────────────────────────────────────────────────────────
const pageStyle = {
  minHeight: '100vh',
  background: 'linear-gradient(180deg, #020617 0%, #0a0f1e 100%)',
  fontFamily: "'Inter', system-ui, sans-serif",
}

const sectionStyle = {
  marginBottom: 36,
}

const sectionTitle = {
  color: '#e2e8f0',
  fontSize: 18,
  fontWeight: 700,
  marginBottom: 16,
  marginTop: 0,
}

const btnStyle = {
  background: '#1e293b',
  color: '#94a3b8',
  border: '1px solid #334155',
  borderRadius: 8,
  padding: '8px 16px',
  cursor: 'pointer',
  fontSize: 13,
  fontWeight: 500,
}

const spinnerStyle = {
  width: 40, height: 40,
  border: '3px solid #1e293b',
  borderTop: '3px solid #6366f1',
  borderRadius: '50%',
  animation: 'spin 0.8s linear infinite',
  margin: '0 auto',
}
