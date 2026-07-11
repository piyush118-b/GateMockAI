import React, { useEffect, useState } from 'react'
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
    decrementTimer
  } = useExamStore()

  const activeQ = questions[activeQuestionIndex]

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
      {/* STRIP 1: Section Navigator & Calculator */}
      <div className="bg-[#337ab7] text-white flex justify-between items-center px-4 h-9">
        <div className="flex items-center gap-4 text-sm font-bold">
          <span className="flex items-center gap-1 cursor-pointer hover:text-gray-200 text-lg">
            &laquo;&raquo;
          </span>
          <span>Sections</span>
          <div className="bg-white text-black px-4 py-1 text-xs border-t-[3px] border-[#337ab7]">
            {activeQ.section || 'General Aptitude'}
          </div>
        </div>
        <div>
          <button 
            type="button" 
            className="flex items-center justify-center w-8 h-8 rounded-full bg-[#1b6d85] border border-white hover:bg-[#155a6d] shadow-sm cursor-pointer"
            onClick={() => useExamStore.setState({ isCalculatorVisible: !useExamStore.getState().isCalculatorVisible })}
            title="Calculator"
          >
            🧮
          </button>
        </div>
      </div>

      {/* STRIP 2: Tabs Row and Timer */}
      <div className="bg-[#f5f5f5] border-b border-gray-300 flex justify-between items-center px-2 py-1">
        <div className="flex gap-1">
          {/* Mock tabs, usually populated dynamically, but hardcoding layout style */}
          <div className="bg-nta-teal text-white px-3 py-1 text-xs font-bold border-r border-[#00796b]">
            {activeQ.section || 'General Aptitude'}
          </div>
          {/* Other sections could go here if we extracted distinct sections */}
        </div>
        <div className="text-black font-bold text-[13px] mr-2">
          Time Left : <span className="text-[#333]">{formatTime(timeLeft)}</span>
        </div>
      </div>

      {/* STRIP 3: Question Meta Info */}
      <div className="bg-[#5bc0de] text-white px-3 py-1 flex items-center justify-between text-[13px] font-bold">
        <span>Question Type: {activeQ.type}</span>
        <div className="flex gap-4">
          <span>Marks for correct answer : <span className="text-[#008000]">{activeQ.marks}</span></span>
          <span>Negative Marks : <span className="text-[#ff0000]">{activeQ.negativeMarks}</span></span>
        </div>
      </div>

      {/* STRIP 4: Question No */}
      <div className="bg-white border-b border-gray-200 px-3 py-1.5 text-[#333] font-bold text-sm">
        Question No. {activeQuestionIndex + 1}
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
