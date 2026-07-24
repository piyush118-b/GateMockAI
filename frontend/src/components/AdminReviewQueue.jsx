import React, { useState, useEffect, useCallback, useRef } from 'react';
import ConfidenceBadge from './ConfidenceBadge';

const AdminReviewQueue = () => {
  const [items, setItems]       = useState([]);
  const [total, setTotal]       = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage]         = useState(0);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState(null);
  const [busy, setBusy]         = useState(null);
  const [modal, setModal]       = useState(null);
  const [answer, setAnswer]     = useState('');
  const [toasts, setToasts]     = useState([]);
  const toastId = useRef(0);

  const showToast = useCallback((msg, type = 'success') => {
    const id = ++toastId.current;
    setToasts(t => [...t, { id, msg, type }]);
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 3500);
  }, []);

  const load = useCallback(async (p = 0) => {
    setLoading(true); setError(null);
    try {
      const res = await fetch(`/api/admin/review/queue?page=${p}&size=15`);
      if (!res.ok) throw new Error(`Server error ${res.status}`);
      const d = await res.json();
      setItems(d.content || []); setTotal(d.totalElements || 0);
      setTotalPages(d.totalPages || 0); setPage(d.currentPage || 0);
    } catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(0); }, [load]);

  const approve = async (item) => {
    setBusy(item.questionId);
    try {
      const r = await fetch(`/api/admin/review/${item.questionId}/approve`, { method: 'POST' });
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      showToast(`✅ Published: ${item.questionId.split('_').slice(-1)[0]}`);
      load(page);
    } catch (e) { showToast(`Failed: ${e.message}`, 'error'); }
    finally { setBusy(null); }
  };

  const correct = async () => {
    if (!modal || !answer.trim()) return;
    setBusy(modal.questionId);
    try {
      const r = await fetch(`/api/admin/review/${modal.questionId}/correct`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ correctAnswer: answer.trim() }),
      });
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      showToast(`✏️ Corrected & published`);
      setModal(null); setAnswer(''); load(page);
    } catch (e) { showToast(`Failed: ${e.message}`, 'error'); }
    finally { setBusy(null); }
  };

  const confColor = (s) => s == null ? '#475569' : s >= 0.7 ? '#f59e0b' : '#ef4444';
  const confPct   = (s) => s != null ? Math.round(s * 100) : 0;

  return (
    <div style={S.page}>
      {/* Ambient background */}
      <div style={S.ambient1} />
      <div style={S.ambient2} />

      {/* Navbar */}
      <nav style={{
        position: 'fixed', top: 0, left: 0, right: 0, zIndex: 100,
        background: 'rgba(2,8,23,0.95)', backdropFilter: 'blur(12px)',
        borderBottom: '1px solid rgba(51,65,85,0.5)',
        height: 64, display: 'flex', alignItems: 'center',
        justifyContent: 'space-between', padding: '0 48px',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
          <a href="/admin/dashboard" style={{
            display: 'flex', alignItems: 'center', gap: 8,
            color: '#64748b', textDecoration: 'none', fontSize: '0.82rem', fontWeight: 600,
            transition: 'color 0.15s',
          }}
            onMouseEnter={e => e.currentTarget.style.color = '#94a3b8'}
            onMouseLeave={e => e.currentTarget.style.color = '#64748b'}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><polyline points="15 18 9 12 15 6"/></svg>
            Dashboard
          </a>
          <div style={{ width: 1, height: 20, background: 'rgba(51,65,85,0.5)' }} />
          <span style={{ fontSize: '0.82rem', fontWeight: 700, color: '#e2e8f0', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Review Queue
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          {total > 0 && (
            <span style={{
              background: 'rgba(245,158,11,0.15)', border: '1px solid rgba(245,158,11,0.3)',
              borderRadius: 9999, padding: '4px 12px',
              fontSize: '0.72rem', fontWeight: 700, color: '#f59e0b',
              textTransform: 'uppercase', letterSpacing: '0.05em',
            }}>
              {total} Pending
            </span>
          )}
          <a href="/logout" style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textDecoration: 'none', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Sign Out
          </a>
        </div>
      </nav>

      {/* Toast stack */}
      <div style={S.toastStack}>
        {toasts.map(t => (
          <div key={t.id} style={{ ...S.toast, background: t.type === 'error' ? '#7f1d1d' : '#14532d', borderColor: t.type === 'error' ? '#ef4444' : '#22c55e' }}>
            {t.msg}
          </div>
        ))}
      </div>

      {/* Header */}
      <div style={{ ...S.header, marginTop: 64 }}>
        <div>
          <div style={S.breadcrumb}>Admin / Review Queue</div>
          <h1 style={S.title}>Answer Review Queue</h1>
          <p style={S.subtitle}>AI-solved questions below confidence threshold · sorted by lowest confidence</p>
        </div>
        <div style={S.statCard}>
          <div style={S.statNum}>{total}</div>
          <div style={S.statLabel}>Pending Review</div>
        </div>
      </div>

      {/* Error */}
      {error && <div style={S.errorBanner}>⚠️ {error}</div>}

      {/* Skeleton / Content */}
      {loading ? (
        <div style={S.grid}>
          {[...Array(5)].map((_, i) => <div key={i} style={{ ...S.skeletonRow, animationDelay: `${i * 0.1}s` }} />)}
        </div>
      ) : items.length === 0 ? (
        <div style={S.empty}>
          <div style={{ fontSize: 56 }}>🎉</div>
          <div style={S.emptyTitle}>All clear!</div>
          <div style={S.emptySub}>No questions pending review. The AI is confident in all answers.</div>
        </div>
      ) : (
        <div style={S.tableWrap}>
          <table style={S.table}>
            <thead>
              <tr>{['Question', 'Type', 'AI Answer', 'Confidence', 'Flagged', 'Actions'].map(h => (
                <th key={h} style={S.th}>{h}</th>
              ))}</tr>
            </thead>
            <tbody>
              {items.map((item, i) => (
                <tr
                  key={item.questionId}
                  style={{ ...S.tr, animationDelay: `${i * 0.04}s` }}
                  onMouseEnter={e => e.currentTarget.style.background = 'rgba(99,102,241,0.06)'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                >
                  <td style={{ ...S.td, maxWidth: 340 }}>
                    <div style={S.qText}>{item.questionText}</div>
                    <div style={S.qMeta}>{item.paperId} · {item.questionId.split('_').slice(-1)[0]}</div>
                  </td>
                  <td style={{ ...S.td, textAlign: 'center' }}>
                    <span style={S.typePill}>{item.questionType || '—'}</span>
                  </td>
                  <td style={S.td}>
                    <code style={S.answerCode}>{item.correctAnswer || '—'}</code>
                  </td>
                  <td style={{ ...S.td, minWidth: 120 }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      <div style={{ ...S.confNum, color: confColor(item.confidenceScore) }}>
                        {confPct(item.confidenceScore)}%
                      </div>
                      <div style={S.confBarBg}>
                        <div style={{ ...S.confBarFill, width: `${confPct(item.confidenceScore)}%`, background: confColor(item.confidenceScore) }} />
                      </div>
                    </div>
                  </td>
                  <td style={{ ...S.td, fontSize: '0.72rem', color: '#64748b', whiteSpace: 'nowrap' }}>
                    {new Date(item.flaggedAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' })}
                  </td>
                  <td style={{ ...S.td, minWidth: 120 }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                      <button
                        style={busy === item.questionId ? S.btnDisabled : S.approveBtn}
                        disabled={busy === item.questionId}
                        onClick={() => approve(item)}
                      >
                        {busy === item.questionId ? '…' : '✅ Approve'}
                      </button>
                      <button
                        style={busy === item.questionId ? S.btnDisabled : S.correctBtn}
                        disabled={busy === item.questionId}
                        onClick={() => { setModal(item); setAnswer(item.correctAnswer || ''); }}
                      >
                        ✏️ Correct
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div style={S.pagination}>
          <button style={page === 0 ? S.pageDisabled : S.pageBtn} disabled={page === 0} onClick={() => load(page - 1)}>← Prev</button>
          <span style={S.pageMeta}>Page {page + 1} of {totalPages}</span>
          <button style={page >= totalPages - 1 ? S.pageDisabled : S.pageBtn} disabled={page >= totalPages - 1} onClick={() => load(page + 1)}>Next →</button>
        </div>
      )}

      {/* Correction Modal */}
      {modal && (
        <div style={S.overlay} onClick={() => { setModal(null); setAnswer(''); }}>
          <div style={S.modalCard} onClick={e => e.stopPropagation()}>
            <div style={S.modalGlow} />
            <div style={S.modalHeader}>
              <h2 style={S.modalTitle}>Override Answer</h2>
              <button style={S.closeBtn} onClick={() => { setModal(null); setAnswer(''); }}>✕</button>
            </div>
            <div style={S.modalMeta}>{modal.questionId} · <span style={{ color: confColor(modal.confidenceScore) }}>{confPct(modal.confidenceScore)}% confidence</span></div>
            <div style={S.modalQText}>{modal.questionText}</div>
            <label style={S.label}>Correct Answer</label>
            <input
              autoFocus
              style={S.input}
              value={answer}
              onChange={e => setAnswer(e.target.value)}
              placeholder="e.g. A  or  B,D  or  42.5 (NAT)"
              onKeyDown={e => e.key === 'Enter' && correct()}
            />
            <div style={S.modalFooter}>
              <button style={S.cancelBtn} onClick={() => { setModal(null); setAnswer(''); }}>Cancel</button>
              <button
                style={!answer.trim() || busy ? S.btnDisabled : S.submitBtn}
                disabled={!answer.trim() || busy}
                onClick={correct}
              >
                {busy ? 'Saving…' : '✏️ Override & Publish'}
              </button>
            </div>
          </div>
        </div>
      )}

      <style>{`
        @keyframes fadeSlideIn {
          from { opacity: 0; transform: translateY(12px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @keyframes pulse {
          0%, 100% { opacity: 0.5; }
          50%       { opacity: 1; }
        }
        @keyframes toastIn {
          from { opacity: 0; transform: translateX(40px); }
          to   { opacity: 1; transform: translateX(0); }
        }
        tr { animation: fadeSlideIn 0.3s both; }
      `}</style>
    </div>
  );
};

const S = {
  page: {
    position: 'relative',
    minHeight: '100vh',
    background: '#020817',
    color: '#e2e8f0',
    padding: '40px 48px',
    fontFamily: "'Inter', system-ui, -apple-system, sans-serif",
    overflow: 'hidden',
  },
  ambient1: {
    position: 'fixed', top: -200, right: -200,
    width: 600, height: 600, borderRadius: '50%',
    background: 'radial-gradient(circle, rgba(99,102,241,0.15) 0%, transparent 70%)',
    pointerEvents: 'none', zIndex: 0,
  },
  ambient2: {
    position: 'fixed', bottom: -200, left: -100,
    width: 500, height: 500, borderRadius: '50%',
    background: 'radial-gradient(circle, rgba(168,85,247,0.1) 0%, transparent 70%)',
    pointerEvents: 'none', zIndex: 0,
  },
  toastStack: {
    position: 'fixed', top: 24, right: 24, zIndex: 9999,
    display: 'flex', flexDirection: 'column', gap: 8,
  },
  toast: {
    padding: '12px 20px', borderRadius: 10,
    border: '1px solid', backdropFilter: 'blur(12px)',
    fontSize: '0.875rem', fontWeight: 600, color: '#f1f5f9',
    boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
    animation: 'toastIn 0.3s ease',
  },
  header: {
    display: 'flex', justifyContent: 'space-between',
    alignItems: 'flex-start', marginBottom: 40, position: 'relative', zIndex: 1,
  },
  breadcrumb: { fontSize: '0.75rem', color: '#475569', marginBottom: 8, letterSpacing: '0.05em' },
  title: { margin: 0, fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.03em',
    background: 'linear-gradient(135deg, #e2e8f0 0%, #94a3b8 100%)',
    WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
  },
  subtitle: { margin: '8px 0 0', color: '#475569', fontSize: '0.875rem' },
  statCard: {
    background: 'rgba(30,41,59,0.8)', border: '1px solid rgba(99,102,241,0.3)',
    borderRadius: 16, padding: '20px 32px', textAlign: 'center',
    backdropFilter: 'blur(12px)',
    boxShadow: '0 0 40px rgba(99,102,241,0.1)',
  },
  statNum: { fontSize: '2.5rem', fontWeight: 900, color: '#f59e0b', lineHeight: 1 },
  statLabel: { fontSize: '0.7rem', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.1em', marginTop: 4 },
  errorBanner: {
    background: 'rgba(127,29,29,0.5)', border: '1px solid #ef4444',
    borderRadius: 10, padding: '12px 16px', color: '#fca5a5',
    marginBottom: 24, position: 'relative', zIndex: 1,
  },
  grid: { display: 'flex', flexDirection: 'column', gap: 8, position: 'relative', zIndex: 1 },
  skeletonRow: {
    height: 64, borderRadius: 10,
    background: 'linear-gradient(90deg, rgba(30,41,59,0.6) 25%, rgba(51,65,85,0.4) 50%, rgba(30,41,59,0.6) 75%)',
    backgroundSize: '200% 100%',
    animation: 'pulse 1.5s ease-in-out infinite',
  },
  empty: {
    textAlign: 'center', padding: '100px 0', position: 'relative', zIndex: 1,
  },
  emptyTitle: { fontSize: '1.5rem', fontWeight: 700, marginTop: 16, color: '#e2e8f0' },
  emptySub: { color: '#475569', marginTop: 8, fontSize: '0.9rem' },
  tableWrap: {
    background: 'rgba(15,23,42,0.8)', border: '1px solid rgba(51,65,85,0.5)',
    borderRadius: 16, overflow: 'hidden', backdropFilter: 'blur(12px)',
    position: 'relative', zIndex: 1,
    boxShadow: '0 4px 60px rgba(0,0,0,0.3)',
  },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: {
    background: 'rgba(2,8,23,0.9)', padding: '14px 16px',
    textAlign: 'left', fontSize: '0.65rem', fontWeight: 700,
    color: '#475569', textTransform: 'uppercase', letterSpacing: '0.08em',
    borderBottom: '1px solid rgba(51,65,85,0.5)',
  },
  tr: {
    borderBottom: '1px solid rgba(30,41,59,0.8)',
    transition: 'background 0.15s',
    cursor: 'default',
  },
  td: { padding: '14px 16px', verticalAlign: 'middle' },
  qText: { fontSize: '0.82rem', color: '#cbd5e1', lineHeight: 1.55, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' },
  qMeta: { fontSize: '0.68rem', color: '#334155', marginTop: 3, fontFamily: 'monospace' },
  typePill: {
    background: 'rgba(51,65,85,0.6)', border: '1px solid rgba(71,85,105,0.5)',
    borderRadius: 6, padding: '3px 8px', fontSize: '0.65rem',
    fontWeight: 700, color: '#94a3b8', letterSpacing: '0.05em',
  },
  answerCode: {
    background: 'rgba(2,8,23,0.8)', border: '1px solid rgba(99,102,241,0.3)',
    borderRadius: 6, padding: '3px 10px', fontSize: '0.8rem',
    color: '#a5b4fc', fontFamily: 'monospace',
  },
  confNum: { fontSize: '1.1rem', fontWeight: 800, lineHeight: 1 },
  confBarBg: { height: 4, background: 'rgba(51,65,85,0.5)', borderRadius: 4 },
  confBarFill: { height: '100%', borderRadius: 4, transition: 'width 0.6s ease' },
  approveBtn: {
    background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.3)',
    borderRadius: 8, color: '#4ade80', padding: '6px 12px',
    fontSize: '0.72rem', fontWeight: 700, cursor: 'pointer',
    transition: 'all 0.15s', whiteSpace: 'nowrap',
  },
  correctBtn: {
    background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.3)',
    borderRadius: 8, color: '#a5b4fc', padding: '6px 12px',
    fontSize: '0.72rem', fontWeight: 700, cursor: 'pointer',
    transition: 'all 0.15s', whiteSpace: 'nowrap',
  },
  btnDisabled: {
    background: 'rgba(30,41,59,0.4)', border: '1px solid rgba(51,65,85,0.3)',
    borderRadius: 8, color: '#334155', padding: '6px 12px',
    fontSize: '0.72rem', fontWeight: 700, cursor: 'not-allowed', whiteSpace: 'nowrap',
  },
  pagination: {
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    gap: 16, marginTop: 28, position: 'relative', zIndex: 1,
  },
  pageBtn: {
    background: 'rgba(30,41,59,0.8)', border: '1px solid rgba(51,65,85,0.5)',
    borderRadius: 8, color: '#94a3b8', padding: '8px 20px',
    fontSize: '0.82rem', cursor: 'pointer',
  },
  pageDisabled: {
    background: 'rgba(15,23,42,0.4)', border: '1px solid rgba(30,41,59,0.5)',
    borderRadius: 8, color: '#334155', padding: '8px 20px',
    fontSize: '0.82rem', cursor: 'not-allowed',
  },
  pageMeta: { color: '#475569', fontSize: '0.82rem' },
  overlay: {
    position: 'fixed', inset: 0, background: 'rgba(2,8,23,0.85)',
    backdropFilter: 'blur(8px)', display: 'flex',
    alignItems: 'center', justifyContent: 'center', zIndex: 9000,
  },
  modalCard: {
    position: 'relative',
    background: 'linear-gradient(135deg, rgba(15,23,42,0.95) 0%, rgba(30,41,59,0.95) 100%)',
    border: '1px solid rgba(99,102,241,0.3)', borderRadius: 20,
    padding: 32, width: '90%', maxWidth: 560,
    boxShadow: '0 0 80px rgba(99,102,241,0.2), 0 24px 80px rgba(0,0,0,0.5)',
    backdropFilter: 'blur(20px)', overflow: 'hidden',
    animation: 'fadeSlideIn 0.25s ease',
  },
  modalGlow: {
    position: 'absolute', top: -60, right: -60,
    width: 200, height: 200, borderRadius: '50%',
    background: 'radial-gradient(circle, rgba(99,102,241,0.15) 0%, transparent 70%)',
    pointerEvents: 'none',
  },
  modalHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 },
  modalTitle: { margin: 0, fontSize: '1.2rem', fontWeight: 700, color: '#e2e8f0' },
  closeBtn: {
    background: 'none', border: 'none', color: '#475569',
    fontSize: '1.1rem', cursor: 'pointer', padding: 4,
  },
  modalMeta: { fontSize: '0.75rem', color: '#475569', marginBottom: 16, fontFamily: 'monospace' },
  modalQText: {
    background: 'rgba(2,8,23,0.6)', border: '1px solid rgba(30,41,59,0.8)',
    borderRadius: 10, padding: 14, fontSize: '0.82rem', color: '#94a3b8',
    lineHeight: 1.6, marginBottom: 20, maxHeight: 140, overflowY: 'auto',
  },
  label: { display: 'block', fontSize: '0.75rem', color: '#64748b', fontWeight: 600, marginBottom: 8, letterSpacing: '0.05em', textTransform: 'uppercase' },
  input: {
    width: '100%', background: 'rgba(2,8,23,0.8)',
    border: '1px solid rgba(99,102,241,0.4)', borderRadius: 10,
    color: '#e2e8f0', padding: '12px 14px', fontSize: '0.9rem',
    outline: 'none', boxSizing: 'border-box', fontFamily: 'monospace',
    transition: 'border-color 0.2s',
  },
  modalFooter: { display: 'flex', gap: 10, marginTop: 20, justifyContent: 'flex-end' },
  cancelBtn: {
    background: 'transparent', border: '1px solid rgba(51,65,85,0.7)',
    borderRadius: 8, color: '#64748b', padding: '10px 20px',
    cursor: 'pointer', fontSize: '0.85rem',
  },
  submitBtn: {
    background: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
    border: 'none', borderRadius: 8, color: '#fff',
    padding: '10px 24px', cursor: 'pointer', fontSize: '0.85rem',
    fontWeight: 700, boxShadow: '0 4px 20px rgba(99,102,241,0.4)',
  },
};

export default AdminReviewQueue;
