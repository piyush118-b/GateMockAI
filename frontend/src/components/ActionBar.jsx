import React from 'react'
import { useExamStore } from '../store/examStore'

export default function ActionBar() {
  const { 
    saveAndNext, 
    markForReviewAndNext, 
    clearActiveResponse
  } = useExamStore()

  return (
    <footer className="bg-white border-t border-gray-300 px-4 py-2.5 flex justify-between items-center select-none shrink-0 font-sans h-[52px]">
      {/* LEFT BLOCK: MARK & CLEAR */}
      <div className="flex gap-2">
        <button
          type="button"
          onClick={markForReviewAndNext}
          className="bg-white border border-gray-300 hover:bg-gray-50 text-[#333] font-medium py-1.5 px-4 rounded-sm shadow-sm cursor-pointer text-xs transition-colors"
        >
          Mark for Review & Next
        </button>

        <button
          type="button"
          onClick={clearActiveResponse}
          className="bg-white border border-gray-300 hover:bg-gray-50 text-[#333] font-medium py-1.5 px-4 rounded-sm shadow-sm cursor-pointer text-xs transition-colors"
        >
          Clear Response
        </button>
      </div>

      {/* RIGHT BLOCK: SAVE & NEXT */}
      <div className="flex gap-2">
        <button
          type="button"
          onClick={saveAndNext}
          className="bg-[#0070a4] border border-[#005882] hover:bg-[#005c89] text-white font-bold py-1.5 px-6 rounded-sm shadow-sm cursor-pointer text-xs transition-colors"
        >
          Save & Next
        </button>
      </div>
    </footer>
  )
}

