import React from 'react';

/**
 * Premium confidence badge component.
 * Shows answer source and AI confidence for each GATE question.
 *
 * Props:
 *   answerSource   'HUMAN_VERIFIED' | 'LLM_DERIVED'
 *   reviewStatus   'PUBLISHED' | 'NEEDS_REVIEW'
 *   confidenceScore  number 0.0–1.0 (optional)
 *   adminView        boolean — if false, NEEDS_REVIEW is hidden
 */
const ConfidenceBadge = ({ answerSource, reviewStatus, confidenceScore, adminView = false }) => {
  if (reviewStatus === 'NEEDS_REVIEW' && !adminView) return null;

  const pct = confidenceScore != null ? Math.round(confidenceScore * 100) : null;

  if (reviewStatus === 'NEEDS_REVIEW') {
    return (
      <span style={styles.needsReview} title="Low AI confidence — pending admin review">
        <span style={styles.dot('#ef4444')} />
        Needs Review
      </span>
    );
  }

  if (answerSource === 'HUMAN_VERIFIED') {
    return (
      <span style={styles.verified} title="Answer verified by human admin">
        <span style={styles.dot('#22c55e')} />
        Verified
      </span>
    );
  }

  if (answerSource === 'LLM_DERIVED' && pct != null) {
    const conf = pct >= 90 ? 'high' : pct >= 75 ? 'mid' : 'low';
    const colors = { high: '#22c55e', mid: '#f59e0b', low: '#ef4444' };
    const c = colors[conf];
    return (
      <span
        style={{ ...styles.aiSolved, borderColor: `${c}55`, color: c }}
        title={`AI-solved · ${pct}% confidence`}
      >
        <span style={styles.dot(c)} />
        AI-Solved · {pct}%
      </span>
    );
  }

  return null;
};

const base = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 5,
  padding: '3px 10px 3px 6px',
  borderRadius: 999,
  fontSize: '0.7rem',
  fontWeight: 700,
  border: '1px solid',
  letterSpacing: '0.03em',
  whiteSpace: 'nowrap',
  backdropFilter: 'blur(4px)',
};

const styles = {
  verified:   { ...base, color: '#22c55e', borderColor: '#22c55e44', background: '#22c55e0d' },
  aiSolved:   { ...base, background: '#f59e0b0d' },
  needsReview:{ ...base, color: '#ef4444', borderColor: '#ef444444', background: '#ef44440d' },
  dot: (color) => ({
    width: 6, height: 6,
    borderRadius: '50%',
    background: color,
    flexShrink: 0,
    boxShadow: `0 0 4px ${color}`,
  }),
};

export default ConfidenceBadge;
