import React, { useEffect, useState, useRef } from 'react'
import { useExamStore } from '../store/examStore'
import LatexRenderer from './LatexRenderer'
import NATInput from './NATInput'

export default function QuestionCard() {
  const { 
    questions, 
    activeQuestionIndex, 
    answersCache, 
    setAnswerValue,
    timeLeft,
    decrementTimer,
    activeSection,
    jumpToQuestion
  } = useExamStore()

  const activeQ = questions[activeQuestionIndex]
  const [popoverSection, setPopoverSection] = useState(null)
  const popoverRef = useRef(null)

  // Close popover on click outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (popoverRef.current && !popoverRef.current.contains(event.target)) {
        setPopoverSection(null)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Calculate dynamic popover counts for a section index or total
  const getPopoverCounts = (sectionKey) => {
    let filteredIndices = []

    if (sectionKey === 'category') {
      filteredIndices = questions.map((_, idx) => idx)
    } else {
      filteredIndices = questions
        .map((_, idx) => idx)
        .filter(idx => {
          const qSection = (idx >= 10) ? 1 : 0
          return qSection === sectionKey
        })
    }

    const counts = {
      NOT_VISITED: 0,
      NOT_ANSWERED: 0,
      ANSWERED: 0,
      MARKED: 0,
      MARKED_ANSWERED: 0
    }

    filteredIndices.forEach(idx => {
      const status = questionStates && questionStates[idx] ? questionStates[idx] : 'NOT_VISITED'
      // Wait, let's read the state from the store
      const stateFromStore = useExamStore.getState().questionStates[idx] || 'NOT_VISITED'
      if (counts[stateFromStore] !== undefined) {
        counts[stateFromStore]++
      }
    })

    return counts
  }

  // Render popup template
  const renderPopover = (sectionKey, title) => {
    const sc = getPopoverCounts(sectionKey)
    return (
      <div 
        ref={popoverRef}
        className="absolute top-7 left-1/2 -translate-x-1/2 z-[100] w-[230px] bg-[#e0ecf8] border border-[#bce8f1] rounded shadow-xl font-sans py-1 text-left"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="bg-[#337ab7] text-white text-[11px] font-bold px-2 py-1 flex justify-between items-center shrink-0">
          <span className="truncate max-w-[190px]">{title}</span>
          <span className="cursor-pointer hover:text-red-200 text-xs px-1" onClick={() => setPopoverSection(null)}>&#10005;</span>
        </div>
        
        {/* Body */}
        <div className="bg-white p-2 border-t-0 rounded-b flex flex-col gap-2">
          {/* Answered */}
          <div className="flex items-center gap-2.5">
            <svg width="24" height="24" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
              <path d="M 18,2 L 34,14 L 34,34 L 2,34 L 2,14 Z" fill="#2e7d32" stroke="#1b5e20" strokeWidth="1.5" strokeLinejoin="round" />
              <text x="50%" y="60%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold">{sc.ANSWERED}</text>
            </svg>
            <span className="text-[11px] text-gray-700 font-semibold">{sc.ANSWERED} Answered</span>
          </div>

          {/* Not Answered */}
          <div className="flex items-center gap-2.5">
            <svg width="24" height="24" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
              <path d="M 2,2 L 34,2 L 34,22 L 18,34 L 2,22 Z" fill="#f05a28" stroke="#b22c00" strokeWidth="1.5" strokeLinejoin="round" />
              <text x="50%" y="54%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold">{sc.NOT_ANSWERED}</text>
            </svg>
            <span className="text-[11px] text-gray-700 font-semibold">{sc.NOT_ANSWERED} Not Answered</span>
          </div>

          {/* Not Visited */}
          <div className="flex items-center gap-2.5">
            <div className="w-6 h-6 border border-[#b6b6b6] bg-gradient-to-b from-[#ffffff] to-[#e6e6e6] text-[#000] font-sans font-bold text-[11px] flex items-center justify-center rounded-sm shadow-sm shrink-0">
              {sc.NOT_VISITED}
            </div>
            <span className="text-[11px] text-gray-700 font-semibold">{sc.NOT_VISITED} Not Visited</span>
          </div>

          {/* Marked for Review */}
          <div className="flex items-center gap-2.5">
            <svg width="24" height="24" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
              <circle cx="18" cy="18" r="16" fill="#8e44ad" stroke="#4a1565" strokeWidth="1.5" />
              <text x="50%" y="53%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold">{sc.MARKED}</text>
            </svg>
            <span className="text-[11px] text-gray-700 font-semibold">{sc.MARKED} Marked for Review</span>
          </div>

          {/* Answered & Marked for Review */}
          <div className="flex items-center gap-2.5">
            <svg width="24" height="24" viewBox="0 0 36 36" className="drop-shadow-sm shrink-0">
              <circle cx="18" cy="18" r="16" fill="#8e44ad" stroke="#4a1565" strokeWidth="1.5" />
              <text x="18" y="19" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold">{sc.MARKED_ANSWERED}</text>
              <circle cx="28" cy="28" r="6.5" fill="#2e7d32" stroke="#fff" strokeWidth="1" />
              <path d="M 25.5,28 L 27,29.5 L 30.5,26" stroke="white" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" fill="none" />
            </svg>
            <span className="text-[11px] text-gray-700 font-semibold truncate leading-none">Answered & Marked for Review</span>
          </div>
        </div>
      </div>
    )
  }

  // Setup timer in QuestionCard since we removed Header
  useEffect(() => {
    const timer = setInterval(() => {
      decrementTimer()
    }, 1000)
    return () => clearInterval(timer)
  }, [decrementTimer])

  const formatTime = (seconds) => {
    const h = Math.floor(seconds / 3600).toString().padStart(2, '0')
    const m = Math.floor((seconds % 3600) / 60).toString().padStart(2, '0')
    const s = (seconds % 60).toString().padStart(2, '0')
    return `${h}:${m}:${s}`
  }

  if (!activeQ) {
    return (
      <div className="flex-1 flex items-center justify-center p-6 bg-white select-none">
        <span className="text-gray-400 font-medium italic">No active question selected.</span>
      </div>
    )
  }

  const selectedVal = answersCache[activeQ.id] || ""

  const handleOptionSelect = (optionId) => {
    if (activeQ.type === 'MCQ') {
      setAnswerValue(activeQ.id, optionId)
    } else if (activeQ.type === 'MSQ') {
      let selected = selectedVal ? selectedVal.split(',') : []
      if (selected.includes(optionId)) {
        selected = selected.filter(id => id !== optionId)
      } else {
        selected.push(optionId)
      }
      setAnswerValue(activeQ.id, selected.join(','))
    }
  }

  const isOptionSelected = (optionId) => {
    if (activeQ.type === 'MCQ') {
      return selectedVal === optionId
    } else if (activeQ.type === 'MSQ') {
      return selectedVal.split(',').includes(optionId)
    }
    return false
  }

  return (
    <div className="flex-1 flex flex-col bg-white font-sans select-none overflow-y-auto">
      {/* ROW 1: Category / Subject bar */}
      <div className="flex items-center bg-[#f5f5f5] border-b border-[#cccccc] h-9 justify-between px-3 select-none shrink-0">
        <div className="flex items-center gap-1.5 h-full py-1">
          <button className="h-7 w-6 bg-white border border-gray-400 text-gray-700 flex items-center justify-center text-xs hover:bg-gray-50 cursor-pointer font-bold rounded-sm outline-none">&lt;</button>
          
          {/* Category Tab */}
          <div className="bg-[#337ab7] text-white text-[11px] font-bold px-4 py-1.5 flex items-center gap-1.5 border border-[#2e6da4] shadow-sm select-none rounded-t-[3px]">
            <span>CS 1 Computer Science and Information Technology Mock</span>
            
            {/* Info popover trigger */}
            <div className="relative inline-block leading-none">
              <span 
                onClick={(e) => { e.stopPropagation(); setPopoverSection(popoverSection === 'category' ? null : 'category'); }} 
                className="w-3.5 h-3.5 rounded-full bg-white text-[#337ab7] flex items-center justify-center text-[9px] font-bold hover:bg-gray-100 cursor-pointer"
              >
                i
              </span>
              {popoverSection === 'category' && renderPopover('category', "CS 1 Computer Science...")}
            </div>
          </div>

          <button className="h-7 w-6 bg-white border border-gray-400 text-gray-700 flex items-center justify-center text-xs hover:bg-gray-50 cursor-pointer font-bold rounded-sm outline-none">&gt;</button>
        </div>

        {/* Orange calculator button */}
        <div>
          <button 
            type="button" 
            className="w-8 h-7 bg-[#f0ad4e] hover:bg-[#ec971f] border border-[#eea236] text-white flex items-center justify-center text-sm cursor-pointer shadow-sm rounded-sm outline-none"
            onClick={() => useExamStore.setState({ isCalculatorVisible: !useExamStore.getState().isCalculatorVisible })}
            title="Calculator"
          >
            🧮
          </button>
        </div>
      </div>

      {/* ROW 2: Sections Title & Time Left */}
      <div className="flex justify-between items-center bg-[#f5f5f5] border-b border-[#cccccc] h-[34px] px-3 select-none shrink-0">
        <div className="text-[13px] font-bold text-gray-700">Sections</div>
        <div className="text-black font-bold text-[13px] mr-1">
          Time Left : <span className="text-[#333] font-mono">{formatTime(timeLeft)}</span>
        </div>
      </div>

      {/* ROW 3: Sub-section tabs bar */}
      <div className="flex justify-between items-center bg-[#e0ecf8] border-b border-[#bce8f1] px-2 h-[38px] select-none shrink-0">
        <div className="flex items-center gap-1.5 h-full mt-[1px]">
          {/* Left Arrow */}
          <button className="h-7 w-6 bg-white border border-gray-400 text-gray-700 flex items-center justify-center text-xs hover:bg-gray-50 cursor-pointer font-bold rounded-sm outline-none">&lt;</button>
          
          {/* Tab 1: GA */}
          <button 
            onClick={() => jumpToQuestion(0)}
            className={`h-[37px] px-4 text-xs font-bold flex items-center gap-1.5 cursor-pointer outline-none border-b-0 transition-all rounded-t-[3px] ${
              activeSection === 0 
                ? 'bg-[#337ab7] text-white border border-[#2e6da4]' 
                : 'bg-white text-[#337ab7] border border-[#cccccc]'
            }`}
          >
            <span>General Aptitude</span>
            
            {/* Info popover trigger */}
            <div className="relative inline-block leading-none">
              <span 
                onClick={(e) => { e.stopPropagation(); setPopoverSection(popoverSection === 0 ? null : 0); }} 
                className={`w-3.5 h-3.5 rounded-full flex items-center justify-center text-[9px] font-bold cursor-pointer ${activeSection === 0 ? 'bg-white text-[#337ab7] hover:bg-gray-100' : 'bg-[#337ab7] text-white hover:bg-[#286090]'}`}
              >
                i
              </span>
              {popoverSection === 0 && renderPopover(0, "General Aptitude")}
            </div>
          </button>

          {/* Tab 2: CS */}
          <button 
            onClick={() => jumpToQuestion(10)}
            className={`h-[37px] px-4 text-xs font-bold flex items-center gap-1.5 cursor-pointer outline-none border-b-0 transition-all rounded-t-[3px] ${
              activeSection === 1 
                ? 'bg-[#337ab7] text-white border border-[#2e6da4]' 
                : 'bg-white text-[#337ab7] border border-[#cccccc]'
            }`}
          >
            <span>CS 1 Computer Science and Information Technology Mock</span>
            
            {/* Info popover trigger */}
            <div className="relative inline-block leading-none">
              <span 
                onClick={(e) => { e.stopPropagation(); setPopoverSection(popoverSection === 1 ? null : 1); }} 
                className={`w-3.5 h-3.5 rounded-full flex items-center justify-center text-[9px] font-bold cursor-pointer ${activeSection === 1 ? 'bg-white text-[#337ab7] hover:bg-gray-100' : 'bg-[#337ab7] text-white hover:bg-[#286090]'}`}
              >
                i
              </span>
              {popoverSection === 1 && renderPopover(1, "CS 1 Computer Science...")}
            </div>
          </button>
          
          {/* Right Arrow */}
          <button className="h-7 w-6 bg-white border border-gray-400 text-gray-700 flex items-center justify-center text-xs hover:bg-gray-50 cursor-pointer font-bold rounded-sm outline-none">&gt;</button>
        </div>
      </div>

      {/* ROW 4: Question Meta Info (TCS Style light blue band) */}
      <div className="bg-[#d9edf7] border-b border-[#bce8f1] text-[#31708f] px-3 py-1.5 flex items-center justify-between text-[13px] font-bold shrink-0">
        <span>Question Type: {activeQ.type}</span>
        <div className="flex gap-4">
          <span>Marks for correct answer : <span className="text-[#008000]">{activeQ.marks}</span></span>
          <span>Negative Marks : <span className="text-[#ff0000]">{activeQ.negativeMarks}</span></span>
        </div>
      </div>

      {/* ROW 5: Question No */}
      <div className="bg-white border-b border-gray-200 px-3 py-1.5 text-[#333] font-bold text-sm shrink-0">
        Question No. {activeQuestionIndex >= 10 ? (activeQuestionIndex - 9) : (activeQuestionIndex + 1)}
      </div>

      {/* QUESTION MAIN WRAPPER */}
      <div className="flex-1 p-4 flex flex-col gap-6 overflow-y-auto relative z-10">
        {/* QUESTION TEXT (LaTeX) */}
        <div className="text-[15px] text-black leading-relaxed font-normal bg-transparent">
          <LatexRenderer text={activeQ.questionText} />

          {/* QUESTION IMAGE DIAGRAM */}
          {(activeQ.imageUrl || activeQ.imagePath) && (
            <div className="mt-4 max-w-lg inline-block">
              <img 
                src={activeQ.imageUrl || activeQ.imagePath} 
                alt={activeQ.imageAltText || `Question diagram ${activeQ.sequenceNo}`} 
                className="max-h-80 object-contain border border-gray-200 rounded shadow-sm"
              />
            </div>
          )}
        </div>

        {/* INPUT OPTIONS SECTION */}
        <div className="mt-6 flex-1">
          {activeQ.type === 'NAT' ? (
            <div className="flex flex-col gap-2">
              <span className="text-xs text-gray-500 font-bold">Numerical Answer:</span>
              <NATInput 
                questionId={activeQ.id}
                value={selectedVal}
                onChange={(val) => setAnswerValue(activeQ.id, val)}
              />
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {activeQ.options.map((opt) => {
                const selected = isOptionSelected(opt.id)
                
                return (
                  <label
                    key={opt.id}
                    className="flex items-start gap-3 p-2 hover:bg-gray-50 cursor-pointer"
                  >
                    <div className="mt-1">
                      {activeQ.type === 'MCQ' ? (
                        <input
                          type="radio"
                          name={`question-${activeQ.id}`}
                          checked={selected}
                          onChange={() => handleOptionSelect(opt.id)}
                          className="w-4 h-4 cursor-pointer"
                        />
                      ) : (
                        <input
                          type="checkbox"
                          checked={selected}
                          onChange={() => handleOptionSelect(opt.id)}
                          className="w-4 h-4 cursor-pointer"
                        />
                      )}
                    </div>

                    <div className="flex-1 text-[15px] text-black pt-[2px]">
                      <LatexRenderer text={opt.optionText} />

                      {opt.imagePath && (
                        <div className="mt-2 max-w-sm inline-block">
                          <img 
                            src={opt.imagePath} 
                            alt={`Option diagram ${opt.optionLabel}`} 
                            className="max-h-48 object-contain border border-gray-300 p-1"
                          />
                        </div>
                      )}
                    </div>
                  </label>
                )
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
