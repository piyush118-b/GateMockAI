import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

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

// Admin Portal
import AdminDashboard from './components/AdminDashboard'
import AdminRag from './components/AdminRag'
import AdminTestEdit from './components/AdminTestEdit'
import AdminTestExport from './components/AdminTestExport'
import WeightedGenerator from './components/WeightedGenerator'
import SseProgressCompiler from './components/SseProgressCompiler'

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

        {/* ─── ADMIN PORTAL ─── */}
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
        <Route path="/admin/rag" element={<AdminRag />} />
        <Route path="/admin/tests/:testId/edit" element={<AdminTestEdit />} />
        <Route path="/admin/tests/:testId/export" element={<AdminTestExport />} />
        <Route path="/admin/weighted-generator" element={<WeightedGenerator />} />
        <Route path="/admin/generate/progress" element={<SseProgressCompiler />} />

        {/* ─── FALLBACK ─── */}
        <Route path="*" element={<Navigate to="/student/tests" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
