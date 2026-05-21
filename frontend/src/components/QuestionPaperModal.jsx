import React from 'react'
import { useExamStore } from '../store/examStore'
import LatexRenderer from './LatexRenderer'

export default function QuestionPaperModal() {
  const { isQuestionPaperVisible, questions } = useExamStore()

  if (!isQuestionPaperVisible) return null;

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/50 p-6 font-sans">
      <div className="bg-white w-full max-w-5xl h-full flex flex-col rounded shadow-2xl overflow-hidden border-2 border-[#337ab7]">
        {/* Header */}
        <div className="bg-[#337ab7] text-white px-4 py-2 flex justify-between items-center shrink-0">
          <h2 className="text-[15px] font-bold">Question Paper</h2>
          <button 
            onClick={() => useExamStore.setState({ isQuestionPaperVisible: false })}
            className="flex items-center gap-1 hover:text-red-200 font-bold text-sm"
          >
            Close X
          </button>
        </div>

        {/* Subheader */}
        <div className="bg-white border-b border-gray-300 p-2 text-center shrink-0">
          <h3 className="text-[#0000ff] font-bold text-[16px]">
            Note that the timer is ticking while you read this question paper. Close this page to return to answering the questions.
          </h3>
        </div>

        {/* Paper Content */}
        <div className="flex-1 overflow-y-auto p-6 bg-white nta-watermark relative">
          <div className="relative z-10 space-y-10">
            {/* Hardcoded grouping by section based on mockup rules */}
            <div>
              <h2 className="text-[#337ab7] text-[22px] mb-6 font-medium">General Aptitude</h2>
              <div className="space-y-8">
                {questions.slice(0, 10).map((q, i) => (
                  <div key={q.id} className="flex gap-4">
                    <div className="font-bold shrink-0 text-sm">Q.{i + 1})</div>
                    <div className="flex-1">
                      <LatexRenderer text={q.questionText} />
                      {q.imagePath && (
                        <div className="mt-2 mb-2">
                          <img src={q.imagePath} alt={`Q${i+1}`} className="max-h-60" />
                        </div>
                      )}
                      
                      <div className="mt-4 italic text-sm text-gray-700">
                        <span className="font-bold">Question Type: </span> {q.type} ; 
                        <span className="font-bold"> Marks for correct answer: <span className="text-[#008000]">{q.marks}</span></span> ; 
                        <span className="font-bold"> Negative Marks: <span className="text-[#ff0000]">{q.negativeMarks}</span></span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {questions.length > 10 && (
              <div className="pt-8 border-t border-gray-300">
                <h2 className="text-[#337ab7] text-[22px] mb-6 font-medium">CS 1 Computer Science and Information Technology Mock</h2>
                <div className="space-y-8">
                  {questions.slice(10).map((q, i) => (
                    <div key={q.id} className="flex gap-4">
                      <div className="font-bold shrink-0 text-sm">Q.{i + 11})</div>
                      <div className="flex-1">
                        <LatexRenderer text={q.questionText} />
                        {q.imagePath && (
                          <div className="mt-2 mb-2">
                            <img src={q.imagePath} alt={`Q${i+11}`} className="max-h-60" />
                          </div>
                        )}
                        <div className="mt-4 italic text-sm text-gray-700">
                          <span className="font-bold">Question Type: </span> {q.type} ; 
                          <span className="font-bold"> Marks for correct answer: <span className="text-[#008000]">{q.marks}</span></span> ; 
                          <span className="font-bold"> Negative Marks: <span className="text-[#ff0000]">{q.negativeMarks}</span></span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
