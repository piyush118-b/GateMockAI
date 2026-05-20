import React, { useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useExamStore } from '../store/examStore'
import NtaHeader from './NtaHeader'
import QuestionCard from './QuestionCard'
import ActionBar from './ActionBar'
import Sidebar from './Sidebar'
import FullscreenGuard from './FullscreenGuard'
import SubmitModal from './SubmitModal'
import Calculator from './Calculator'
import { Loader2 } from 'lucide-react'

export default function ExamConsole() {
  const { testId } = useParams()
  const { loadSession, isLoaded, error } = useExamStore()

  useEffect(() => {
    if (testId) {
      loadSession(testId)
    }
  }, [testId, loadSession])

  // Prevent right-click context menu and copy-paste key combinations in secure zone
  useEffect(() => {
    const handleContextMenu = (e) => {
      e.preventDefault();
    };

    const handleKeyDown = (e) => {
      // Block Ctrl+C, Ctrl+V, Cmd+C, Cmd+V, Ctrl+U, F12, etc.
      if (
        (e.ctrlKey || e.metaKey) && 
        (e.key === 'c' || e.key === 'v' || e.key === 'u' || e.key === 'a')
      ) {
        e.preventDefault();
      }
      if (e.key === 'F12') {
        e.preventDefault();
      }
    };

    document.addEventListener('contextmenu', handleContextMenu);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('contextmenu', handleContextMenu);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  if (error) {
    return (
      <div className="min-h-screen bg-nta-bg flex items-center justify-center p-4 font-sans">
        <div className="bg-red-50 border border-red-200 p-6 text-center shadow">
          <h3 className="text-red-700 font-bold text-lg">Launch Failed</h3>
          <p className="text-sm text-red-600 mt-2">{error}</p>
          <button 
            type="button"
            onClick={() => window.location.reload()}
            className="mt-4 bg-nta-blue text-white font-bold text-xs px-4 py-2 border border-blue-800"
          >
            Retry Connection
          </button>
        </div>
      </div>
    )
  }

  if (!isLoaded) {
    return (
      <div className="min-h-screen bg-nta-bg flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-2">
          <Loader2 className="w-8 h-8 text-nta-blue animate-spin" />
          <span className="text-sm font-semibold text-gray-600">Initializing Secure Proctor Exam Console...</span>
        </div>
      </div>
    )
  }

  return (
    <div className="h-screen w-screen flex flex-col overflow-hidden bg-white font-sans select-none relative">
      {/* Proctor Fullscreen and Submission overlays */}
      <FullscreenGuard />
      <SubmitModal />
      <Calculator />

      {/* NTA HEADER PANEL (BAND 1 & 2) */}
      <NtaHeader title="CS 1 Computer Science and Information Technology Mock" />

      {/* CONSOLE MAIN SCREEN AREA (BAND 3) */}
      <div className="flex-1 flex overflow-hidden min-h-0 border-t border-[#bce8f1]">
        {/* LEFT COLUMN: ACTIVE QUESTION CARD & BOTTOM ACTION CONTROLS (83%) */}
        <div className="flex-[0.83] flex flex-col min-h-0 bg-white border-r border-gray-400">
          <div className="flex-1 overflow-y-auto min-h-0 relative nta-watermark">
            <QuestionCard />
          </div>
          <ActionBar />
        </div>

        {/* RIGHT COLUMN: PALETTE & CANDIDATE BIO (17%) */}
        <Sidebar />
      </div>
    </div>
  )
}
