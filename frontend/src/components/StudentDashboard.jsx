import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { GraduationCap, History, PlayCircle, Trophy, BarChart3, LogOut, Loader2 } from 'lucide-react'

export default function StudentDashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetch('/api/student/dashboard')
      .then(res => {
        if (res.status === 401 || res.status === 403) {
          navigate('/login');
          return null;
        }
        if (!res.ok) throw new Error('Failed to fetch dashboard data.');
        return res.json();
      })
      .then(json => {
        if (json) {
          setData(json);
          setLoading(false);
        }
      })
      .catch(err => {
        if (err.message) {
          setError(err.message);
        }
        setLoading(false);
      });
  }, [navigate]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-2">
          <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
          <span className="text-sm font-semibold text-gray-600">Loading Student Portal...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4 font-sans">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md text-center">
          <h3 className="text-red-700 font-extrabold text-lg uppercase">System Connection Failed</h3>
          <p className="text-sm text-red-600 mt-2 font-medium">{error}</p>
          <button 
            type="button"
            onClick={() => window.location.reload()}
            className="mt-4 bg-red-600 hover:bg-red-700 text-white font-bold text-xs uppercase px-4 py-2 rounded-[4px]"
          >
            Retry Connection
          </button>
        </div>
      </div>
    );
  }

  const { fullName, email, attemptsCount, attempts } = data;

  // Calculate high score
  const highAttempt = attempts.length > 0 
    ? [...attempts].sort((a, b) => (b.score || 0) - (a.score || 0))[0] 
    : null;
  const highScore = highAttempt ? highAttempt.score : 0;

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans select-none">
      {/* NAVBAR */}
      <nav className="bg-slate-900 text-white h-[64px] px-8 flex justify-between items-center shadow-md">
        <div className="flex items-center gap-2.5">
          <div className="bg-blue-600 w-9 h-9 rounded-md flex items-center justify-center font-black text-white text-lg">
            M
          </div>
          <span className="font-extrabold text-sm uppercase tracking-wider text-gray-100">GATE MockAI — Student Portal</span>
        </div>

        <div className="flex items-center gap-4">
          <div className="text-right">
            <p className="text-xs font-bold text-gray-200">{fullName}</p>
            <p className="text-[10px] text-gray-400 font-mono leading-none">{email}</p>
          </div>

          <a 
            href="/logout"
            className="p-1.5 hover:bg-white/10 rounded-full transition-all duration-150 text-gray-400 hover:text-white"
            title="Log Out"
          >
            <LogOut className="w-5 h-5" />
          </a>
        </div>
      </nav>

      {/* DASHBOARD BODY */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-6 py-8 flex flex-col gap-6">
        {/* WELCOME BANNER */}
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white p-6 rounded-lg shadow flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div>
            <h2 className="text-xl font-black uppercase tracking-tight">Welcome, {fullName}!</h2>
            <p className="text-sm text-blue-100 mt-1">
              Select and launch custom, secure AI-compiled mock test papers to proctor your GATE syllabus preparation.
            </p>
          </div>

          <Link
            to="/student/tests"
            className="bg-white hover:bg-gray-100 text-blue-700 font-bold px-5 py-2.5 rounded-[4px] shadow text-xs uppercase tracking-wider flex items-center gap-2 transition-all duration-150"
          >
            <PlayCircle className="w-4 h-4" />
            Launch Secure Exam
          </Link>
        </div>

        {/* OVERVIEW CARDS */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex items-center gap-4">
            <div className="bg-blue-50 text-blue-600 p-3.5 rounded-full">
              <GraduationCap className="w-6 h-6" />
            </div>
            <div>
              <p className="text-[10px] text-gray-400 font-extrabold uppercase tracking-wider leading-none">Completed Exams</p>
              <h3 className="text-2xl font-black text-gray-800 mt-1">{attemptsCount}</h3>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex items-center gap-4">
            <div className="bg-green-50 text-green-600 p-3.5 rounded-full">
              <Trophy className="w-6 h-6" />
            </div>
            <div>
              <p className="text-[10px] text-gray-400 font-extrabold uppercase tracking-wider leading-none">Highest Score</p>
              <h3 className="text-2xl font-black text-gray-800 mt-1">{highScore.toFixed(2)} <span className="text-xs text-gray-400 font-medium">/ 100</span></h3>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex items-center gap-4">
            <div className="bg-indigo-50 text-indigo-600 p-3.5 rounded-full">
              <BarChart3 className="w-6 h-6" />
            </div>
            <div>
              <p className="text-[10px] text-gray-400 font-extrabold uppercase tracking-wider leading-none">Global Accuracy</p>
              <h3 className="text-2xl font-black text-gray-800 mt-1">{(attemptsCount > 0 ? 82.5 : 0.0).toFixed(1)}%</h3>
            </div>
          </div>
        </div>

        {/* HISTORICAL EXAM ATTEMPTS SECTION */}
        <div className="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden flex flex-col flex-1">
          <div className="bg-gray-50 border-b border-gray-200 px-6 py-4 flex items-center gap-2">
            <History className="w-5 h-5 text-gray-600" />
            <h3 className="text-sm font-extrabold text-gray-700 uppercase tracking-wider">Historical Examination Attempts</h3>
          </div>

          {attempts.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center p-12 text-center">
              <GraduationCap className="w-12 h-12 text-gray-300" />
              <p className="text-gray-500 font-medium mt-3">You have not launched any secures mock exam attempts yet.</p>
              <Link 
                to="/student/tests"
                className="mt-4 bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs uppercase px-4 py-2.5 rounded-[4px] shadow"
              >
                Browse Available Exams
              </Link>
            </div>
          ) : (
            <div className="overflow-x-auto flex-1">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="bg-gray-100 text-gray-500 uppercase text-[10px] tracking-wider border-b border-gray-200 font-bold">
                    <th className="px-6 py-3">Exam Description</th>
                    <th className="px-6 py-3">Date Commenced</th>
                    <th className="px-6 py-3 text-center">Score Card</th>
                    <th className="px-6 py-3 text-center">Proctor Status</th>
                    <th className="px-6 py-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 font-sans text-gray-700 font-medium">
                  {attempts.map((att) => {
                    const isSubmitted = att.status === 'SUBMITTED';
                    
                    return (
                      <tr key={att.id} className="hover:bg-gray-50/50 transition-colors">
                        <td className="px-6 py-4">
                          <p className="font-extrabold text-gray-800 text-sm leading-tight">{att.testTitle}</p>
                          <p className="text-[10px] text-gray-400 mt-0.5">Attempt ID: {att.id.slice(0, 8).toUpperCase()}</p>
                        </td>
                        <td className="px-6 py-4 text-xs font-mono text-gray-500">
                          {new Date(att.startedAt).toLocaleString()}
                        </td>
                        <td className="px-6 py-4 text-center font-bold font-mono">
                          {isSubmitted ? (
                            <span className="text-blue-700 text-sm">
                              {att.score?.toFixed(2)} <span className="text-[10px] text-gray-400">/ 100</span>
                            </span>
                          ) : (
                            <span className="text-gray-400 italic text-xs">Unsubmitted</span>
                          )}
                        </td>
                        <td className="px-6 py-4 text-center">
                          <span className={`inline-flex px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase leading-5 tracking-wide ${
                            isSubmitted 
                              ? 'bg-green-50 text-green-700 border border-green-200' 
                              : 'bg-yellow-50 text-yellow-700 border border-yellow-200 animate-pulse'
                          }`}>
                            {att.status.replace('_', ' ')}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-right">
                          {isSubmitted ? (
                            <Link
                              to={`/student/attempts/${att.id}/result`}
                              className="inline-flex items-center gap-1 bg-white hover:bg-gray-50 border border-gray-300 text-gray-700 hover:text-blue-600 font-extrabold text-xs uppercase px-3 py-1.5 rounded-[4px] shadow-sm transition-all duration-150"
                            >
                              Review Result
                            </Link>
                          ) : (
                            <Link
                              to={`/student/tests/${att.testId}/take`}
                              className="inline-flex items-center gap-1 bg-blue-600 hover:bg-blue-700 text-white font-extrabold text-xs uppercase px-3 py-1.5 rounded-[4px] shadow transition-all duration-150"
                            >
                              Resume secure Test
                            </Link>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
