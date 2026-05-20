import React, { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useExamStore } from '../store/examStore'

export default function NtaLogin() {
  const navigate = useNavigate()
  const { testId } = useParams()
  const [username, setUsername] = useState('11111')
  const [password, setPassword] = useState('password')

  const handleSignIn = (e) => {
    e.preventDefault()
    // Normally we'd authenticate, but since they are already auth'd via Spring Boot,
    // this is just a mock step to simulate the NTA flow.
    navigate(`/student/tests/${testId}/instructions`)
  }

  return (
    <div className="h-screen flex flex-col bg-white font-sans select-none">
      {/* BAND 1: White Header */}
      <div className="flex justify-between items-center px-4 py-2 bg-white border-b-4 border-[#2d4a6b]">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 bg-white flex items-center justify-center border border-gray-300 rounded text-[10px] text-center font-bold text-gray-500">
            {/* Mock GATE Logo */}
            <img src="https://upload.wikimedia.org/wikipedia/en/thumb/1/18/GATE_2021_Logo.png/150px-GATE_2021_Logo.png" alt="GATE Logo" className="object-contain h-full w-full opacity-50" onError={(e) => e.target.style.display='none'} />
          </div>
        </div>
        <div className="flex flex-col items-center flex-1 text-center mt-1">
          <h1 className="text-[#4b0082] text-xl font-bold uppercase tracking-wide">Graduate Aptitude Test in Engineering (GATE 2026)</h1>
          <h2 className="text-[#009688] text-sm font-semibold tracking-wide">Organizing Institute : INDIAN INSTITUTE OF TECHNOLOGY GUWAHATI</h2>
        </div>
        <div className="flex items-center">
           <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center border border-gray-300 text-[8px] text-center font-bold text-gray-500">
             {/* Mock IIT Logo */}
           </div>
        </div>
      </div>

      {/* BAND 2: Grey System Info Band */}
      <div className="bg-[#757575] text-white flex justify-between relative px-6 py-4 shadow-sm border-b-2 border-white">
        <div className="flex flex-col">
          <div className="flex items-baseline gap-2">
            <span className="text-[15px]">System Name :</span>
          </div>
          <div className="text-[#ffeb3b] text-4xl font-semibold mb-2">C001</div>
          <div className="text-xs">Kindly contact the invigilator if there are any discrepancies in the Name and Photograph displayed on the screen or if the photograph is not yours</div>
        </div>
        
        <div className="flex flex-col text-right mr-32">
          <div className="text-[15px]">Candidate Name :</div>
          <div className="text-[#ffeb3b] text-3xl font-semibold mb-1">John Smith</div>
          <div className="text-sm">Subject : <span className="text-white">Mock Exam</span></div>
        </div>
        
        {/* Absolute positioned candidate photo overlapping the grey bar */}
        <div className="absolute right-6 top-2 w-[110px] h-[130px] bg-white border border-gray-400 flex items-center justify-center p-1 shadow-md">
          <img src="https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png" alt="Candidate" className="w-full h-full object-cover" />
        </div>
      </div>

      {/* Main Login Area */}
      <div className="flex-1 flex justify-center mt-20 relative z-0">
        <div className="w-[450px] bg-[#f5f5f5] border border-[#e0e0e0] shadow-sm flex flex-col rounded-sm">
          <div className="bg-[#e0e0e0] py-2 px-4 text-sm font-bold text-gray-700 border-b border-[#cccccc]">
            Login
          </div>
          <div className="p-8 flex flex-col gap-6 bg-[#f5f5f5]">
            <div className="flex flex-col gap-5">
              <div className="flex items-center">
                <div className="w-10 h-10 bg-white border border-[#ccc] border-r-0 flex items-center justify-center text-gray-500 text-lg">
                  👤
                </div>
                <input 
                  type="text" 
                  value={username}
                  autoComplete="off"
                  onChange={(e) => setUsername(e.target.value)}
                  className="flex-1 h-10 border border-[#ccc] px-3 focus:outline-none focus:border-blue-400 bg-white text-gray-700"
                />
                <div className="w-10 h-10 bg-[#e0e0e0] border border-[#ccc] border-l-0 flex items-center justify-center cursor-pointer text-gray-600">
                  ⌨️
                </div>
              </div>
              
              <div className="flex items-center">
                <div className="w-10 h-10 bg-white border border-[#ccc] border-r-0 flex items-center justify-center text-gray-500 text-lg">
                  🔒
                </div>
                <input 
                  type="text" 
                  value={password}
                  autoComplete="off"
                  onChange={(e) => setPassword(e.target.value)}
                  style={{ WebkitTextSecurity: 'disc' }}
                  className="flex-1 h-10 border border-[#ccc] px-3 focus:outline-none focus:border-blue-400 bg-white text-gray-700"
                />
                <div className="w-10 h-10 bg-[#e0e0e0] border border-[#ccc] border-l-0 flex items-center justify-center cursor-pointer text-gray-600">
                  ⌨️
                </div>
              </div>

              <div className="flex justify-center mt-4">
                <button 
                  type="button"
                  onClick={handleSignIn}
                  className="w-full py-2 bg-[#42a5f5] hover:bg-[#2196f3] text-white font-semibold text-[15px] border border-[#1e88e5] rounded-[2px]"
                >
                  Sign In
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      {/* Footer */}
      <div className="h-6 bg-[#757575] text-white text-xs flex items-center justify-center border-t border-gray-400">
        Version 17.05.21
      </div>
    </div>
  )
}
