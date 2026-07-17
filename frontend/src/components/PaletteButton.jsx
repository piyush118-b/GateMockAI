import React from 'react'

export default function PaletteButton({ index, status, isActive, onClick }) {
  const activeClass = isActive 
    ? 'ring-2 ring-amber-500 ring-offset-1 scale-105 z-10' 
    : 'hover:opacity-90 hover:scale-102';

  const renderShape = () => {
    switch (status) {
      case 'NOT_ANSWERED':
        return (
          <svg width="34" height="34" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
            <defs>
              <linearGradient id={`orangeGrad-${index}`} x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#f05a28" />
                <stop offset="100%" stopColor="#d93c0b" />
              </linearGradient>
            </defs>
            <path d="M 2,2 L 34,2 L 34,22 L 18,34 L 2,22 Z" fill={`url(#orangeGrad-${index})`} stroke="#b22c00" strokeWidth="1.5" strokeLinejoin="round" />
            <text x="50%" y="54%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold" fontFamily="sans-serif">
              {index}
            </text>
          </svg>
        )
      case 'ANSWERED':
        return (
          <svg width="34" height="34" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
            <defs>
              <linearGradient id={`greenGrad-${index}`} x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#4caf50" />
                <stop offset="100%" stopColor="#2e7d32" />
              </linearGradient>
            </defs>
            <path d="M 18,2 L 34,14 L 34,34 L 2,34 L 2,14 Z" fill={`url(#greenGrad-${index})`} stroke="#1b5e20" strokeWidth="1.5" strokeLinejoin="round" />
            <text x="50%" y="60%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold" fontFamily="sans-serif">
              {index}
            </text>
          </svg>
        )
      case 'MARKED':
        return (
          <svg width="34" height="34" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
            <defs>
              <linearGradient id={`purpleGrad-${index}`} x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#8e44ad" />
                <stop offset="100%" stopColor="#682a8a" />
              </linearGradient>
            </defs>
            <circle cx="18" cy="18" r="16" fill={`url(#purpleGrad-${index})`} stroke="#4a1565" strokeWidth="1.5" />
            <text x="50%" y="53%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold" fontFamily="sans-serif">
              {index}
            </text>
          </svg>
        )
      case 'MARKED_ANSWERED':
        return (
          <svg width="34" height="34" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
            <defs>
              <linearGradient id={`purpleGrad-${index}-2`} x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#8e44ad" />
                <stop offset="100%" stopColor="#682a8a" />
              </linearGradient>
              <linearGradient id={`badgeGrad-${index}`} x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#4caf50" />
                <stop offset="100%" stopColor="#2e7d32" />
              </linearGradient>
            </defs>
            <circle cx="18" cy="18" r="16" fill={`url(#purpleGrad-${index}-2)`} stroke="#4a1565" strokeWidth="1.5" />
            <text x="18" y="19" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold" fontFamily="sans-serif">
              {index}
            </text>
            <circle cx="28" cy="28" r="6.5" fill={`url(#badgeGrad-${index})`} stroke="#fff" strokeWidth="1" />
            <path d="M 25.5,28 L 27,29.5 L 30.5,26" stroke="white" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" fill="none" />
          </svg>
        )
      case 'NOT_VISITED':
      default:
        return (
          <div className="w-[34px] h-[34px] border border-[#b6b6b6] bg-gradient-to-b from-[#ffffff] to-[#e6e6e6] text-[#000] font-sans font-bold text-sm flex items-center justify-center rounded-[4px] shadow-[inset_0_1px_0_#fff,0_1px_2px_rgba(0,0,0,0.1)] shrink-0">
            {index}
          </div>
        )
    }
  }

  return (
    <button
      type="button"
      onClick={onClick}
      className={`w-10 h-10 flex items-center justify-center cursor-pointer bg-transparent border-none p-0 outline-none transition-all duration-100 ${activeClass}`}
    >
      {renderShape()}
    </button>
  )
}

