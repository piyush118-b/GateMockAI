import React from 'react'
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom'

// Auth
import Login from './components/Login'
import Register from './components/Register'

// Student Portal
import StudentDashboard from './components/StudentDashboard'
import StudentTestsList from './components/StudentTestsList'
import NtaLogin from './components/NtaLogin'
import NtaInstructions from './components/NtaInstructions'
import ExamConsole from './components/ExamConsole'
import ScorecardResult from './components/ScorecardResult'
import AttemptAnalytics from './components/AttemptAnalytics'

// Admin Portal
import AdminDashboard from './components/AdminDashboard'
import AdminRag from './components/AdminRag'
import AdminTestEdit from './components/AdminTestEdit'
import AdminTestExport from './components/AdminTestExport'
import WeightedGenerator from './components/WeightedGenerator'
import SseProgressCompiler from './components/SseProgressCompiler'
import AdminAnalytics from './components/AdminAnalytics'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* ─── AUTH ─── */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* ─── STUDENT PORTAL ─── */}
        <Route path="/student/dashboard" element={<StudentDashboard />} />
        <Route path="/student/tests" element={<StudentTestsList />} />

        {/* NTA Pixel-Perfect Exam Flow */}
        <Route path="/student/tests/:testId/login" element={<NtaLogin />} />
        <Route path="/student/tests/:testId/instructions" element={<NtaInstructions />} />
        <Route path="/student/tests/:testId/take" element={<ExamConsole />} />
        <Route path="/student/attempts/:attemptId/result" element={<ScorecardResult />} />
        <Route path="/student/attempts/:attemptId/analytics" element={<AttemptAnalytics />} />

        {/* ─── ADMIN PORTAL ─── */}
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
        <Route path="/admin/rag" element={<AdminRag />} />
        <Route path="/admin/tests/:testId/edit" element={<AdminTestEdit />} />
        <Route path="/admin/tests/:testId/export" element={<AdminTestExport />} />
        <Route path="/admin/weighted-generator" element={<WeightedGenerator />} />
        <Route path="/admin/generate/progress" element={<SseProgressCompiler />} />
        <Route path="/admin/analytics" element={<AdminAnalytics />} />
        <Route path="/admin/analytics/test/:testId" element={
          <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6 text-center font-sans">
            <div className="bg-white border border-gray-200 rounded-lg p-6 max-w-sm shadow-sm">
              <h3 className="text-gray-800 font-extrabold uppercase text-xs">Mock Test In-depth Analytics</h3>
              <p className="text-xs text-gray-500 mt-2">
                Detailed question-level analysis and response statistics for test UUID:
              </p>
              <p className="text-[10px] text-indigo-600 font-mono mt-1 select-all break-all bg-indigo-50 p-2 rounded border border-indigo-100">
                {window.location.pathname.split('/').pop()}
              </p>
              <Link 
                to="/admin/analytics" 
                className="mt-5 inline-flex items-center gap-1 text-[10px] font-extrabold text-white bg-indigo-600 hover:bg-indigo-700 px-4 py-2 rounded uppercase tracking-wider transition-all duration-150"
              >
                Back to Analytics
              </Link>
            </div>
          </div>
        } />

        {/* ─── FALLBACK ─── */}
        <Route path="*" element={<Navigate to="/student/tests" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
