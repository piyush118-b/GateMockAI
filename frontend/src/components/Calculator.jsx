import React, { useState, useRef, useEffect } from 'react'
import { useExamStore } from '../store/examStore'

export default function Calculator() {
  const { isCalculatorVisible } = useExamStore()
  const [position, setPosition] = useState({ x: 200, y: 150 })
  const [isDragging, setIsDragging] = useState(false)
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 })
  const [display, setDisplay] = useState('0')
  const [expression, setExpression] = useState('')
  
  const calcRef = useRef(null)

  useEffect(() => {
    const handleMouseMove = (e) => {
      if (isDragging) {
        setPosition({
          x: e.clientX - dragOffset.x,
          y: e.clientY - dragOffset.y
        })
      }
    }
    const handleMouseUp = () => setIsDragging(false)
    
    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)
    }
    return () => {
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
    }
  }, [isDragging, dragOffset])

  if (!isCalculatorVisible) return null

  const handleMouseDown = (e) => {
    setIsDragging(true)
    setDragOffset({
      x: e.clientX - position.x,
      y: e.clientY - position.y
    })
  }

  const handleInput = (val) => {
    if (display === '0' || display === 'Error') {
      setDisplay(val)
    } else {
      setDisplay(display + val)
    }
  }

  const handleClear = () => {
    setDisplay('0')
    setExpression('')
  }

  const handleEvaluate = () => {
    try {
      // Basic evaluation for mock purposes
      // eslint-disable-next-line no-eval
      const result = eval(display.replace('×', '*').replace('÷', '/'))
      setDisplay(String(result))
      setExpression(display + ' =')
    } catch (e) {
      setDisplay('Error')
    }
  }

  const buttons = [
    ['C', '(', ')', '÷'],
    ['7', '8', '9', '×'],
    ['4', '5', '6', '-'],
    ['1', '2', '3', '+'],
    ['0', '.', '±', '=']
  ]

  const handleButtonClick = (btn) => {
    if (btn === 'C') handleClear()
    else if (btn === '=') handleEvaluate()
    else if (btn === '±') {
      if (display !== '0' && display !== 'Error') {
        setDisplay(display.startsWith('-') ? display.slice(1) : '-' + display)
      }
    }
    else handleInput(btn)
  }

  return (
    <div 
      ref={calcRef}
      className="fixed z-[9999] bg-[#e6e6e6] border-[2px] border-[#333] shadow-lg rounded-sm overflow-hidden flex flex-col font-sans select-none"
      style={{ left: `${position.x}px`, top: `${position.y}px`, width: '280px' }}
    >
      {/* Title Bar - Draggable */}
      <div 
        className="bg-[#2d4a6b] text-white px-2 py-1 flex justify-between items-center cursor-move text-xs font-bold"
        onMouseDown={handleMouseDown}
      >
        <span>Scientific Calculator</span>
        <button 
          onClick={() => useExamStore.setState({ isCalculatorVisible: false })}
          className="hover:text-red-300 font-bold px-1 cursor-pointer"
        >
          X
        </button>
      </div>

      {/* Calculator Body */}
      <div className="p-3 bg-[#c3c3c3] border-t border-gray-400">
        {/* Display */}
        <div className="bg-[#f2f2f2] border-[2px] border-[#999] rounded-sm p-1.5 h-14 mb-3 flex flex-col items-end justify-between overflow-hidden shadow-inner">
          <div className="text-[10px] text-gray-500 font-mono tracking-widest">{expression}</div>
          <div className="text-xl font-bold font-mono tracking-widest">{display}</div>
        </div>

        {/* Buttons Grid */}
        <div className="flex flex-col gap-1.5">
          {buttons.map((row, i) => (
            <div key={i} className="flex justify-between gap-1.5">
              {row.map(btn => (
                <button
                  key={btn}
                  onClick={() => handleButtonClick(btn)}
                  className={`flex-1 h-8 rounded-sm font-bold text-sm border-b-[2px] border-r-[2px] active:border-b-0 active:border-r-0 active:translate-x-[1px] active:translate-y-[1px] ${
                    ['÷', '×', '-', '+', '='].includes(btn) 
                      ? 'bg-[#337ab7] border-[#2e6da4] text-white hover:bg-[#286090]' 
                      : btn === 'C' 
                        ? 'bg-[#d9534f] border-[#d43f3a] text-white hover:bg-[#c9302c]'
                        : 'bg-[#f5f5f5] border-[#ccc] text-black hover:bg-[#e6e6e6]'
                  }`}
                >
                  {btn}
                </button>
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
