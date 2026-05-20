import React from 'react'
import { Delete, X } from 'lucide-react'

export default function NATInput({ questionId, value = '', onChange }) {
  const handleKeyPress = (char) => {
    let newValue = value;

    if (char === 'clear') {
      newValue = '';
    } else if (char === 'backspace') {
      newValue = value.slice(0, -1);
    } else if (char === '-') {
      if (value.startsWith('-')) {
        newValue = value.slice(1);
      } else {
        newValue = '-' + value;
      }
    } else if (char === '.') {
      if (!value.includes('.')) {
        newValue = value + '.';
      }
    } else {
      // Prevent entering characters if they are invalid or exceed length
      if (value.replace('-', '').replace('.', '').length < 10) {
        newValue = value + char;
      }
    }

    onChange(newValue);
  };

  const keys = [
    ['1', '2', '3'],
    ['4', '5', '6'],
    ['7', '8', '9'],
    ['0', '.', '-']
  ];

  return (
    <div className="flex flex-col gap-3 p-4 bg-gray-50 border border-gray-200 rounded-[4px] max-w-[280px] shadow-sm select-none">
      <div className="flex flex-col">
        <span className="text-[10px] text-gray-500 uppercase tracking-wider font-semibold">Virtual Entry Pad</span>
        <div className="flex gap-1 items-center bg-white border-2 border-blue-500 rounded-[4px] px-3 py-2 mt-1 shadow-inner min-h-[44px]">
          <span className="font-mono text-lg font-bold text-gray-800 tracking-wide select-none">
            {value || <span className="text-gray-300 font-normal italic text-sm">Enter numerical value...</span>}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-2">
        {keys.map((row, rIdx) => 
          row.map((key) => (
            <button
              key={`${rIdx}-${key}`}
              type="button"
              onClick={() => handleKeyPress(key)}
              className="bg-white hover:bg-blue-50 active:bg-blue-100 border border-gray-300 active:border-blue-400 text-gray-800 font-mono font-bold text-base py-2 rounded-[4px] shadow-sm cursor-pointer transition-all duration-150 h-11 flex items-center justify-center"
            >
              {key}
            </button>
          ))
        )}

        <button
          type="button"
          onClick={() => handleKeyPress('backspace')}
          className="bg-orange-50 hover:bg-orange-100 border border-orange-300 active:border-orange-400 text-orange-700 font-bold py-2 rounded-[4px] shadow-sm cursor-pointer transition-all duration-150 h-11 flex items-center justify-center gap-1"
        >
          <Delete className="w-4 h-4" />
          <span className="text-xs uppercase">Bksp</span>
        </button>

        <button
          type="button"
          onClick={() => handleKeyPress('clear')}
          className="col-span-2 bg-red-50 hover:bg-red-100 border border-red-300 active:border-red-400 text-red-700 font-bold py-2 rounded-[4px] shadow-sm cursor-pointer transition-all duration-150 h-11 flex items-center justify-center gap-1"
        >
          <X className="w-4 h-4" />
          <span className="text-xs uppercase">Clear Entry</span>
        </button>
      </div>
      <p className="text-[10px] text-gray-400 leading-tight italic">
        * Please use this virtual keypad exclusively. Keyboard injections are monitored.
      </p>
    </div>
  )
}
