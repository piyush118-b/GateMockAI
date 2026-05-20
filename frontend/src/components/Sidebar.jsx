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
    jumpToQuestion 
  } = useExamStore()

  const candidateName = attempt?.user?.fullName || 'John Smith'

  // Normally NTA groups questions by section. For the mock, we'll assume they're all under one "GATE 2026" section,
  // or we can extract the section names if there are multiple.
  const sectionTitle = questions[activeQuestionIndex]?.section || 'General Aptitude'

  return (
    <aside className="w-[17%] min-w-[220px] bg-[#e0ecf8] flex flex-col h-full font-sans select-none shrink-0 overflow-hidden relative">
      {/* CANDIDATE BRIEF SUMMARY PROFILE */}
      <div className="flex flex-col bg-white">
        <div className="flex gap-2 p-2 h-[80px]">
          <div className="w-[60px] h-[70px] border-2 border-gray-300 bg-gray-50 flex items-center justify-center shrink-0">
            <img src="https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png" alt="Candidate" className="w-full h-full object-cover" />
          </div>
          <div className="flex flex-col justify-start mt-1">
            <span className="text-[14px] font-bold text-[#000080] leading-tight">
              {candidateName}
            </span>
          </div>
        </div>
      </div>

      {/* STATUS LEGENDS */}
      <Legend />

      {/* SECTION TITLE ROW */}
      <div className="bg-[#337ab7] text-white font-bold text-[13px] px-2 py-1 mt-1 border-t border-gray-300">
        {sectionTitle}
      </div>

      {/* QUESTION PALETTE GRID AREA */}
      <div className="flex-1 overflow-y-auto bg-[#e0ecf8] p-2">
        <span className="text-[12px] font-bold text-[#333] mb-2 block">Choose a Question</span>

        {/* Note: NTA palette uses a 4 column layout */}
        <div className="grid grid-cols-4 gap-2">
          {questions.map((q, idx) => (
            <PaletteButton
              key={q.id}
              index={idx + 1}
              status={questionStates[idx] || 'NOT_VISITED'}
              isActive={activeQuestionIndex === idx}
              onClick={() => jumpToQuestion(idx)}
            />
          ))}
        </div>
      </div>

      {/* The Submit button is usually floating near the bottom or in the ActionBar. 
          The NTA interface doesn't always have a giant Submit button until the end, 
          but some versions have it at the bottom of the left panel or in the palette panel. 
          We already put Submit in the ActionBar, so we don't need it here. */}
    </aside>
  )
}
