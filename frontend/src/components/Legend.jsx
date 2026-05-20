import React from 'react'

export default function Legend() {
  return (
    <div className="bg-white border-t border-b border-gray-300 p-2 select-none font-sans text-xs">
      <table className="w-full text-[11px] leading-tight border-separate border-spacing-y-2">
        <tbody>
          <tr>
            <td className="w-10">
              <div className="w-7 h-7 bg-white border border-gray-400 text-black rounded-none flex items-center justify-center font-bold">1</div>
            </td>
            <td className="w-2/5">Not Visited</td>
            <td className="w-10">
              <div className="w-7 h-7 bg-[#e55a2b] text-white rounded-[50%_0_50%_50%] transform rotate-45 flex items-center justify-center font-bold mx-auto">
                <span className="-rotate-45">2</span>
              </div>
            </td>
            <td>Not Answered</td>
          </tr>
          <tr>
            <td>
              <div className="w-7 h-7 bg-[#2e7d32] text-white rounded-[50%_50%_0_50%] transform rotate-45 flex items-center justify-center font-bold mx-auto">
                <span className="-rotate-45">3</span>
              </div>
            </td>
            <td>Answered</td>
            <td>
              <div className="w-7 h-7 bg-[#6a1b9a] text-white rounded-full flex items-center justify-center font-bold mx-auto">4</div>
            </td>
            <td>Marked for Review</td>
          </tr>
          <tr>
            <td>
              <div className="w-7 h-7 bg-[#6a1b9a] text-white rounded-full flex items-center justify-center font-bold relative mx-auto">
                5
                <span className="absolute -bottom-0.5 -right-0.5 w-2 h-2 bg-[#2e7d32] rounded-full border border-white"></span>
              </div>
            </td>
            <td colSpan="3" className="pl-1">
              Answered & Marked for Review (will be considered for evaluation)
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}
