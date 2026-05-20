import React from 'react'
import { useExamStore } from '../store/examStore'

export default function ActionBar() {
  const { 
    saveAndNext, 
    markForReviewAndNext, 
    clearActiveResponse
  } = useExamStore()

  const handleSubmitTrigger = () => {
    useExamStore.setState({ isSubmitModalActive: true });
  }

  return (
    <footer className="bg-[#f5f5f5] border-t border-gray-300 px-4 py-2 flex justify-between items-center select-none shrink-0 font-sans">
      {/* LEFT BLOCK: MARK & CLEAR */}
      <div className="flex gap-2">
        <button
          type="button"
          onClick={markForReviewAndNext}
          className="bg-white border border-gray-400 hover:bg-gray-50 text-gray-800 font-semibold py-1.5 px-3 rounded-sm shadow-sm cursor-pointer text-xs"
        >
          Mark for Review & Next
        </button>

        <button
          type="button"
          onClick={clearActiveResponse}
          className="bg-white border border-gray-400 hover:bg-gray-50 text-gray-800 font-semibold py-1.5 px-3 rounded-sm shadow-sm cursor-pointer text-xs"
        >
          Clear Response
        </button>
      </div>

      {/* RIGHT BLOCK: SAVE & SUBMIT */}
      <div className="flex gap-2">
        <button
          type="button"
          onClick={saveAndNext}
          className="bg-[#337ab7] border border-[#2e6da4] hover:bg-[#286090] text-white font-semibold py-1.5 px-6 rounded-sm shadow-sm cursor-pointer text-xs"
        >
          Save & Next
        </button>

        <button
          type="button"
          onClick={handleSubmitTrigger}
          className="bg-[#5bc0de] border border-[#46b8da] hover:bg-[#31b0d5] text-white font-semibold py-1.5 px-4 rounded-sm shadow-sm cursor-pointer text-xs ml-4"
        >
          Submit
        </button>
      </div>
    </footer>
  )
}
