import React from 'react'
import { useExamStore } from '../store/examStore'

export default function Legend() {
  const { questionStates } = useExamStore()

  const counts = {
    NOT_VISITED: 0,
    NOT_ANSWERED: 0,
    ANSWERED: 0,
    MARKED: 0,
    MARKED_ANSWERED: 0
  }

  questionStates.forEach(status => {
    if (counts[status] !== undefined) {
      counts[status]++
    }
  })

  return (
    <div className="bg-white border-t border-b border-gray-300 p-2.5 select-none font-sans text-xs">
      <div className="grid grid-cols-2 gap-x-2 gap-y-3 items-center">
        {/* Answered */}
        <div className="flex items-center gap-2">
          <svg width="34" height="34" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
            <defs>
              <linearGradient id="greenGradLegend" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#4caf50" />
                <stop offset="100%" stopColor="#2e7d32" />
              </linearGradient>
            </defs>
            <path d="M 18,2 L 34,14 L 34,34 L 2,34 L 2,14 Z" fill="url(#greenGradLegend)" stroke="#1b5e20" strokeWidth="1.5" strokeLinejoin="round" />
            <text x="50%" y="60%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold" fontFamily="sans-serif">
              {counts.ANSWERED}
            </text>
          </svg>
          <span className="text-[11px] text-gray-700 leading-tight font-medium">Answered</span>
        </div>

        {/* Not Answered */}
        <div className="flex items-center gap-2">
          <svg width="34" height="34" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
            <defs>
              <linearGradient id="orangeGradLegend" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#f05a28" />
                <stop offset="100%" stopColor="#d93c0b" />
              </linearGradient>
            </defs>
            <path d="M 2,2 L 34,2 L 34,22 L 18,34 L 2,22 Z" fill="url(#orangeGradLegend)" stroke="#b22c00" strokeWidth="1.5" strokeLinejoin="round" />
            <text x="50%" y="54%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold" fontFamily="sans-serif">
              {counts.NOT_ANSWERED}
            </text>
          </svg>
          <span className="text-[11px] text-gray-700 leading-tight font-medium">Not Answered</span>
        </div>

        {/* Not Visited */}
        <div className="flex items-center gap-2">
          <div className="w-[34px] h-[34px] border border-[#b6b6b6] bg-gradient-to-b from-[#ffffff] to-[#e6e6e6] text-[#000] font-sans font-bold text-sm flex items-center justify-center rounded-[4px] shadow-[inset_0_1px_0_#fff,0_1px_2px_rgba(0,0,0,0.1)] shrink-0">
            {counts.NOT_VISITED}
          </div>
          <span className="text-[11px] text-gray-700 leading-tight font-medium">Not Visited</span>
        </div>

        {/* Marked for Review */}
        <div className="flex items-center gap-2">
          <svg width="34" height="34" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
            <defs>
              <linearGradient id="purpleGradLegend" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#8e44ad" />
                <stop offset="100%" stopColor="#682a8a" />
              </linearGradient>
            </defs>
            <circle cx="18" cy="18" r="16" fill="url(#purpleGradLegend)" stroke="#4a1565" strokeWidth="1.5" />
            <text x="50%" y="53%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold" fontFamily="sans-serif">
              {counts.MARKED}
            </text>
          </svg>
          <span className="text-[11px] text-gray-700 leading-tight font-medium">Marked for Review</span>
        </div>
      </div>

      {/* Answered & Marked for Review (Full Width) */}
      <div className="flex items-center gap-2 mt-3 pl-0.5">
        <svg width="34" height="34" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
          <defs>
            <linearGradient id="purpleGradLegend2" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#8e44ad" />
              <stop offset="100%" stopColor="#682a8a" />
            </linearGradient>
            <linearGradient id="badgeGradLegend" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#4caf50" />
              <stop offset="100%" stopColor="#2e7d32" />
            </linearGradient>
          </defs>
          <circle cx="18" cy="18" r="16" fill="url(#purpleGradLegend2)" stroke="#4a1565" strokeWidth="1.5" />
          <text x="18" y="19" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold" fontFamily="sans-serif">
            {counts.MARKED_ANSWERED}
          </text>
          <circle cx="28" cy="28" r="6.5" fill="url(#badgeGradLegend)" stroke="#fff" strokeWidth="1" />
          <path d="M 25.5,28 L 27,29.5 L 30.5,26" stroke="white" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" fill="none" />
        </svg>
        <span className="text-[10px] text-gray-700 leading-snug font-medium">
          Answered & Marked for Review (will also be evaluated)
        </span>
      </div>
    </div>
  )
}

