import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import StudentDashboard from './components/StudentDashboard'
import StudentTestsList from './components/StudentTestsList'
import NtaLogin from './components/NtaLogin'
import NtaInstructions from './components/NtaInstructions'
import ExamConsole from './components/ExamConsole'
import ScorecardResult from './components/ScorecardResult'
import AdminDashboard from './components/AdminDashboard'
import WeightedGenerator from './components/WeightedGenerator'
import SseProgressCompiler from './components/SseProgressCompiler'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* STUDENT PORTAL VIEWS */}
        <Route path="/student/dashboard" element={<StudentDashboard />} />
        <Route path="/student/tests" element={<StudentTestsList />} />
        
        {/* NTA PIXEL-PERFECT EXAM FLOW */}
        <Route path="/student/tests/:testId/login" element={<NtaLogin />} />
        <Route path="/student/tests/:testId/instructions" element={<NtaInstructions />} />
        <Route path="/student/tests/:testId/take" element={<ExamConsole />} />
        
        <Route path="/student/attempts/:attemptId/result" element={<ScorecardResult />} />

        {/* ADMIN PORTAL VIEWS */}
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
        <Route path="/admin/weighted-generator" element={<WeightedGenerator />} />
        <Route path="/admin/generate/progress" element={<SseProgressCompiler />} />

        {/* FALLBACK REDIRECT - default student home */}
        <Route path="*" element={<Navigate to="/student/tests" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
