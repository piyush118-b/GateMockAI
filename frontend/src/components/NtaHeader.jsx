import React from 'react'
import { useExamStore } from '../store/examStore'

export default function NtaHeader({ title = "CS 1 Computer Science and Information Technology Mock" }) {
  const { attempt } = useExamStore()

  return (
    <div className="flex flex-col w-full select-none font-sans">
      {/* BAND 1: White Header */}
      <div className="flex justify-between items-center px-4 py-2 bg-white border-b-4 border-[#1b3e6f] h-[72px]">
        <div className="flex items-center gap-4">
          <div className="w-[50px] h-[50px] bg-white flex items-center justify-center border border-gray-200 p-0.5 shadow-sm">
            {/* Inline SVG GATE Logo */}
            <svg viewBox="0 0 100 100" className="max-h-full max-w-full object-contain select-none">
              <circle cx="50" cy="50" r="46" fill="#ffffff" stroke="#1b3e6f" strokeWidth="4" />
              <circle cx="50" cy="50" r="38" fill="none" stroke="#f58220" strokeWidth="3" strokeDasharray="6,3" />
              <circle cx="50" cy="50" r="28" fill="#1b3e6f" />
              <path d="M 38,48 L 50,55 L 62,48 L 50,42 Z" fill="#ffffff" />
              <path d="M 50,55 L 50,66" stroke="#ffffff" strokeWidth="2.5" />
              <path d="M 38,48 L 38,58 L 50,65 L 62,58 L 62,48" fill="none" stroke="#ffffff" strokeWidth="2" />
              <text x="50%" y="82%" dominantBaseline="middle" textAnchor="middle" fill="#1b3e6f" fontSize="11" fontWeight="bold" fontFamily="sans-serif">GATE</text>
              <text x="50%" y="16%" dominantBaseline="middle" textAnchor="middle" fill="#1b3e6f" fontSize="10" fontWeight="bold" fontFamily="sans-serif">2026</text>
            </svg>
          </div>
        </div>
        
        <div className="flex flex-col items-center flex-1 text-center justify-center">
          <h1 className="text-[#7b1fa2] text-[18px] font-bold uppercase tracking-wide leading-tight">
            Graduate Aptitude Test in Engineering (GATE 2026)
          </h1>
          <h2 className="text-[#009688] text-[13px] font-bold tracking-wide mt-0.5">
            Organizing Institute : INDIAN INSTITUTE OF TECHNOLOGY GUWAHATI
          </h2>
        </div>

        <div className="flex items-center">
          <div className="w-[50px] h-[50px] bg-white flex items-center justify-center border border-gray-200 p-0.5 rounded-full overflow-hidden shadow-sm">
            {/* Inline SVG IITG Logo */}
            <svg viewBox="0 0 100 100" className="max-h-full max-w-full object-contain select-none">
              <circle cx="50" cy="50" r="46" fill="#ffffff" stroke="#009688" strokeWidth="3" />
              <circle cx="50" cy="50" r="40" fill="none" stroke="#1b3e6f" strokeWidth="1" />
              <path d="M 32,45 C 32,32 50,22 50,22 C 50,22 68,32 68,45 C 68,62 50,75 50,75 C 50,75 32,62 32,45 Z" fill="#009688" opacity="0.85" />
              <path d="M 36,55 Q 43,50 50,55 Q 57,60 64,55" fill="none" stroke="#ffffff" strokeWidth="3" strokeLinecap="round" />
              <path d="M 38,62 Q 44,58 50,62 Q 56,66 62,62" fill="none" stroke="#ffffff" strokeWidth="2.5" strokeLinecap="round" />
              <circle cx="50" cy="36" r="5" fill="#ffeb3b" />
              <text x="50%" y="90%" dominantBaseline="middle" textAnchor="middle" fill="#009688" fontSize="8" fontWeight="bold" fontFamily="sans-serif">IITG</text>
            </svg>
          </div>
        </div>
      </div>

      {/* BAND 2: Sub-header */}
      <div className="bg-[#1f3d5a] flex justify-between items-center px-4 py-1.5 h-9 shrink-0">
        <div className="text-white text-[13px] font-bold font-sans">
          {title}
        </div>
        <div className="flex gap-4 items-center mr-2">
          <button
            type="button"
            onClick={() => useExamStore.setState({ isInstructionsVisible: true })}
            className="flex items-center gap-1.5 text-white hover:text-gray-200 text-xs font-semibold cursor-pointer outline-none border-none bg-transparent"
          >
            <span className="w-[18px] h-[18px] rounded-full bg-[#3498db] text-white flex items-center justify-center text-[10px] font-black italic shadow-sm">i</span>
            Instructions
          </button>
          <button
            type="button"
            onClick={() => useExamStore.setState({ isQuestionPaperVisible: true })}
            className="flex items-center gap-1.5 text-white hover:text-gray-200 text-xs font-semibold cursor-pointer outline-none border-none bg-transparent"
          >
            <span className="w-[18px] h-[18px] rounded bg-[#2ecc71] text-white flex items-center justify-center text-[10px] font-black shadow-sm">📑</span>
            Question Paper
          </button>
        </div>
      </div>
    </div>
  )
}

