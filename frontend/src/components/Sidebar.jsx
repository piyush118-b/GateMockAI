import React from 'react'
import { useExamStore } from '../store/examStore'
import PaletteButton from './PaletteButton'
import Legend from './Legend'

export default function Sidebar() {
  const { 
    attempt, 
    questions, 
    activeQuestionIndex, 
    questionStates, 
    jumpToQuestion,
    activeSection
  } = useExamStore()

  const candidateName = attempt?.user?.fullName || 'John Smith'

  // Dynamic section title based on activeSection index
  const sectionTitle = activeSection === 0 
    ? 'General Aptitude' 
    : 'CS 1 Computer Science and Information Technology Mock'

  const handleSubmitTrigger = () => {
    useExamStore.setState({ isSubmitModalActive: true });
  }

  // Filter questions that belong to the active section:
  // indices 0-9 (first 10) for General Aptitude (activeSection === 0)
  // indices 10+ for CS Subject section (activeSection === 1)
  const filteredQuestions = questions.map((q, idx) => ({ q, idx })).filter(item => {
    const qSection = (item.idx >= 10) ? 1 : 0
    return qSection === activeSection
  })

  return (
    <aside className="w-[17%] min-w-[220px] bg-[#e0ecf8] flex flex-col h-full font-sans select-none shrink-0 overflow-hidden relative border-l border-gray-400 justify-between">
      
      {/* Top scrollable candidate info and palette */}
      <div className="flex-1 flex flex-col overflow-y-auto min-h-0">
        
        {/* CANDIDATE BRIEF SUMMARY PROFILE */}
        <div className="flex flex-col bg-white border-b border-gray-200 shrink-0">
          <div className="flex gap-2.5 p-2 h-[84px] items-center">
            {/* Custom SVG Candidate Avatar */}
            <div className="w-[60px] h-[72px] border border-gray-300 bg-white flex items-center justify-center shrink-0 overflow-hidden shadow-sm">
              <svg viewBox="0 0 100 120" className="w-full h-full bg-gray-200">
                <rect width="100" height="120" fill="#f0f4f8" />
                <circle cx="50" cy="42" r="22" fill="#5b86b6" />
                <path d="M 50,68 C 28,68 16,84 16,102 L 84,102 C 84,84 72,68 50,68 Z" fill="#3a5e8c" />
                <path d="M 50,68 L 44,78 L 50,92 L 56,78 Z" fill="#e0ecf8" />
              </svg>
            </div>
            
            <div className="flex flex-col justify-start">
              <span className="text-[10px] text-gray-500 font-bold uppercase tracking-wider">Candidate</span>
              <span className="text-[14px] font-extrabold text-[#0f2c59] leading-tight">
                {candidateName}
              </span>
            </div>
          </div>
        </div>

        {/* STATUS LEGENDS */}
        <Legend />

        {/* SECTION TITLE ROW */}
        <div className="bg-[#337ab7] text-white font-bold text-[12px] px-3 py-2 shrink-0 border-t border-b border-[#2e6da4] tracking-wide select-none">
          {sectionTitle}
        </div>

        {/* QUESTION PALETTE GRID AREA */}
        <div className="flex-1 overflow-y-auto bg-[#e0ecf8] p-3">
          <span className="text-[11px] font-bold text-gray-700 mb-2 block uppercase tracking-wide">Choose a Question</span>

          {/* 4 Column Question Grid */}
          <div className="grid grid-cols-4 gap-2">
            {filteredQuestions.map(({ q, idx }, localIdx) => (
              <PaletteButton
                key={q.id}
                index={localIdx + 1}
                status={questionStates[idx] || 'NOT_VISITED'}
                isActive={activeQuestionIndex === idx}
                onClick={() => jumpToQuestion(idx)}
              />
            ))}
          </div>
        </div>
      </div>

      {/* BOTTOM SIDEBAR FOOTER: Submit Button */}
      <div className="bg-[#d9edf7] border-t border-[#bce8f1] h-[52px] flex items-center justify-center shrink-0 w-full">
        <button
          type="button"
          onClick={handleSubmitTrigger}
          className="bg-[#5bc0de] border border-[#46b8da] hover:bg-[#31b0d5] text-white font-bold py-1.5 px-10 rounded-sm shadow-sm cursor-pointer text-xs transition-all uppercase tracking-wider outline-none"
        >
          Submit
        </button>
      </div>

    </aside>
  )
}

