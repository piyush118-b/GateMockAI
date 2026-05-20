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
                          <td className="border border-gray-400 p-2 text-center w-12">
                            <div className="w-8 h-8 bg-white border border-gray-400 text-black flex items-center justify-center font-bold">1</div>
                          </td>
                          <td className="border border-gray-400 p-2">You have NOT visited the question yet.</td>
                        </tr>
                        <tr>
                          <td className="border border-gray-400 p-2 text-center">
                            <div className="w-8 h-8 bg-[#e55a2b] text-white rounded-[50%_0_50%_50%] transform rotate-45 flex items-center justify-center font-bold mx-auto">
                              <span className="-rotate-45">2</span>
                            </div>
                          </td>
                          <td className="border border-gray-400 p-2">You have NOT answered the question.</td>
                        </tr>
                        <tr>
                          <td className="border border-gray-400 p-2 text-center">
                            <div className="w-8 h-8 bg-[#2e7d32] text-white rounded-[50%_50%_0_50%] transform rotate-45 flex items-center justify-center font-bold mx-auto">
                              <span className="-rotate-45">3</span>
                            </div>
                          </td>
                          <td className="border border-gray-400 p-2">You have answered the question. <strong>This will be evaluated.</strong></td>
                        </tr>
                        <tr>
                          <td className="border border-gray-400 p-2 text-center">
                            <div className="w-8 h-8 bg-[#6a1b9a] text-white rounded-full flex items-center justify-center font-bold mx-auto">4</div>
                          </td>
                          <td className="border border-gray-400 p-2">You have NOT answered the question but marked it for review.</td>
                        </tr>
                        <tr>
                          <td className="border border-gray-400 p-2 text-center relative">
                            <div className="w-8 h-8 bg-[#6a1b9a] text-white rounded-full flex items-center justify-center font-bold mx-auto relative">
                              5
                              <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-[#2e7d32] rounded-full border border-white"></div>
                            </div>
                          </td>
                          <td className="border border-gray-400 p-2">You have answered the question and marked it for review.</td>
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
        <div className="w-[200px] border-l border-gray-300 bg-white flex flex-col items-center pt-6">
          <div className="w-24 h-28 border border-gray-300 bg-gray-50 flex items-center justify-center shadow-sm">
            <img src="https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png" alt="Candidate" className="w-full h-full object-cover" />
          </div>
          <div className="mt-4 font-semibold text-[15px] text-[#000080] text-center px-2">
            {candidateName}
          </div>
        </div>
      </div>

      {/* Navigation Footer */}
      <div className="h-14 bg-white border-t border-gray-300 flex items-center justify-between px-6 z-10">
        <div>
          {page === 2 && (
            <button 
              onClick={() => setPage(1)}
              className="px-6 py-1.5 border border-gray-400 bg-white hover:bg-gray-50 text-[13px] rounded flex items-center gap-1 shadow-sm font-semibold"
            >
              &lt; Previous
            </button>
          )}
        </div>
        
        {page === 1 ? (
          <button 
            onClick={() => setPage(2)}
            className="px-6 py-1.5 border border-gray-400 bg-white hover:bg-gray-50 text-[13px] rounded flex items-center gap-1 shadow-sm font-semibold"
          >
            Next &gt;
          </button>
        ) : (
          <button 
            onClick={handleBegin}
            disabled={!agreed}
            className={`px-8 py-2 border rounded font-semibold text-[14px] shadow-sm transition-colors ${
              agreed 
                ? 'bg-[#5bc0de] hover:bg-[#46b8da] text-white border-[#46b8da] cursor-pointer' 
                : 'bg-[#999999] text-white border-[#777777] cursor-not-allowed'
            }`}
          >
            I am ready to begin
          </button>
        )}
      </div>
      
      {/* Footer Version */}
      <div className="h-5 bg-[#5e7d9b] text-white text-[11px] flex items-center justify-center border-t border-gray-400 font-bold">
        Version : 17.07.00
      </div>
    </div>
  )
}
