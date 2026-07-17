import React, { useState } from 'react'
import { useExamStore } from '../store/examStore'

export default function InstructionsModal() {
  const { isInstructionsVisible } = useExamStore()
  const [page, setPage] = useState(1)

  if (!isInstructionsVisible) return null;

  const handleClose = () => {
    useExamStore.setState({ isInstructionsVisible: false });
  }

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/50 p-6 font-sans">
      <div className="bg-white w-full max-w-4xl h-[90vh] flex flex-col rounded shadow-2xl overflow-hidden border-2 border-[#337ab7]">
        {/* Header */}
        <div className="bg-[#337ab7] text-white px-4 py-2.5 flex justify-between items-center shrink-0">
          <h2 className="text-[15px] font-bold">Instructions</h2>
          <button 
            onClick={handleClose}
            className="text-white hover:text-red-200 font-bold text-sm bg-transparent border-none cursor-pointer outline-none"
          >
            Close X
          </button>
        </div>

        {/* Subheader */}
        <div className="bg-[#d9edf7] border-b border-[#bce8f1] px-4 py-2 shrink-0">
          <h3 className="text-[#31708f] font-bold text-[14px]">
            {page === 1 ? 'General Instructions' : 'Other Important Instructions'}
          </h3>
        </div>

        {/* Instructions Body */}
        <div className="flex-1 overflow-y-auto p-6 bg-white text-[13px] text-black select-none">
          {page === 1 ? (
            <div className="space-y-4">
              <h2 className="text-center font-bold text-[15px] mb-4 text-[#337ab7]">General Instructions</h2>
              <ol className="list-decimal pl-5 space-y-3">
                <li>The duration of the examination is <strong>180 minutes</strong>. The clock will be set on the server. The countdown timer at the top right-hand corner of your screen displays the time available for you to complete the examination.</li>
                <li>When the timer reaches zero, the examination will end automatically. You will NOT be required to submit your examination.</li>
                <li>The screen is divided into two panels. The panel on the left shows the Questions - one at a time, and the narrow panel on the right has the Question Palette and Question numbers.</li>
                <li>
                  The Question Palette shows the status of each question using one of the following symbols:
                  <table className="mt-2.5 border-collapse border border-gray-400 w-full max-w-lg text-[12px]">
                    <tbody>
                      <tr>
                        <td className="border border-gray-300 p-2 text-center w-12 bg-white">
                          <div className="w-[30px] h-[30px] border border-gray-400 text-black flex items-center justify-center font-bold rounded-sm mx-auto">1</div>
                        </td>
                        <td className="border border-gray-300 p-2 text-gray-700">You have NOT visited the question yet.</td>
                      </tr>
                      <tr>
                        <td className="border border-gray-300 p-2 text-center bg-white">
                          <svg width="30" height="30" viewBox="0 0 36 36" className="mx-auto">
                            <defs>
                              <linearGradient id="orangeGradInst" x1="0%" y1="0%" x2="0%" y2="100%">
                                <stop offset="0%" stopColor="#f05a28" />
                                <stop offset="100%" stopColor="#d93c0b" />
                              </linearGradient>
                            </defs>
                            <path d="M 2,2 L 34,2 L 34,22 L 18,34 L 2,22 Z" fill="url(#orangeGradInst)" stroke="#b22c00" strokeWidth="1.5" strokeLinejoin="round" />
                            <text x="50%" y="54%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold">2</text>
                          </svg>
                        </td>
                        <td className="border border-gray-300 p-2 text-gray-700">You have NOT answered the question.</td>
                      </tr>
                      <tr>
                        <td className="border border-gray-300 p-2 text-center bg-white">
                          <svg width="30" height="30" viewBox="0 0 36 36" className="mx-auto">
                            <defs>
                              <linearGradient id="greenGradInst" x1="0%" y1="0%" x2="0%" y2="100%">
                                <stop offset="0%" stopColor="#4caf50" />
                                <stop offset="100%" stopColor="#2e7d32" />
                              </linearGradient>
                            </defs>
                            <path d="M 18,2 L 34,14 L 34,34 L 2,34 L 2,14 Z" fill="url(#greenGradInst)" stroke="#1b5e20" strokeWidth="1.5" strokeLinejoin="round" />
                            <text x="50%" y="60%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold">3</text>
                          </svg>
                        </td>
                        <td className="border border-gray-300 p-2 text-gray-700">You have answered the question. <strong>This will be evaluated.</strong></td>
                      </tr>
                      <tr>
                        <td className="border border-gray-300 p-2 text-center bg-white">
                          <svg width="30" height="30" viewBox="0 0 36 36" className="mx-auto">
                            <defs>
                              <linearGradient id="purpleGradInst" x1="0%" y1="0%" x2="0%" y2="100%">
                                <stop offset="0%" stopColor="#8e44ad" />
                                <stop offset="100%" stopColor="#682a8a" />
                              </linearGradient>
                            </defs>
                            <circle cx="18" cy="18" r="16" fill="url(#purpleGradInst)" stroke="#4a1565" strokeWidth="1.5" />
                            <text x="50%" y="53%" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold">4</text>
                          </svg>
                        </td>
                        <td className="border border-gray-300 p-2 text-gray-700">You have NOT answered the question but marked it for review.</td>
                      </tr>
                      <tr>
                        <td className="border border-gray-300 p-2 text-center relative bg-white">
                          <svg width="30" height="30" viewBox="0 0 36 36" className="mx-auto">
                            <defs>
                              <linearGradient id="purpleGradInst2" x1="0%" y1="0%" x2="0%" y2="100%">
                                <stop offset="0%" stopColor="#8e44ad" />
                                <stop offset="100%" stopColor="#682a8a" />
                              </linearGradient>
                              <linearGradient id="badgeGradInst" x1="0%" y1="0%" x2="0%" y2="100%">
                                <stop offset="0%" stopColor="#4caf50" />
                                <stop offset="100%" stopColor="#2e7d32" />
                              </linearGradient>
                            </defs>
                            <circle cx="18" cy="18" r="16" fill="url(#purpleGradInst2)" stroke="#4a1565" strokeWidth="1.5" />
                            <text x="18" y="19" dominantBaseline="middle" textAnchor="middle" fill="white" fontSize="13" fontWeight="bold">5</text>
                            <circle cx="28" cy="28" r="6.5" fill="url(#badgeGradInst)" stroke="#fff" strokeWidth="1" />
                            <path d="M 25.5,28 L 27,29.5 L 30.5,26" stroke="white" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" fill="none" />
                          </svg>
                        </td>
                        <td className="border border-gray-300 p-2 text-gray-700">You have answered the question and marked it for review (will be considered for evaluation).</td>
                      </tr>
                    </tbody>
                  </table>
                </li>
              </ol>
            </div>
          ) : (
            <div className="space-y-4">
              <h2 className="text-center font-bold text-[15px] mb-4 text-[#337ab7]">Paper-Specific Instructions</h2>
              <p className="font-bold text-gray-800">Please read the following carefully.</p>
              <p className="leading-relaxed">This question paper has 65 questions for a total of 100 marks. It consists of 2 sections: General Aptitude (GA) for 15 marks and the subject-specific section for 85 marks. Both sections are compulsory. The marks distribution is as follows:</p>
              
              <table className="border-collapse border border-gray-300 w-full max-w-md text-center text-[12px] mx-auto mt-4">
                <thead>
                  <tr className="bg-gray-100 font-bold text-gray-700">
                    <td className="border border-gray-300 p-2">Section</td>
                    <td className="border border-gray-300 p-2">1-mark questions</td>
                    <td className="border border-gray-300 p-2">2-mark questions</td>
                  </tr>
                </thead>
                <tbody>
                  <tr className="text-gray-600">
                    <td className="border border-gray-300 p-2 font-semibold">General Aptitude</td>
                    <td className="border border-gray-300 p-2">5</td>
                    <td className="border border-gray-300 p-2">5</td>
                  </tr>
                  <tr className="text-gray-600">
                    <td className="border border-gray-300 p-2 font-semibold">Subject Section</td>
                    <td className="border border-gray-300 p-2">25</td>
                    <td className="border border-gray-300 p-2">30</td>
                  </tr>
                </tbody>
              </table>
              
              <p className="mt-4 leading-relaxed">The 1-mark questions are followed by the 2-mark questions in each section.</p>
            </div>
          )}
        </div>

        {/* Footer Navigation */}
        <div className="bg-[#f5f5f5] border-t border-gray-300 px-4 py-3 flex justify-between shrink-0">
          <div>
            {page === 2 && (
              <button 
                onClick={() => setPage(1)}
                className="px-4 py-1.5 border border-gray-400 bg-white hover:bg-gray-50 text-[13px] rounded-sm font-semibold cursor-pointer shadow-sm"
              >
                &lt; Previous
              </button>
            )}
          </div>
          <div className="flex gap-2">
            {page === 1 ? (
              <button 
                onClick={() => setPage(2)}
                className="px-4 py-1.5 border border-gray-400 bg-white hover:bg-gray-50 text-[13px] rounded-sm font-semibold cursor-pointer shadow-sm"
              >
                Next &gt;
              </button>
            ) : (
              <button 
                onClick={handleClose}
                className="px-5 py-1.5 bg-[#5bc0de] hover:bg-[#31b0d5] border border-[#46b8da] text-white text-[13px] rounded-sm font-semibold cursor-pointer shadow-sm"
              >
                Close Instructions
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
