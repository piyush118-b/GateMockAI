import React from 'react'
import { useExamStore } from '../store/examStore'
import { AlertTriangle, Loader2 } from 'lucide-react'

export default function SubmitModal() {
  const { 
    isSubmitModalActive, 
    isSubmitting, 
    questions, 
    questionStates, 
    submitExam, 
    error,
    set 
  } = useExamStore()

  if (!isSubmitModalActive) return null;

  // Compute counts
  const total = questions.length;
  const answered = questionStates.filter(s => s === 'ANSWERED').length;
  const notAnswered = questionStates.filter(s => s === 'NOT_ANSWERED').length;
  const marked = questionStates.filter(s => s === 'MARKED').length;
  const markedAnswered = questionStates.filter(s => s === 'MARKED_ANSWERED').length;
  const notVisited = questionStates.filter(s => s === 'NOT_VISITED').length;

  const handleCancel = () => {
    useExamStore.setState({ isSubmitModalActive: false });
  };

  const handleSubmit = () => {
    submitExam(false);
  };

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[99999] flex items-center justify-center p-4 select-none">
      <div className="bg-white border-2 border-gray-300 rounded-[4px] max-w-xl w-full shadow-2xl overflow-hidden">
        {/* HEADER */}
        <div className="bg-blue-600 text-white px-5 py-3.5 flex items-center gap-2 font-bold text-base">
          <AlertTriangle className="w-5 h-5 text-yellow-300 animate-bounce" />
          <span>Confirm Examination Submission</span>
        </div>

        {/* BODY */}
        <div className="p-6">
          <p className="text-gray-700 text-sm leading-relaxed mb-4">
            You are about to finalize and submit your GATE mock examination paper. Please review the visual checklist of your answer states below:
          </p>

          <table className="w-full text-left text-sm border-collapse border border-gray-200 mb-5 font-sans">
            <thead>
              <tr className="bg-gray-100">
                <th className="border border-gray-200 px-4 py-2 text-gray-700 font-bold">Question Status Legend</th>
                <th className="border border-gray-200 px-4 py-2 text-gray-700 font-bold text-center w-24">Count</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="border border-gray-200 px-4 py-2 text-gray-600 flex items-center gap-2">
                  <span className="w-3.5 h-3.5 bg-[#2DB34A] inline-block rounded-[2px_12px_2px_12px] border border-green-700" />
                  <span>Answered</span>
                </td>
                <td className="border border-gray-200 px-4 py-2 text-center font-bold text-green-700">{answered}</td>
              </tr>
              <tr>
                <td className="border border-gray-200 px-4 py-2 text-gray-600 flex items-center gap-2">
                  <span className="w-3.5 h-3.5 bg-[#FF0000] inline-block rounded-[12px_2px_12px_2px] border border-red-700" />
                  <span>Not Answered</span>
                </td>
                <td className="border border-gray-200 px-4 py-2 text-center font-bold text-red-600">{notAnswered}</td>
              </tr>
              <tr>
                <td className="border border-gray-200 px-4 py-2 text-gray-600 flex items-center gap-2">
                  <span className="w-3.5 h-3.5 bg-[#9B59B6] inline-block rounded-full border border-purple-700" />
                  <span>Marked for Review</span>
                </td>
                <td className="border border-gray-200 px-4 py-2 text-center font-bold text-purple-700">{marked}</td>
              </tr>
              <tr>
                <td className="border border-gray-200 px-4 py-2 text-gray-600 flex items-center gap-2">
                  <span className="w-3.5 h-3.5 bg-[#9B59B6] inline-block rounded-full border border-purple-700 relative">
                    <span className="absolute -bottom-0.5 -right-0.5 w-2 h-2 bg-green-500 rounded-full border border-white" />
                  </span>
                  <span>Answered & Marked for Review (will be evaluated)</span>
                </td>
                <td className="border border-gray-200 px-4 py-2 text-center font-bold text-purple-700">{markedAnswered}</td>
              </tr>
              <tr>
                <td className="border border-gray-200 px-4 py-2 text-gray-600 flex items-center gap-2">
                  <span className="w-3.5 h-3.5 bg-[#E0E0E0] inline-block rounded-[4px] border border-gray-400" />
                  <span>Not Visited</span>
                </td>
                <td className="border border-gray-200 px-4 py-2 text-center font-bold text-gray-500">{notVisited}</td>
              </tr>
              <tr className="bg-blue-50 font-bold">
                <td className="border border-gray-200 px-4 py-2 text-blue-900">Total Paper Questions</td>
                <td className="border border-gray-200 px-4 py-2 text-center text-blue-900">{total}</td>
              </tr>
            </tbody>
          </table>

          {error && (
            <div className="bg-red-50 border border-red-200 rounded-md p-3.5 text-xs text-red-600 font-semibold mb-4 leading-relaxed">
              {error}
            </div>
          )}

          <p className="text-xs text-red-600 leading-normal italic font-semibold">
            Warning: Once submitted, you cannot modify your answers or re-attempt this secure testing session.
          </p>
        </div>

        {/* FOOTER ACTIONS */}
        <div className="bg-gray-50 border-t border-gray-200 px-5 py-4 flex justify-end gap-3">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={handleCancel}
            className="bg-white hover:bg-gray-100 border border-gray-300 text-gray-700 font-bold py-2 px-4 rounded-[4px] shadow-sm cursor-pointer transition-all duration-150 text-xs uppercase"
          >
            Cancel & Review
          </button>
          
          <button
            type="button"
            disabled={isSubmitting}
            onClick={handleSubmit}
            className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-bold py-2 px-5 rounded-[4px] shadow-md cursor-pointer transition-all duration-150 text-xs uppercase flex items-center gap-1.5"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Submitting Attempt...</span>
              </>
            ) : (
              <span>Final Submit Paper</span>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
