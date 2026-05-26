import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { 
  Users, 
  Activity, 
  Percent, 
  Award, 
  ChevronLeft, 
  Loader2, 
  TrendingUp, 
  Trophy, 
  ArrowUpDown, 
  BookOpen, 
  ChevronRight, 
  AlertCircle 
} from 'lucide-react'
import { 
  BarChart, 
  Bar, 
  Cell, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer 
} from 'recharts'

export default function AdminAnalytics() {
  const navigate = useNavigate();

  // Core Data States
  const [summary, setSummary] = useState(null);
  const [distribution, setDistribution] = useState([]);
  const [weaknesses, setWeaknesses] = useState([]);
  const [performance, setPerformance] = useState([]);
  const [leaderboard, setLeaderboard] = useState([]);
  const [selectedTestId, setSelectedTestId] = useState('');

  // Status States
  const [loading, setLoading] = useState(true);
  const [leaderboardLoading, setLeaderboardLoading] = useState(false);
  const [error, setError] = useState(null);

  // Sorting State for Section 3
  const [sortField, setSortField] = useState('attemptCount');
  const [sortDirection, setSortDirection] = useState('desc');

  // Fetch all basic admin analytics datasets on mount
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);

        const [summaryRes, distRes, weakRes, perfRes] = await Promise.all([
          fetch('/api/admin/analytics/platform-summary'),
          fetch('/api/admin/analytics/score-distribution'),
          fetch('/api/admin/analytics/subject-weakness'),
          fetch('/api/admin/analytics/test-performance')
        ]);

        if (!summaryRes.ok || !distRes.ok || !weakRes.ok || !perfRes.ok) {
          throw new Error('Failed to load platform analytics.');
        }

        const summaryData = await summaryRes.json();
        const distData = await distRes.json();
        const weakData = await weakRes.json();
        const perfData = await perfRes.json();

        setSummary(summaryData);
        setDistribution(distData);
        setWeaknesses(weakData);
        setPerformance(perfData);

        // Fetch initial global leaderboard
        const leaderRes = await fetch('/api/admin/analytics/student-leaderboard?limit=10');
        if (leaderRes.ok) {
          const leaderData = await leaderRes.json();
          setLeaderboard(leaderData);
        }

        setLoading(false);
      } catch (err) {
        logError(err);
        setError('Failed to load analytics — try refreshing.');
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // Fetch leaderboard when selected test changes
  useEffect(() => {
    const fetchFilteredLeaderboard = async () => {
      setLeaderboardLoading(true);
      try {
        let url = '/api/admin/analytics/student-leaderboard?limit=10';
        if (selectedTestId) {
          url += `&testId=${selectedTestId}`;
        }
        const res = await fetch(url);
        if (res.ok) {
          const data = await res.json();
          setLeaderboard(data);
        }
      } catch (err) {
        console.error('Failed to fetch filtered leaderboard:', err);
      } finally {
        setLeaderboardLoading(false);
      }
    };

    // Skip first run when basic loading is active
    if (!loading) {
      fetchFilteredLeaderboard();
    }
  }, [selectedTestId, loading]);

  const logError = (err) => {
    console.error('AdminAnalytics Error:', err);
  };

  // Section 3 local sort logic
  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const getSortedPerformance = () => {
    return [...performance].sort((a, b) => {
      let valA = a[sortField];
      let valB = b[sortField];

      if (typeof valA === 'string') {
        return sortDirection === 'asc' 
          ? valA.localeCompare(valB) 
          : valB.localeCompare(valA);
      }

      return sortDirection === 'asc' ? valA - valB : valB - valA;
    });
  };

  // Helper to color code score buckets in distribution chart
  const getBucketColor = (bucketName) => {
    const minVal = parseInt(bucketName.split('-')[0]);
    if (minVal < 40) return '#ef4444'; // Red for low scores
    if (minVal < 70) return '#f59e0b'; // Amber for mid scores
    return '#10b981'; // Green for high scores
  };

  // Helper to color code subject weaknesses
  const getWeaknessColor = (pct) => {
    if (pct < 40) return 'bg-red-500';
    if (pct < 65) return 'bg-amber-500';
    return 'bg-green-500';
  };

  const getWeaknessTextColor = (pct) => {
    if (pct < 40) return 'text-red-600';
    if (pct < 65) return 'text-amber-600';
    return 'text-green-600';
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col font-sans select-none shrink-0">
        {/* Nav Skeleton */}
        <div className="bg-slate-900 h-[64px] w-full animate-pulse" />
        <main className="max-w-6xl w-full mx-auto px-6 py-8 flex flex-col gap-6 flex-1">
          {/* Section 1 KPI Skeleton */}
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-5">
            {[1, 2, 3, 4].map(i => (
              <div key={i} className="bg-white border border-gray-200 rounded-lg p-5 h-24 animate-pulse flex items-center gap-4">
                <div className="bg-gray-200 w-12 h-12 rounded-full" />
                <div className="flex-1 space-y-2">
                  <div className="bg-gray-200 h-3 w-1/2 rounded" />
                  <div className="bg-gray-200 h-6 w-3/4 rounded" />
                </div>
              </div>
            ))}
          </div>
          {/* Section 2 Skeleton */}
          <div className="grid grid-cols-1 md:grid-cols-5 gap-6">
            <div className="md:col-span-3 bg-white border border-gray-200 rounded-lg p-6 h-[320px] animate-pulse" />
            <div className="md:col-span-2 bg-white border border-gray-200 rounded-lg p-6 h-[320px] animate-pulse" />
          </div>
          {/* Section 3 Skeleton */}
          <div className="bg-white border border-gray-200 rounded-lg p-6 h-[240px] animate-pulse" />
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4 font-sans">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md text-center">
          <AlertCircle className="w-10 h-10 text-red-600 mx-auto" />
          <h3 className="text-red-700 font-extrabold text-lg uppercase mt-2">Error Incurred</h3>
          <p className="text-sm text-red-600 mt-1 font-medium">{error}</p>
          <button 
            type="button"
            onClick={() => window.location.reload()}
            className="mt-4 bg-red-600 hover:bg-red-700 text-white font-bold text-xs uppercase px-4 py-2 rounded-[4px]"
          >
            Retry Loading
          </button>
        </div>
      </div>
    );
  }

  const sortedPerformance = getSortedPerformance();

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans select-none shrink-0">
      {/* NAVBAR */}
      <nav className="bg-slate-900 text-white h-[64px] px-8 flex justify-between items-center shadow-md select-none shrink-0">
        <div className="flex items-center gap-2.5">
          <Link 
            to="/admin/dashboard"
            className="hover:bg-white/10 p-1.5 rounded-full text-gray-400 hover:text-white transition-colors mr-1"
          >
            <ChevronLeft className="w-5 h-5" />
          </Link>
          <span className="font-extrabold text-sm uppercase tracking-wider text-gray-100">
            GATE MockAI — Platform Analytics
          </span>
        </div>

        <a 
          href="/logout"
          className="text-xs uppercase font-extrabold text-gray-400 hover:text-white transition-colors"
        >
          Sign Out
        </a>
      </nav>

      {/* DASHBOARD BODY */}
      <main className="max-w-6xl w-full mx-auto px-6 py-8 flex flex-col gap-6 flex-1">
        
        {/* SECTION 1 — PLATFORM KPI BAR */}
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-5">
          {/* Card 1: Total Students */}
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex items-center gap-4">
            <div className="bg-blue-50 text-blue-600 p-3.5 rounded-full">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[9px] text-gray-400 font-bold uppercase tracking-wider block leading-none">Total Students</span>
              <h3 className="text-xl font-black text-gray-800 mt-1">{summary?.totalStudents}</h3>
            </div>
          </div>

          {/* Card 2: Total Attempts */}
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex items-center gap-4">
            <div className="bg-indigo-50 text-indigo-600 p-3.5 rounded-full">
              <Activity className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[9px] text-gray-400 font-bold uppercase tracking-wider block leading-none">Total Attempts</span>
              <h3 className="text-xl font-black text-gray-800 mt-1">{summary?.totalAttempts}</h3>
            </div>
          </div>

          {/* Card 3: Average Score */}
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex items-center gap-4">
            <div className="bg-green-50 text-green-600 p-3.5 rounded-full">
              <Percent className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[9px] text-gray-400 font-bold uppercase tracking-wider block leading-none">Average Score</span>
              <h3 className="text-xl font-black text-gray-800 mt-1">{summary?.avgScore}%</h3>
            </div>
          </div>

          {/* Card 4: Pass Rate */}
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex items-center gap-4">
            <div className="bg-amber-50 text-amber-600 p-3.5 rounded-full">
              <Award className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[9px] text-gray-400 font-bold uppercase tracking-wider block leading-none">Platform Pass Rate</span>
              <h3 className="text-xl font-black text-gray-800 mt-1">{summary?.passRate}%</h3>
            </div>
          </div>
        </div>

        {/* SECTION 2 — SCORE DISTRIBUTION + SUBJECT WEAKNESS */}
        <div className="grid grid-cols-1 md:grid-cols-5 gap-6">
          
          {/* Left 60%: Score Distribution Bar Chart */}
          <div className="md:col-span-3 bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col gap-4">
            <div>
              <h3 className="text-xs font-black uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-indigo-600" />
                Score Distribution
              </h3>
              <p className="text-[10px] text-gray-400 mt-0.5">Aggregated attempt scoring percentages segmented into 10% buckets.</p>
            </div>

            <div className="h-[230px] w-full font-sans text-xs">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={distribution} margin={{ top: 5, right: 10, left: -25, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="bucket" />
                  <YAxis allowDecimals={false} />
                  <Tooltip 
                    formatter={(value) => [`${value} attempts`, 'Volume']} 
                    labelFormatter={(label) => `Score range: ${label}%`} 
                  />
                  <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                    {distribution.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={getBucketColor(entry.bucket)} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Right 40%: Subject Weakness list */}
          <div className="md:col-span-2 bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col gap-4 overflow-y-auto max-h-[350px]">
            <div>
              <h3 className="text-xs font-black uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <BookOpen className="w-4 h-4 text-indigo-600" />
                Syllabus Subject weaknesses
              </h3>
              <p className="text-[10px] text-gray-400 mt-0.5">Average accuracy rates of all students grouped by syllabus subject (weakest first).</p>
            </div>

            <div className="flex flex-col gap-3.5">
              {weaknesses.length === 0 ? (
                <p className="text-xs text-gray-400 italic text-center py-8">No answered questions recorded.</p>
              ) : (
                weaknesses.map((weak, idx) => (
                  <div key={idx} className="flex flex-col gap-1">
                    <div className="flex justify-between items-center text-xs font-bold">
                      <span className="text-slate-700 truncate max-w-[70%]">{weak.subject}</span>
                      <span className={getWeaknessTextColor(weak.accuracyPct)}>{weak.accuracyPct}% accuracy</span>
                    </div>
                    <div className="w-full bg-gray-100 h-2.5 rounded-full overflow-hidden">
                      <div 
                        className={`h-full rounded-full transition-all duration-300 ${getWeaknessColor(weak.accuracyPct)}`}
                        style={{ width: `${weak.accuracyPct}%` }}
                      />
                    </div>
                    <span className="text-[9px] text-gray-400 font-mono">
                      Solved: {weak.totalCorrect} / {weak.totalAnswered} questions
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* SECTION 3 — TEST PERFORMANCE TABLE */}
        <div className="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden flex flex-col">
          <div className="bg-gray-50 border-b border-gray-200 px-6 py-4 flex justify-between items-center">
            <div className="flex items-center gap-2">
              <Activity className="w-5 h-5 text-gray-600" />
              <h3 className="text-xs font-black text-gray-700 uppercase tracking-wider">Exam Papers Performance Overview</h3>
            </div>
            <span className="text-[9px] text-gray-400 font-bold uppercase tracking-wider">Sortable Table</span>
          </div>

          <div className="overflow-x-auto">
            {sortedPerformance.length === 0 ? (
              <div className="text-center py-12 text-gray-400 italic text-xs">
                No mock test data has been submitted on this platform yet.
              </div>
            ) : (
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="bg-gray-100 text-gray-500 uppercase text-[9px] tracking-wider border-b border-gray-200 font-bold select-none">
                    <th className="px-6 py-3.5 cursor-pointer hover:bg-gray-200" onClick={() => handleSort('title')}>
                      <div className="flex items-center gap-1.5">
                        Test Title
                        <ArrowUpDown className="w-3.5 h-3.5" />
                      </div>
                    </th>
                    <th className="px-6 py-3.5 text-center cursor-pointer hover:bg-gray-200" onClick={() => handleSort('attemptCount')}>
                      <div className="flex items-center justify-center gap-1.5">
                        Attempts
                        <ArrowUpDown className="w-3.5 h-3.5" />
                      </div>
                    </th>
                    <th className="px-6 py-3.5 text-center cursor-pointer hover:bg-gray-200" onClick={() => handleSort('avgScore')}>
                      <div className="flex items-center justify-center gap-1.5">
                        Avg Score %
                        <ArrowUpDown className="w-3.5 h-3.5" />
                      </div>
                    </th>
                    <th className="px-6 py-3.5 text-center cursor-pointer hover:bg-gray-200" onClick={() => handleSort('avgTimeMins')}>
                      <div className="flex items-center justify-center gap-1.5">
                        Avg Time (min)
                        <ArrowUpDown className="w-3.5 h-3.5" />
                      </div>
                    </th>
                    <th className="px-6 py-3.5 text-center cursor-pointer hover:bg-gray-200" onClick={() => handleSort('passRate')}>
                      <div className="flex items-center justify-center gap-1.5">
                        Pass Rate %
                        <ArrowUpDown className="w-3.5 h-3.5" />
                      </div>
                    </th>
                    <th className="px-6 py-3.5 text-center cursor-pointer hover:bg-gray-200" onClick={() => handleSort('highestScore')}>
                      <div className="flex items-center justify-center gap-1.5">
                        High
                        <ArrowUpDown className="w-3.5 h-3.5" />
                      </div>
                    </th>
                    <th className="px-6 py-3.5 text-center cursor-pointer hover:bg-gray-200" onClick={() => handleSort('lowestScore')}>
                      <div className="flex items-center justify-center gap-1.5">
                        Low
                        <ArrowUpDown className="w-3.5 h-3.5" />
                      </div>
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 font-sans text-gray-700 font-medium">
                  {sortedPerformance.map((test) => (
                    <tr 
                      key={test.testId} 
                      onClick={() => navigate(`/admin/analytics/test/${test.testId}`)}
                      className="hover:bg-gray-50/70 transition-colors cursor-pointer"
                    >
                      <td className="px-6 py-4">
                        <span className="font-extrabold text-gray-800 text-sm leading-tight hover:text-indigo-600">
                          {test.title}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-center font-bold text-gray-800 text-xs">
                        {test.attemptCount}
                      </td>
                      <td className="px-6 py-4 text-center text-xs font-mono text-indigo-600 font-bold">
                        {test.avgScore}%
                      </td>
                      <td className="px-6 py-4 text-center text-xs font-mono text-gray-500">
                        {test.avgTimeMins} m
                      </td>
                      <td className="px-6 py-4 text-center text-xs">
                        <span className={`inline-flex px-2 py-0.5 rounded-full text-[9px] font-extrabold uppercase leading-5 tracking-wide ${
                          test.passRate >= 70 ? 'bg-green-50 text-green-700 border border-green-200' :
                          test.passRate >= 45 ? 'bg-amber-50 text-amber-700 border border-amber-200' :
                          'bg-red-50 text-red-700 border border-red-200'
                        }`}>
                          {test.passRate}%
                        </span>
                      </td>
                      <td className="px-6 py-4 text-center text-xs font-mono text-green-600 font-bold">
                        {test.highestScore}
                      </td>
                      <td className="px-6 py-4 text-center text-xs font-mono text-red-500 font-bold">
                        {test.lowestScore}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* SECTION 4 — STUDENT LEADERBOARD */}
        <div className="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden flex flex-col gap-4 p-6">
          <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
            <div>
              <h3 className="text-xs font-black uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <Trophy className="w-4 h-4 text-indigo-600 animate-bounce" />
                Candidate rankings leaderboard
              </h3>
              <p className="text-[10px] text-gray-400 mt-0.5">Top-performing student candidates sorted by average exam score.</p>
            </div>

            {/* Test Selection Dropdown */}
            <div className="flex items-center gap-2">
              <span className="text-[10px] text-gray-400 font-bold uppercase shrink-0">Filter By Test:</span>
              <select
                value={selectedTestId}
                onChange={(e) => setSelectedTestId(e.target.value)}
                className="bg-white border border-gray-300 rounded-[4px] px-2 py-1 text-xs text-gray-700 font-semibold focus:outline-none focus:border-indigo-500"
              >
                <option value="">Global Leaderboard (All Tests)</option>
                {performance.map(test => (
                  <option key={test.testId} value={test.testId}>
                    {test.title}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="overflow-x-auto min-h-[160px] relative">
            {leaderboardLoading ? (
              <div className="absolute inset-0 bg-white/70 backdrop-blur-sm flex items-center justify-center z-10">
                <Loader2 className="w-8 h-8 text-indigo-600 animate-spin" />
              </div>
            ) : null}

            {leaderboard.length === 0 ? (
              <div className="text-center py-12 text-gray-400 italic text-xs">
                No student has completed attempt logs for the selected test.
              </div>
            ) : (
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="bg-gray-100 text-gray-500 uppercase text-[9px] tracking-wider border-b border-gray-200 font-bold select-none">
                    <th className="px-6 py-3 text-center w-16">Rank</th>
                    <th className="px-6 py-3">Student Name</th>
                    <th className="px-6 py-3 text-center">Attempts</th>
                    <th className="px-6 py-3 text-center">Best Score</th>
                    <th className="px-6 py-3 text-center">Average Score</th>
                    <th className="px-6 py-3 text-right">Last Attempt Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 font-sans text-gray-700 font-medium">
                  {leaderboard.map((student, idx) => {
                    const rankNum = idx + 1;
                    return (
                      <tr 
                        key={student.userId} 
                        className={`hover:bg-gray-50/50 transition-colors ${
                          rankNum === 1 ? 'bg-yellow-50/30' : 
                          rankNum === 2 ? 'bg-slate-50/30' : 
                          rankNum === 3 ? 'bg-amber-50/20' : ''
                        }`}
                      >
                        <td className="px-6 py-4 text-center">
                          <span className={`inline-flex items-center justify-center w-7 h-7 rounded-full text-xs font-black leading-none ${
                            rankNum === 1 ? 'bg-yellow-100 text-yellow-800 ring-2 ring-yellow-400' :
                            rankNum === 2 ? 'bg-slate-200 text-slate-800 ring-2 ring-slate-400' :
                            rankNum === 3 ? 'bg-amber-100 text-amber-800 ring-2 ring-amber-400' :
                            'bg-gray-100 text-gray-500'
                          }`}>
                            {rankNum}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <p className="font-extrabold text-gray-800 text-sm leading-tight">{student.fullName}</p>
                          <p className="text-[10px] text-gray-400 mt-0.5 truncate font-mono">{student.userId}</p>
                        </td>
                        <td className="px-6 py-4 text-center text-xs font-mono text-gray-500">
                          {student.attemptCount}
                        </td>
                        <td className="px-6 py-4 text-center text-xs font-mono text-indigo-600 font-bold">
                          {student.bestScore}
                        </td>
                        <td className="px-6 py-4 text-center text-xs font-mono text-gray-700">
                          {student.avgScore}
                        </td>
                        <td className="px-6 py-4 text-right text-xs font-mono text-gray-400">
                          {student.lastAttemptDate ? new Date(student.lastAttemptDate).toLocaleString() : 'N/A'}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>

      </main>
    </div>
  )
}
