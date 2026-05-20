import React from 'react'
import { useExamStore } from '../store/examStore'

export default function NtaHeader({ title = "CS 1 Computer Science and Information Technology Mock" }) {
  const { attempt } = useExamStore()
  const candidateName = attempt?.user?.fullName || 'John Smith'

  return (
    <div className="flex flex-col w-full select-none font-sans">
      {/* BAND 1: White Header */}
      <div className="flex justify-between items-center px-4 py-2 bg-white border-b-2 border-gray-300">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 bg-gray-100 flex items-center justify-center border border-gray-300 rounded text-[10px] text-center font-bold text-gray-500">
            {/* Mock GATE Logo */}
            GATE<br/>2026
          </div>
        </div>
        <div className="flex flex-col items-center flex-1 text-center">
          <h1 className="text-[#4b0082] text-xl font-bold uppercase">Graduate Aptitude Test in Engineering (GATE 2026)</h1>
          <h2 className="text-[#0000ff] text-sm font-semibold">Organizing Institute : INDIAN INSTITUTE OF TECHNOLOGY GUWAHATI</h2>
        </div>
        <div className="flex items-center">
           <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center border border-gray-300 text-[8px] text-center font-bold text-gray-500">
             {/* Mock IIT Logo */}
             IIT<br/>LOGO
           </div>
        </div>
      </div>

      {/* BAND 2: Sub-header */}
      <div className="bg-nta-subheader flex justify-between items-center px-4 py-1.5 h-9">
        <div className="text-white text-[13px] font-bold">
          {title}
        </div>
        <div className="flex gap-2">
          {/* We'll add the specific right-aligned items based on context if needed, or leave empty if passed as children */}
        </div>
      </div>
    </div>
  )
}
