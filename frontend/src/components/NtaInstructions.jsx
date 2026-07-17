import React, { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useExamStore } from '../store/examStore'
import NtaHeader from './NtaHeader'

export default function NtaInstructions() {
  const [page, setPage] = useState(1)
  const [agreed, setAgreed] = useState(false)
  const navigate = useNavigate()
  const { testId } = useParams()
  const { attempt } = useExamStore()
  
  const candidateName = attempt?.user?.fullName || 'John Smith'

  const handleBegin = () => {
    if (agreed) {
      navigate(`/student/tests/${testId}/take`)
    }
  }

  return (
    <div className="h-screen flex flex-col bg-white font-sans select-none overflow-hidden">
      <NtaHeader title="" />
      
      {/* Sub Header for Instructions */}
      <div className="bg-[#d9edf7] border-b border-[#bce8f1] px-4 py-1.5 flex justify-between items-center">
        <h3 className="text-[#31708f] font-bold text-[14px]">
          {page === 1 ? 'Instructions' : 'Other Important Instructions'}
        </h3>
      </div>

      <div className="flex-1 flex overflow-hidden">
        {/* Left Content Area */}
        <div className="flex-1 overflow-y-auto p-8 relative pb-24">
          <div className="max-w-4xl mx-auto">
            {page === 1 ? (
              <div className="text-[13px] text-black">
                <h2 className="text-center font-bold text-[16px] mb-6">General Instructions</h2>
                
                <p className="font-bold mb-4">Please read the following carefully.</p>
                
                <ol className="list-decimal pl-5 space-y-4">
                  <li>The duration of the examination is <strong>180 minutes</strong>. The clock will be set on the server. The countdown timer at the top right-hand corner of your screen displays the time available for you to complete the examination.</li>
                  <li>When the timer reaches zero, the examination will end automatically. You will NOT be required to submit your examination.</li>
                  <li>The screen is divided in two panels. The panel on the left shows the Questions - one at a time and the narrow panel on the right (below candidate name) has Question Palette and Question numbers.</li>
                  <li>
                    The Question Palette shows the status of each question using one of the following symbols:
                    <table className="mt-2 border-collapse border border-gray-400 w-full max-w-lg text-[13px]">
                      <tbody>
                        <tr>
                          <td className="border border-gray-400 p-2 text-center w-12 bg-white">
                            <div className="w-7 h-7 border border-[#b6b6b6] bg-gradient-to-b from-[#ffffff] to-[#e6e6e6] text-[#000] font-sans font-bold text-xs flex items-center justify-center rounded-[3px] shadow-sm mx-auto select-none">1</div>
                          </td>
                          <td className="border border-gray-400 p-2 text-gray-700">You have NOT visited the question yet.</td>
                        </tr>
                        <tr>
                          <td className="border border-gray-400 p-2 text-center bg-white">
                            <svg width="28" height="28" viewBox="0 0 36 36" className="mx-auto drop-shadow-sm select-none">
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
                          <td className="border border-gray-400 p-2 text-gray-700">You have NOT answered the question.</td>
                        </tr>
                        <tr>
                          <td className="border border-gray-400 p-2 text-center bg-white">
                            <svg width="28" height="28" viewBox="0 0 36 36" className="mx-auto drop-shadow-sm select-none">
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
                          <td className="border border-gray-400 p-2 text-gray-700">You have answered the question. <strong>This will be evaluated.</strong></td>
                        </tr>
                        <tr>
                          <td className="border border-gray-400 p-2 text-center bg-white">
                            <svg width="28" height="28" viewBox="0 0 36 36" className="mx-auto drop-shadow-sm select-none">
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
                          <td className="border border-gray-400 p-2 text-gray-700">You have NOT answered the question but marked it for review.</td>
                        </tr>
                        <tr>
                          <td className="border border-gray-400 p-2 text-center relative bg-white">
                            <svg width="28" height="28" viewBox="0 0 36 36" className="mx-auto drop-shadow-sm select-none">
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
                          <td className="border border-gray-400 p-2 text-gray-700">You have answered the question and marked it for review (will also be evaluated).</td>
                        </tr>
                      </tbody>
                    </table>
                  </li>
                </ol>
              </div>
            ) : (
              <div className="text-[13px] text-black">
                <h2 className="text-center font-bold text-[16px] mb-6">Paper-specific instructions</h2>
                
                <p className="font-bold mb-4">Please read the following carefully.</p>
                
                <p className="mb-4">This question paper has 65 questions for a total of 100 marks. It consists of 2 sections: General Aptitude (GA) for 15 marks and the subject-specific section for 85 marks. Both the sections are compulsory. The marks distribution is as follows:</p>
                
                <table className="border-collapse border border-gray-400 mx-auto w-full max-w-lg text-center mb-6 text-[13px]">
                  <thead>
                    <tr className="bg-gray-100 font-bold">
                      <td className="border border-gray-400 p-2">Section</td>
                      <td className="border border-gray-400 p-2">1-mark questions</td>
                      <td className="border border-gray-400 p-2">2-mark questions</td>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td className="border border-gray-400 p-2">General Aptitude</td>
                      <td className="border border-gray-400 p-2">5</td>
                      <td className="border border-gray-400 p-2">5</td>
                    </tr>
                    <tr>
                      <td className="border border-gray-400 p-2">Subject-specific section</td>
                      <td className="border border-gray-400 p-2">25</td>
                      <td className="border border-gray-400 p-2">30</td>
                    </tr>
                  </tbody>
                </table>
                
                <p className="mb-8">The 1-mark questions are followed by the 2-mark questions in each section.</p>
                
                <div className="mt-16 pt-4 border-t border-gray-300">
                  <label className="flex items-start gap-2 cursor-pointer">
                    <input 
                      type="checkbox" 
                      className="mt-1 w-4 h-4"
                      checked={agreed}
                      onChange={(e) => setAgreed(e.target.checked)}
                    />
                    <span className="text-[11px] leading-tight">
                      I have read and understood the instructions. All computer hardware allotted to me are in proper working condition. I declare that I am not in possession of/not wearing/not carrying any prohibited gadget like mobile phone, Bluetooth devices etc/any prohibited material with me into the Examination Hall. I agree that in case of not adhering to the instructions, I shall be liable to be debarred from this Test and/or to disciplinary action, which may include ban from future Tests/Examinations.
                    </span>
                  </label>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Sidebar - Candidate Info */}
        <div className="w-[200px] border-l border-gray-300 bg-white flex flex-col items-center pt-10 px-4 shrink-0">
          <div className="w-[100px] h-[120px] border border-gray-300 bg-white flex items-center justify-center shrink-0 overflow-hidden shadow-sm">
            <svg viewBox="0 0 100 120" className="w-full h-full bg-gray-200">
              <rect width="100" height="120" fill="#f0f4f8" />
              <circle cx="50" cy="42" r="22" fill="#5b86b6" />
              <path d="M 50,68 C 28,68 16,84 16,102 L 84,102 C 84,84 72,68 50,68 Z" fill="#3a5e8c" />
              <path d="M 50,68 L 44,78 L 50,92 L 56,78 Z" fill="#e0ecf8" />
            </svg>
          </div>
          <div className="mt-4 font-bold text-[14px] text-[#0f2c59] text-center w-full truncate px-2 select-text">
            {candidateName}
          </div>
        </div>
      </div>

      {/* Navigation Footer */}
      <div className="h-14 bg-[#f5f5f5] border-t border-gray-300 flex items-center justify-between px-6 z-10 shrink-0 select-none">
        <div className="w-1/4">
          {page === 2 && (
            <button 
              onClick={() => setPage(1)}
              className="px-6 py-1.5 border border-gray-400 bg-white hover:bg-gray-50 text-[13px] rounded-sm flex items-center gap-1 shadow-sm font-semibold cursor-pointer outline-none transition-colors"
            >
              &lt; Previous
            </button>
          )}
        </div>
        
        <div className="flex-1 flex justify-center">
          {page === 2 && (
            <button 
              onClick={handleBegin}
              disabled={!agreed}
              className={`px-8 py-2 border rounded-sm font-semibold text-[14px] shadow-sm transition-all duration-100 ${
                agreed 
                  ? 'bg-[#5bc0de] hover:bg-[#31b0d5] text-white border-[#46b8da] cursor-pointer' 
                  : 'bg-[#999999] text-white border-[#777777] cursor-not-allowed'
              }`}
            >
              I am ready to begin
            </button>
          )}
        </div>

        <div className="w-1/4 flex justify-end">
          {page === 1 && (
            <button 
              onClick={() => setPage(2)}
              className="px-6 py-1.5 border border-gray-400 bg-white hover:bg-gray-50 text-[13px] rounded-sm flex items-center gap-1 shadow-sm font-semibold cursor-pointer outline-none transition-colors"
            >
              Next &gt;
            </button>
          )}
        </div>
      </div>
      
      {/* Footer Version */}
      <div className="h-[22px] bg-[#5e7d9b] text-white text-[11px] flex items-center justify-center border-t border-gray-400 font-bold select-none shrink-0">
        Version : 17.07.00
      </div>
    </div>
  )
}
