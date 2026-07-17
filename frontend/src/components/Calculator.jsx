import React, { useState, useRef, useEffect } from 'react'
import { useExamStore } from '../store/examStore'

export default function Calculator() {
  const { isCalculatorVisible } = useExamStore()
  const [position, setPosition] = useState({ x: 250, y: 120 })
  const [isDragging, setIsDragging] = useState(false)
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 })
  
  const [display, setDisplay] = useState('0')
  const [expression, setExpression] = useState('')
  const [isDeg, setIsDeg] = useState(true)
  const [memory, setMemory] = useState(0)
  
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

  const handleBackspace = () => {
    if (display.length > 1) {
      setDisplay(display.slice(0, -1))
    } else {
      setDisplay('0')
    }
  }

  const handleEvaluate = () => {
    try {
      // Replace symbols for evaluation
      let evalStr = display
        .replace(/×/g, '*')
        .replace(/÷/g, '/')
        .replace(/mod/g, '%')
        .replace(/π/g, 'Math.PI')
        .replace(/e/g, 'Math.E');

      // Simple evaluation
      // eslint-disable-next-line no-eval
      const result = (0, eval)(evalStr);
      setDisplay(String(Number(result.toFixed(10))));
      setExpression(display + ' =');
    } catch (e) {
      setDisplay('Error')
    }
  }

  // Handle immediate unary scientific actions
  const handleScientific = (type) => {
    try {
      let currentVal = parseFloat(display);
      if (isNaN(currentVal)) return;

      let result;
      switch (type) {
        case 'sin':
          result = Math.sin(isDeg ? (currentVal * Math.PI) / 180 : currentVal);
          break;
        case 'cos':
          result = Math.cos(isDeg ? (currentVal * Math.PI) / 180 : currentVal);
          break;
        case 'tan':
          result = Math.tan(isDeg ? (currentVal * Math.PI) / 180 : currentVal);
          break;
        case 'sin-1':
          result = Math.asin(currentVal);
          if (isDeg) result = (result * 180) / Math.PI;
          break;
        case 'cos-1':
          result = Math.acos(currentVal);
          if (isDeg) result = (result * 180) / Math.PI;
          break;
        case 'tan-1':
          result = Math.atan(currentVal);
          if (isDeg) result = (result * 180) / Math.PI;
          break;
        case 'sinh':
          result = Math.sinh(currentVal);
          break;
        case 'cosh':
          result = Math.cosh(currentVal);
          break;
        case 'tanh':
          result = Math.tanh(currentVal);
          break;
        case 'sinh-1':
          result = Math.asinh(currentVal);
          break;
        case 'cosh-1':
          result = Math.acosh(currentVal);
          break;
        case 'tanh-1':
          result = Math.atanh(currentVal);
          break;
        case 'ln':
          result = Math.log(currentVal);
          break;
        case 'log':
          result = Math.log10(currentVal);
          break;
        case 'sqrt':
          result = Math.sqrt(currentVal);
          break;
        case '1/x':
          result = 1 / currentVal;
          break;
        case 'x^2':
          result = Math.pow(currentVal, 2);
          break;
        case 'x^3':
          result = Math.pow(currentVal, 3);
          break;
        case 'e^x':
          result = Math.exp(currentVal);
          break;
        case '10^x':
          result = Math.pow(10, currentVal);
          break;
        case 'abs':
          result = Math.abs(currentVal);
          break;
        case 'fact':
          // Factorial
          let f = 1;
          for (let i = 1; i <= Math.floor(currentVal); i++) f *= i;
          result = f;
          break;
        default:
          return;
      }
      
      setDisplay(String(Number(result.toFixed(10))));
      setExpression(`${type}(${currentVal})`);
    } catch (e) {
      setDisplay('Error');
    }
  }

  // Memory functions
  const handleMemory = (op) => {
    const val = parseFloat(display) || 0;
    switch (op) {
      case 'MC':
        setMemory(0);
        break;
      case 'MR':
        setDisplay(String(memory));
        break;
      case 'MS':
        setMemory(val);
        break;
      case 'M+':
        setMemory(memory + val);
        break;
      case 'M-':
        setMemory(memory - val);
        break;
      default:
        break;
    }
  }

  return (
    <div 
      ref={calcRef}
      className="fixed z-[9999] bg-[#d9d9d9] border border-[#a8a8a8] shadow-2xl rounded flex flex-col font-sans select-none"
      style={{ left: `${position.x}px`, top: `${position.y}px`, width: '540px' }}
    >
      {/* Title Bar - Draggable */}
      <div 
        className="bg-[#2a75d3] text-white px-3 py-1.5 flex justify-between items-center cursor-move text-[13px] font-bold shrink-0 border-b border-[#1b5cb2]"
        onMouseDown={handleMouseDown}
      >
        <span>Scientific Calculator</span>
        <div className="flex items-center gap-1.5">
          <button 
            type="button"
            className="bg-[#5bc0de] text-white text-[10px] font-bold px-2 py-0.5 rounded border border-[#2e9cb7] hover:bg-[#31b0d5]"
            onClick={() => alert('Scientific Calculator Help')}
          >
            Help
          </button>
          <span className="text-white hover:text-gray-300 font-bold px-1 text-sm cursor-pointer" onClick={() => useExamStore.setState({ isCalculatorVisible: false })}>&minus;</span>
          <span className="text-white hover:text-red-300 font-bold px-1 text-sm cursor-pointer" onClick={() => useExamStore.setState({ isCalculatorVisible: false })}>&#10005;</span>
        </div>
      </div>

      {/* Screen Displays */}
      <div className="p-3 bg-[#e6e6e6] flex flex-col gap-1 shrink-0 border-b border-[#c8c8c8]">
        <input 
          type="text" 
          value={expression} 
          disabled 
          className="w-full h-6 bg-white text-right px-2 py-0.5 text-xs font-mono text-gray-500 border border-[#b8b8b8] outline-none select-none" 
        />
        <input 
          type="text" 
          value={display} 
          disabled 
          className="w-full h-8 bg-white text-right px-2 py-1 text-[16px] font-mono font-bold text-black border border-[#b8b8b8] outline-none select-none" 
        />
      </div>

      {/* Buttons Area */}
      <div className="p-2.5 bg-[#d9d9d9] flex flex-col gap-1.5 text-[11px] shrink-0 font-medium">
        
        {/* Row 1: mod, Radios, Memory */}
        <div className="flex gap-1.5 items-center w-full justify-between">
          <button onClick={() => handleInput('mod')} className="w-11 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">mod</button>
          
          <div className="flex gap-2 items-center border border-[#b8b8b8] bg-[#ececec] px-2 py-0.5 rounded-sm text-[10px]">
            <label className="flex items-center gap-1 cursor-pointer">
              <input type="radio" checked={isDeg} onChange={() => setIsDeg(true)} className="w-3 h-3 cursor-pointer" />
              <span>Deg</span>
            </label>
            <label className="flex items-center gap-1 cursor-pointer">
              <input type="radio" checked={!isDeg} onChange={() => setIsDeg(false)} className="w-3 h-3 cursor-pointer" />
              <span>Rad</span>
            </label>
          </div>

          <div className="flex gap-1 shrink-0">
            {['MC', 'MR', 'MS', 'M+', 'M-'].map(op => (
              <button key={op} onClick={() => handleMemory(op)} className="w-10 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">{op}</button>
            ))}
          </div>
        </div>

        {/* Row 2: hyperbolic, Exp, brackets, actions */}
        <div className="flex gap-1.5 w-full justify-between">
          {['sinh', 'cosh', 'tanh'].map(fn => (
            <button key={fn} onClick={() => handleScientific(fn)} className="w-11 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">{fn}</button>
          ))}
          <button onClick={() => handleInput('e')} className="w-11 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">Exp</button>
          <button onClick={() => handleInput('(')} className="w-11 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">(</button>
          <button onClick={() => handleInput(')')} className="w-11 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">)</button>

          <div className="flex gap-1 shrink-0 ml-auto">
            <button onClick={handleBackspace} className="w-10 h-7 bg-[#c9302c] border border-[#ac2925] text-white rounded-sm hover:bg-[#d9534f] font-bold text-center text-sm flex items-center justify-center">&#8592;</button>
            <button onClick={handleClear} className="w-10 h-7 bg-[#c9302c] border border-[#ac2925] text-white rounded-sm hover:bg-[#d9534f] font-bold text-center">C</button>
            <button onClick={() => setDisplay(display.startsWith('-') ? display.slice(1) : '-' + display)} className="w-10 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">&plusmn;</button>
            <button onClick={() => handleScientific('sqrt')} className="w-10 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">&#8730;</button>
          </div>
        </div>

        {/* Main Grid splitting scientific (cols 1-6) and number pad (cols 7-11) */}
        <div className="flex gap-1.5 w-full">
          {/* Scientific buttons on left */}
          <div className="flex flex-col gap-1.5 w-[294px]">
            {/* Row 3 scientific */}
            <div className="flex gap-1">
              {['sinh-1', 'cosh-1', 'tanh-1'].map(fn => (
                <button key={fn} onClick={() => handleScientific(fn)} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold text-[9px]">{fn}</button>
              ))}
              <button onClick={() => handleInput('log')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">log<sub>y</sub>x</button>
              <button onClick={() => handleScientific('ln')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">ln</button>
              <button onClick={() => handleScientific('log')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">log</button>
            </div>
            {/* Row 4 scientific */}
            <div className="flex gap-1">
              <button onClick={() => handleInput('π')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">&#960;</button>
              <button onClick={() => handleInput('e')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">e</button>
              <button onClick={() => handleScientific('fact')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">n!</button>
              <button onClick={() => handleScientific('log')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">log<sub>2</sub>x</button>
              <button onClick={() => handleScientific('e^x')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">e<sup>x</sup></button>
              <button onClick={() => handleScientific('10^x')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">10<sup>x</sup></button>
            </div>
            {/* Row 5 scientific */}
            <div className="flex gap-1">
              {['sin', 'cos', 'tan'].map(fn => (
                <button key={fn} onClick={() => handleScientific(fn)} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">{fn}</button>
              ))}
              <button onClick={() => handleInput('**')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">x<sup>y</sup></button>
              <button onClick={() => handleScientific('x^3')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">x<sup>3</sup></button>
              <button onClick={() => handleScientific('x^2')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">x<sup>2</sup></button>
            </div>
            {/* Row 6 scientific */}
            <div className="flex gap-1">
              {['sin-1', 'cos-1', 'tan-1'].map(fn => (
                <button key={fn} onClick={() => handleScientific(fn)} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold text-[9px]">{fn}</button>
              ))}
              <button onClick={() => handleInput('**0.5')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold"><sup>y</sup>&#8730;x</button>
              <button onClick={() => handleScientific('x^3')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold"><sup>3</sup>&#8730;x</button>
              <button onClick={() => handleScientific('abs')} className="w-[47px] h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">|x|</button>
            </div>
          </div>

          {/* Number pad & Operators on right */}
          <div className="flex-1 flex flex-col gap-1.5">
            {/* Pad row 1 */}
            <div className="flex gap-1">
              {['7', '8', '9'].map(num => (
                <button key={num} onClick={() => handleInput(num)} className="w-10 h-7 bg-white border border-[#b8b8b8] rounded-sm hover:bg-[#f0f0f0] font-bold text-black text-xs">{num}</button>
              ))}
              <button onClick={() => handleInput('÷')} className="w-10 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">/</button>
              <button onClick={() => handleInput('%')} className="w-10 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">%</button>
            </div>
            {/* Pad row 2 */}
            <div className="flex gap-1">
              {['4', '5', '6'].map(num => (
                <button key={num} onClick={() => handleInput(num)} className="w-10 h-7 bg-white border border-[#b8b8b8] rounded-sm hover:bg-[#f0f0f0] font-bold text-black text-xs">{num}</button>
              ))}
              <button onClick={() => handleInput('×')} className="w-10 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">*</button>
              <button onClick={() => handleScientific('1/x')} className="w-10 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">1/x</button>
            </div>
            {/* Pad rows 3 & 4 with vertical Equal button */}
            <div className="flex gap-1 w-full">
              <div className="flex flex-col gap-1.5 flex-1">
                {/* 1, 2, 3, - */}
                <div className="flex gap-1">
                  {['1', '2', '3'].map(num => (
                    <button key={num} onClick={() => handleInput(num)} className="w-10 h-7 bg-white border border-[#b8b8b8] rounded-sm hover:bg-[#f0f0f0] font-bold text-black text-xs">{num}</button>
                  ))}
                  <button onClick={() => handleInput('-')} className="w-10 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">-</button>
                </div>
                {/* 0, ., + */}
                <div className="flex gap-1">
                  <button onClick={() => handleInput('0')} className="w-[84px] h-7 bg-white border border-[#b8b8b8] rounded-sm hover:bg-[#f0f0f0] font-bold text-black text-xs text-center">0</button>
                  <button onClick={() => handleInput('.')} className="w-10 h-7 bg-white border border-[#b8b8b8] rounded-sm hover:bg-[#f0f0f0] font-bold text-black text-xs">.</button>
                  <button onClick={() => handleInput('+')} className="w-10 h-7 bg-[#ececec] border border-[#b8b8b8] rounded-sm hover:bg-[#e0e0e0] font-bold">+</button>
                </div>
              </div>
              
              {/* Tall vertical equal button */}
              <button onClick={handleEvaluate} className="w-10 h-[61px] bg-[#2e7d32] border border-[#1b5e20] text-white hover:bg-[#4caf50] rounded-sm font-bold flex items-center justify-center text-sm shadow-sm">=</button>
            </div>
          </div>
        </div>

      </div>
    </div>
  )
}

