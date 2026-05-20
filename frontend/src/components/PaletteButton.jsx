import React from 'react'

export default function PaletteButton({ index, status, isActive, onClick }) {
  const activeBorder = isActive 
    ? 'ring-[3px] ring-yellow-400 ring-offset-0 z-10' 
    : 'hover:opacity-90';

  const renderShape = () => {
    switch (status) {
      case 'NOT_ANSWERED':
        return (
          <div className={`w-8 h-8 bg-[#e55a2b] text-white rounded-[50%_0_50%_50%] transform rotate-45 flex items-center justify-center font-bold shadow-sm ${activeBorder}`}>
            <span className="-rotate-45">{index}</span>
          </div>
        )
      case 'ANSWERED':
        return (
          <div className={`w-8 h-8 bg-[#2e7d32] text-white rounded-[50%_50%_0_50%] transform rotate-45 flex items-center justify-center font-bold shadow-sm ${activeBorder}`}>
            <span className="-rotate-45">{index}</span>
          </div>
        )
      case 'MARKED':
        return (
          <div className={`w-8 h-8 bg-[#6a1b9a] text-white rounded-full flex items-center justify-center font-bold shadow-sm ${activeBorder}`}>
            <span>{index}</span>
          </div>
        )
      case 'MARKED_ANSWERED':
        return (
          <div className={`w-8 h-8 bg-[#6a1b9a] text-white rounded-full flex items-center justify-center font-bold relative shadow-sm ${activeBorder}`}>
            <span>{index}</span>
            <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-[#2e7d32] rounded-full border border-white" />
          </div>
        )
      case 'NOT_VISITED':
      default:
        return (
          <div className={`w-8 h-8 bg-white border border-gray-400 text-black rounded-none flex items-center justify-center font-bold shadow-sm ${activeBorder}`}>
            <span>{index}</span>
          </div>
        )
    }
  }

  return (
    <button
      type="button"
      onClick={onClick}
      className="w-10 h-10 flex items-center justify-center cursor-pointer bg-transparent border-none p-0 outline-none"
    >
      {renderShape()}
    </button>
  )
}
