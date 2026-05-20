/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "../src/main/resources/templates/**/*.html",
  ],
  theme: {
    extend: {
      colors: {
        nta: {
          bg: '#f0f0f0', // Page background
          header: '#ffffff', // Header background
          subheader: '#2d4a6b', // Sub-header bar
          teal: '#009688', // Section active tab
          blue: '#1565c0', // Save & Next button
          border: '#cccccc', // General border
          sidebar: '#f5f5f5', // Right sidebar bg
          
          // Palette state colors
          notVisited: '#ffffff', 
          notAnswered: '#e55a2b', 
          answered: '#2e7d32', 
          marked: '#6a1b9a', 
          markedAnswered: '#6a1b9a' 
        }
      },
      fontFamily: {
        sans: ['"Times New Roman"', 'Times', 'serif'], // NTA uses browser default serif often, or basic sans. 
      },
      borderRadius: {
        nta: '4px' // Boxy, not rounded
      }
    }
  },
  plugins: [],
}
