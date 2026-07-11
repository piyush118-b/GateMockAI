import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { 
  GraduationCap, 
  History, 
  PlayCircle, 
  Trophy, 
  BarChart3, 
  LogOut, 
  Loader2, 
  Clock, 
  Percent, 
  TrendingUp, 
  Award, 
  BookOpen, 
  CheckCircle2, 
  AlertCircle, 
  HelpCircle,
  ChevronDown,
  ChevronUp,
  ArrowUpDown,
  BookMarked
} from 'lucide-react'
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  Radar,
  Legend
} from 'recharts'

export default function StudentDashboard() {
  const navigate = useNavigate();

  // Core Summary Data State (Section 1, 3, 4, 5)
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Section 2 Independent Datasets
  const [scoreTimeline, setScoreTimeline] = useState([]);
  const [timelineLoading, setTimelineLoading] = useState(true);
  
  const [subjectRadar, setSubjectRadar] = useState([]);
  const [radarLoading, setRadarLoading] = useState(true);

  // Section 3: Attempt History Sorting
  const [historySortDirection, setHistorySortDirection] = useState('desc'); // 'asc' or 'desc'

  // Section 4: Weak Questions List & localStorage revised tracking
  const [localQuestionsList, setLocalQuestionsList] = useState([]);
  const [expandedTextIds, setExpandedTextIds] = useState({});
  const [expandedExplanationIds, setExpandedExplanationIds] = useState({});

  // Section 5: Rank & Percentile
  const [attemptedTests, setAttemptedTests] = useState([]);
  const [selectedTestId, setSelectedTestId] = useState('');
  const [rankData, setRankData] = useState(null);
  const [rankLoading, setRankLoading] = useState(false);

  // Spaced Repetition States
  const [dueCount, setDueCount] = useState(0);
  const [generatingRevision, setGeneratingRevision] = useState(false);

  // Helper: Convert total time spent minutes to 'Xh Ym' format
  const formatTimeSpent = (totalMins) => {
    if (!totalMins || totalMins <= 0) return '0h 0m';
    const hours = Math.floor(totalMins / 60);
    const mins = Math.round(totalMins % 60);
    return `${hours}h ${mins}m`;
  };

  // Helper: Format Dates to 'DD MMM YYYY HH:mm'
  const formatDateString = (dateStr) => {
    if (!dateStr) return 'N/A';
    const d = new Date(dateStr);
    const day = String(d.getDate()).padStart(2, '0');
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    const month = months[d.getMonth()];
    const year = d.getFullYear();
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    return `${day} ${month} ${year} ${hours}:${minutes}`;
  };

  // Fetch summary and attempts on mount
  useEffect(() => {
    fetch('/api/student/analytics/my-summary')
      .then(res => {
        if (res.status === 401 || res.status === 403) {
          navigate('/login');
          return null;
        }
        if (!res.ok) throw new Error('Failed to load student progress dashboard.');
        return res.json();
      })
      .then(json => {
        if (json) {
          setSummary(json);

          // Process Weak Questions (Section 4) with localStorage revised persistence
          if (json.weakQuestions) {
            const list = json.weakQuestions.map(q => ({
              ...q,
              revised: localStorage.getItem(`gate_revised_${q.questionId}`) === 'true'
            }));

            // Push revised ones to the bottom of the list
            list.sort((a, b) => (a.revised === b.revised ? 0 : a.revised ? 1 : -1));
            setLocalQuestionsList(list);
          }

          setLoading(false);
        }
      })
      .catch(err => {
        console.error('Progress summary load error:', err);
        setError('Failed to load summary — try refreshing.');
        setLoading(false);
      });
  }, [navigate]);

  // Fetch due count on mount
  useEffect(() => {
    fetch('/api/exam/revision/due-count')
      .then(res => res.json())
      .then(json => setDueCount(json.dueCount || 0))
      .catch(err => console.error('Failed to load due count:', err));
  }, []);

  const handleStartRevision = async () => {
    if (generatingRevision) return;
    setGeneratingRevision(true);
    try {
      const res = await fetch('/api/exam/revision/generate', {
        method: 'POST'
      });
      if (!res.ok) throw new Error('Failed to generate revision test');
      const data = await res.json();
      if (data.mockTestId) {
        navigate(`/student/tests/${data.mockTestId}/take`);
      } else {
        alert(data.message || 'No questions due today!');
      }
    } catch (err) {
      console.error(err);
      alert('Error generating revision test. Please try again.');
    } finally {
      setGeneratingRevision(false);
    }
  };

  // Fetch Score Timeline (Section 2 - Left Panel)
  useEffect(() => {
    if (loading) return;
    fetch('/api/student/analytics/my-score-timeline')
      .then(res => {
        if (!res.ok) throw new Error('Failed to load score timeline.');
        return res.json();
      })
      .then(json => {
        setScoreTimeline(json);
        setTimelineLoading(false);
      })
      .catch(err => {
        console.error('Score timeline error:', err);
        setTimelineLoading(false);
      });
  }, [loading]);

  // Fetch Subject Radar (Section 2 - Right Panel)
  useEffect(() => {
    if (loading) return;
    fetch('/api/student/analytics/my-subject-radar')
      .then(res => {
        if (!res.ok) throw new Error('Failed to load subject radar.');
        return res.json();
      })
      .then(json => {
        setSubjectRadar(json);
        setRadarLoading(false);
      })
      .catch(err => {
        console.error('Subject radar error:', err);
        setRadarLoading(false);
      });
  }, [loading]);

  // Fetch list of tests to populate the rank selector
  useEffect(() => {
    if (loading || !summary) return;
    fetch('/api/student/tests')
      .then(res => res.json())
      .then(json => {
        if (json) {
          // Filter to only tests that have been attempted by this student
          const attemptedTitles = new Set(summary.attemptHistory.map(h => h.testTitle));
          const filtered = json.filter(t => attemptedTitles.has(t.title));
          setAttemptedTests(filtered);
          if (filtered.length > 0) {
            setSelectedTestId(filtered[0].id);
          }
        }
      })
      .catch(err => console.error('Failed to load attempted tests list:', err));
  }, [loading, summary]);

  // Fetch Rank details (Section 5) when selected test changes
  useEffect(() => {
    if (!selectedTestId) return;
    setRankLoading(true);
    fetch(`/api/student/analytics/my-rank?testId=${selectedTestId}`)
      .then(res => {
        if (!res.ok) throw new Error('Rank details not found.');
        return res.json();
      })
      .then(json => {
        setRankData(json);
        setRankLoading(false);
      })
      .catch(err => {
        console.error('Rank fetch error:', err);
        setRankData(null);
        setRankLoading(false);
      });
  }, [selectedTestId]);

  // Section 3 Date toggle sorting
  const handleToggleSortHistory = () => {
    setHistorySortDirection(prev => (prev === 'desc' ? 'asc' : 'desc'));
  };

  const getSortedHistory = () => {
    if (!summary || !summary.attemptHistory) return [];
    return [...summary.attemptHistory].sort((a, b) => {
      const dateA = a.submittedAt ? new Date(a.submittedAt).getTime() : 0;
      const dateB = b.submittedAt ? new Date(b.submittedAt).getTime() : 0;
      return historySortDirection === 'asc' ? dateA - dateB : dateB - dateA;
    });
  };

  // Section 4 Revised toggle handler
  const handleToggleRevised = (questionId) => {
    const isCurrentlyRevised = localStorage.getItem(`gate_revised_${questionId}`) === 'true';
    const newRevisedState = !isCurrentlyRevised;
    
    // Save to local storage
    if (newRevisedState) {
      localStorage.setItem(`gate_revised_${questionId}`, 'true');
    } else {
      localStorage.removeItem(`gate_revised_${questionId}`);
    }

    // splice + append in local state to move the revised question to the bottom
    const newList = [...localQuestionsList];
    const index = newList.findIndex(q => q.questionId === questionId);
    if (index > -1) {
      const [item] = newList.splice(index, 1);
      const updatedItem = { ...item, revised: newRevisedState };
      if (newRevisedState) {
        newList.push(updatedItem); // Append to the bottom
      } else {
        newList.unshift(updatedItem); // Prepend to the top of unrevised
      }
      setLocalQuestionsList(newList);
    }
  };

  // Expand / collapse question text
  const handleToggleTextExpand = (qId) => {
    setExpandedTextIds(prev => ({ ...prev, [qId]: !prev[qId] }));
  };

  // Expand / collapse question explanation
  const handleToggleExplanationExpand = (qId) => {
    setExpandedExplanationIds(prev => ({ ...prev, [qId]: !prev[qId] }));
  };

  // UI helpers
  const getScoreCellClass = (score) => {
    if (score < 50) return 'text-red-600 bg-red-50 font-bold px-2.5 py-1.5 rounded-md';
    if (score <= 70) return 'text-amber-600 bg-amber-50 font-bold px-2.5 py-1.5 rounded-md';
    return 'text-green-600 bg-green-50 font-bold px-2.5 py-1.5 rounded-md';
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col font-sans shrink-0">
        <div className="bg-slate-900 h-[64px] w-full animate-pulse" />
        <main className="max-w-6xl w-full mx-auto px-6 py-8 flex flex-col gap-6 flex-1">
          {/* Section 1 Loading Skeletons */}
          <div className="grid grid-cols-1 sm:grid-cols-5 gap-5">
            {[1, 2, 3, 4, 5].map(i => (
              <div key={i} className="bg-white border border-gray-200 rounded-lg p-5 h-24 animate-pulse" />
            ))}
          </div>
          {/* Section 2 Loading Skeletons */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-white border border-gray-200 rounded-lg p-6 h-[320px] animate-pulse" />
            <div className="bg-white border border-gray-200 rounded-lg p-6 h-[320px] animate-pulse" />
          </div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4 font-sans">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md text-center">
          <AlertCircle className="w-10 h-10 text-red-600 mx-auto" />
          <h3 className="text-red-700 font-extrabold text-lg uppercase mt-2">Dashboard Error</h3>
          <p className="text-sm text-red-600 mt-1 font-medium">{error}</p>
          <button 
            type="button"
            onClick={() => window.location.reload()}
            className="mt-4 bg-red-600 hover:bg-red-700 text-white font-bold text-xs uppercase px-4 py-2 rounded-[4px]"
          >
            Refresh Dashboard
          </button>
        </div>
      </div>
    );
  }

  const { totalAttempts, avgScore, bestScore, totalTimeSpentMins, attemptHistory, scoreTrend, globalRank } = summary;
  const sortedHistory = getSortedHistory();

  // Check if subject radar accuracy are all zero
  const isAllRadarZero = subjectRadar.length > 0 && subjectRadar.every(s => s.accuracyPct === 0);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans select-none shrink-0">
      
      {/* NAVBAR */}
      <nav className="bg-slate-900 text-white h-[64px] px-8 flex justify-between items-center shadow-md">
        <div className="flex items-center gap-2.5">
          <div className="bg-blue-600 w-9 h-9 rounded-md flex items-center justify-center font-black text-white text-lg">
            M
          </div>
          <span className="font-extrabold text-sm uppercase tracking-wider text-gray-100">
            GATE MockAI — Student Portal
          </span>
        </div>

        <div className="flex items-center gap-6">
          <Link to="/student/dashboard" className="text-xs uppercase font-extrabold text-blue-500 border-b-2 border-blue-500 pb-1">
            My Progress
          </Link>
          <Link to="/student/tests" className="text-xs uppercase font-extrabold text-gray-400 hover:text-white transition-colors pb-1">
            Browse Exams
          </Link>
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
      <main className="max-w-6xl w-full mx-auto px-6 py-8 flex flex-col gap-6 flex-1">
        
        {/* REVISION SESSION ALERT CARD */}
        {dueCount > 0 && (
          <div className="bg-gradient-to-r from-amber-500 to-orange-600 text-white rounded-lg p-5 shadow-md flex flex-col sm:flex-row justify-between items-center gap-4 animate-fadeIn border border-orange-500">
            <div className="flex items-center gap-3">
              <div className="bg-white/20 p-2.5 rounded-full shrink-0">
                <BookOpen className="w-6 h-6 text-white animate-pulse" />
              </div>
              <div>
                <h4 className="text-sm font-black uppercase tracking-wider">Revision Session Due</h4>
                <p className="text-xs text-white/95 mt-1 font-medium">
                  You have <span className="font-extrabold text-white bg-white/20 px-2 py-0.5 rounded">{dueCount} questions</span> scheduled for spaced repetition review today.
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={handleStartRevision}
              disabled={generatingRevision}
              className="bg-white hover:bg-orange-50 text-orange-700 font-extrabold text-xs uppercase tracking-wider px-5 py-3 rounded shadow hover:shadow-md transition-all shrink-0 flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
            >
              {generatingRevision ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Generating...
                </>
              ) : (
                <>
                  <PlayCircle className="w-4 h-4" />
                  Start Revision Session
                </>
              )}
            </button>
          </div>
        )}

        {/* SECTION 1 — MY KPI BAR */}
        <div className="grid grid-cols-1 sm:grid-cols-5 gap-5">
          {/* Card 1: Tests Taken */}
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex flex-col justify-between">
            <span className="text-[10px] text-gray-400 font-extrabold uppercase tracking-wider block">Tests Taken</span>
            <div className="flex items-baseline gap-2 mt-1">
              <h3 className="text-2xl font-black text-gray-800">{totalAttempts}</h3>
              <span className="text-xs text-gray-400 font-medium">attempts</span>
            </div>
          </div>

          {/* Card 2: Average Score % */}
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex flex-col justify-between">
            <span className="text-[10px] text-gray-400 font-extrabold uppercase tracking-wider block">Avg Score %</span>
            <div className="mt-1">
              <h3 className="text-2xl font-black text-gray-800">{avgScore.toFixed(1)}%</h3>
              <span className={`text-[10px] font-bold block mt-0.5 ${
                avgScore < 50 ? 'text-red-500' :
                avgScore <= 70 ? 'text-amber-500' :
                'text-green-600'
              }`}>
                {avgScore < 50 ? "Keep practising!" :
                 avgScore <= 70 ? "Good progress!" :
                 "Excellent work! 🎯"}
              </span>
            </div>
          </div>

          {/* Card 3: Best Score % */}
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex flex-col justify-between">
            <span className="text-[10px] text-gray-400 font-extrabold uppercase tracking-wider block">Best Score %</span>
            <div className="flex items-baseline gap-1 mt-1">
              <h3 className="text-2xl font-black text-gray-800">{bestScore.toFixed(1)}%</h3>
            </div>
          </div>

          {/* Card 4: Total Time */}
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex flex-col justify-between">
            <span className="text-[10px] text-gray-400 font-extrabold uppercase tracking-wider block">Total Time</span>
            <div className="flex items-baseline gap-1 mt-1">
              <h3 className="text-2xl font-black text-gray-800">{formatTimeSpent(totalTimeSpentMins)}</h3>
            </div>
          </div>

          {/* Card 5: Score Trend */}
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex flex-col justify-between">
            <span className="text-[10px] text-gray-400 font-extrabold uppercase tracking-wider block">Score Trend</span>
            {totalAttempts === 0 ? (
              <div className="mt-1 flex flex-col gap-0.5">
                <h3 className="text-2xl font-black text-gray-400">—</h3>
                <p className="text-[10px] text-gray-400 font-medium">Take your first test to start tracking!</p>
              </div>
            ) : totalAttempts === 1 ? (
              <div className="mt-1 flex flex-col gap-0.5">
                <h3 className="text-2xl font-black text-gray-400">—</h3>
                <p className="text-[10px] text-gray-400 font-medium">Take another test to see progress!</p>
              </div>
            ) : (
              <div className="flex items-center gap-1.5 mt-2">
                {scoreTrend > 0 ? (
                  <>
                    <span className="text-green-600 text-lg font-black leading-none">↑</span>
                    <span className="text-lg font-black text-green-600">+{scoreTrend.toFixed(1)}%</span>
                  </>
                ) : scoreTrend < 0 ? (
                  <>
                    <span className="text-red-500 text-lg font-black leading-none">↓</span>
                    <span className="text-lg font-black text-red-500">{scoreTrend.toFixed(1)}%</span>
                  </>
                ) : (
                  <>
                    <span className="text-gray-400 text-lg font-black leading-none">→</span>
                    <span className="text-sm font-extrabold text-gray-400">No change</span>
                  </>
                )}
              </div>
            )}
          </div>
        </div>

        {/* SECTION 2 — SCORE TIMELINE + SUBJECT RADAR */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
          
          {/* LEFT PANEL (55% width) - Score Timeline */}
          <div className="md:col-span-7 bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col justify-between gap-4">
            <div>
              <h3 className="text-xs font-black uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-indigo-600" />
                Score Timeline
              </h3>
              <p className="text-[10px] text-gray-400 mt-0.5 font-semibold">Chronological display of test scores.</p>
            </div>

            <div className="h-[260px] w-full font-sans text-xs flex flex-col justify-between">
              {timelineLoading ? (
                <div className="w-full h-full flex items-center justify-center">
                  <Loader2 className="w-6 h-6 text-indigo-600 animate-spin" />
                </div>
              ) : scoreTimeline.length === 0 ? (
                <div className="flex-1 flex flex-col items-center justify-center text-center p-6 border-2 border-dashed border-gray-200 rounded-md">
                  <HelpCircle className="w-8 h-8 text-gray-300" />
                  <p className="text-xs text-gray-400 font-bold uppercase mt-2">No attempts yet. Take your first mock test!</p>
                </div>
              ) : (
                <ResponsiveContainer width="100%" height={260}>
                  <LineChart data={scoreTimeline} margin={{ top: 15, right: 15, left: -10, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis 
                      dataKey="submittedAt" 
                      tickFormatter={(tick) => {
                        const date = new Date(tick);
                        const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                        return `${months[date.getMonth()]} ${date.getDate()}`;
                      }}
                      tickLine={false}
                      stroke="#94a3b8"
                      dy={10}
                    />
                    <YAxis 
                      domain={[0, 100]} 
                      tickLine={false}
                      stroke="#94a3b8"
                      label={{ value: 'Score %', angle: -90, position: 'insideLeft', offset: -5, style: { fill: '#94a3b8', fontWeight: 600 } }}
                    />
                    <Tooltip 
                      content={({ active, payload }) => {
                        if (active && payload && payload.length) {
                          const data = payload[0].payload;
                          return (
                            <div className="bg-slate-900 text-white p-3 rounded-lg shadow-lg border border-slate-800 text-xs font-sans">
                              <p className="font-extrabold mb-1">{data.testTitle}</p>
                              <p className="text-indigo-400 font-semibold">Score: <span className="font-black text-white">{data.scorePct}%</span></p>
                            </div>
                          );
                        }
                        return null;
                      }}
                    />
                    <Line 
                      type="monotone" 
                      dataKey="scorePct" 
                      stroke="#6366f1" 
                      strokeWidth={2} 
                      dot={{ r: 4, stroke: '#6366f1', strokeWidth: 2, fill: '#fff' }} 
                      activeDot={{ r: 6 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </div>

            {scoreTimeline.length === 1 && (
              <div className="bg-blue-50 border border-blue-100 rounded p-3 text-[10px] text-blue-700 font-semibold leading-relaxed flex items-center gap-1.5 mt-2 animate-pulse">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>Take more tests to see your score trend over time.</span>
              </div>
            )}
          </div>

          {/* RIGHT PANEL (45% width) - Subject Radar */}
          <div className="md:col-span-5 bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col justify-between gap-4 relative overflow-hidden">
            <div>
              <h3 className="text-xs font-black uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <BarChart3 className="w-4 h-4 text-indigo-600" />
                Subject Analysis Radar
              </h3>
              <p className="text-[10px] text-gray-400 mt-0.5 font-semibold">Your syllabus accuracy distribution.</p>
            </div>

            <div className="h-[260px] w-full font-sans text-[10px] flex items-center justify-center relative">
              {radarLoading ? (
                <Loader2 className="w-6 h-6 text-indigo-600 animate-spin" />
              ) : subjectRadar.length === 0 ? (
                <p className="text-xs text-gray-400 italic">No subject metrics solved yet.</p>
              ) : (
                <>
                  {isAllRadarZero && (
                    <div className="absolute inset-0 bg-white/80 backdrop-blur-sm flex items-center justify-center z-10 p-6 text-center">
                      <p className="text-xs font-extrabold text-indigo-600 uppercase">
                        Complete attempts to see your subject breakdown.
                      </p>
                    </div>
                  )}
                  <ResponsiveContainer width="100%" height={260}>
                    <RadarChart cx="50%" cy="50%" outerRadius="70%" data={subjectRadar}>
                      <PolarGrid stroke="#e2e8f0" />
                      <PolarAngleAxis dataKey="subject" tick={{ fill: '#64748b', fontSize: 10, fontWeight: 500 }} />
                      <Radar 
                        name="Accuracy %" 
                        dataKey="accuracyPct" 
                        stroke="#6366f1" 
                        fill="#6366f1" 
                        fillOpacity={0.25} 
                      />
                      <Legend verticalAlign="bottom" height={36} />
                    </RadarChart>
                  </ResponsiveContainer>
                </>
              )}
            </div>
          </div>
        </div>

        {/* SECTION 3 — ATTEMPT HISTORY TABLE */}
        <div className="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden flex flex-col">
          <div className="bg-gray-50 border-b border-gray-200 px-6 py-4 flex justify-between items-center select-none">
            <div className="flex items-center gap-2">
              <History className="w-5 h-5 text-gray-600" />
              <h3 className="text-xs font-black text-gray-700 uppercase tracking-wider">Historical Examination Attempts</h3>
            </div>
            <span className="text-[9px] text-gray-400 font-extrabold uppercase">Sorting Available</span>
          </div>

          <div className="overflow-x-auto">
            {sortedHistory.length === 0 ? (
              <div className="text-center py-16 px-6">
                <p className="text-sm text-gray-400 font-bold uppercase mb-4">You haven't attempted any tests yet. Go to Tests to get started.</p>
                <Link 
                  to="/student/tests"
                  className="bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs uppercase px-5 py-2.5 rounded shadow inline-flex items-center gap-1.5"
                >
                  <PlayCircle className="w-4 h-4" />
                  Browse Exams
                </Link>
              </div>
            ) : (
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="bg-gray-100 text-gray-500 uppercase text-[9px] tracking-wider border-b border-gray-200 font-bold select-none">
                    <th className="px-6 py-3.5">Test Name</th>
                    <th className="px-6 py-3.5 cursor-pointer hover:bg-gray-200" onClick={handleToggleSortHistory}>
                      <div className="flex items-center gap-1.5">
                        Date Submitted
                        <ArrowUpDown className="w-3.5 h-3.5 text-gray-400" />
                      </div>
                    </th>
                    <th className="px-6 py-3.5 text-center">Score %</th>
                    <th className="px-6 py-3.5 text-center">Time Taken</th>
                    <th className="px-6 py-3.5 text-center">Status</th>
                    <th className="px-6 py-3.5 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 font-sans text-gray-700 font-medium">
                  {sortedHistory.map((att) => {
                    const isSubmitted = att.status === 'SUBMITTED';
                    const scorePercentage = att.totalMarks > 0 ? (att.score / att.totalMarks) * 100.0 : 0.0;
                    
                    return (
                      <tr key={att.attemptId} className="hover:bg-gray-50/50 transition-colors">
                        <td className="px-6 py-4">
                          <p className="font-extrabold text-gray-800 text-sm leading-tight">{att.testTitle}</p>
                          <p className="text-[10px] text-gray-400 mt-1 font-mono">Attempt: {att.attemptId.slice(0, 8).toUpperCase()}</p>
                        </td>
                        <td className="px-6 py-4 text-xs font-mono text-gray-500">
                          {att.submittedAt ? formatDateString(att.submittedAt) : 'In progress'}
                        </td>
                        <td className="px-6 py-4 text-center font-mono">
                          {isSubmitted ? (
                            <span className={getScoreCellClass(scorePercentage)}>
                              {scorePercentage.toFixed(1)}%
                            </span>
                          ) : (
                            <span className="text-gray-400 italic text-xs">Unsubmitted</span>
                          )}
                        </td>
                        <td className="px-6 py-4 text-center text-xs font-mono text-gray-500">
                          {att.timeMins ? `${Math.round(att.timeMins)} min` : 'N/A'}
                        </td>
                        <td className="px-6 py-4 text-center">
                          <span className={`inline-flex px-2 py-0.5 rounded-full text-[9px] font-extrabold uppercase leading-5 tracking-wide ${
                            att.status === 'SUBMITTED' ? 'bg-green-100 text-green-800 border border-green-200' :
                            att.status === 'TIMED_OUT' ? 'bg-amber-100 text-amber-800 border border-amber-200' :
                            'bg-gray-100 text-gray-800 border border-gray-200'
                          }`}>
                            {att.status === 'SUBMITTED' ? 'Submitted' :
                             att.status === 'TIMED_OUT' ? 'Timed Out' : 'In Progress'}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-right">
                          {isSubmitted ? (
                            <Link
                              to={`/student/attempts/${att.attemptId}/analytics`}
                              className="bg-white hover:bg-gray-50 border border-gray-300 text-gray-700 hover:text-indigo-600 font-extrabold text-xs uppercase px-3.5 py-1.5 rounded shadow-sm transition-all duration-150 inline-flex items-center gap-1"
                            >
                              View Details
                            </Link>
                          ) : (
                            <Link
                              to={`/student/tests/${att.testId}/take`}
                              className="bg-blue-600 hover:bg-blue-700 text-white font-extrabold text-xs uppercase px-3.5 py-1.5 rounded shadow transition-all duration-150"
                            >
                              Resume Exam
                            </Link>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* SECTION 4 — WEAK QUESTIONS BANK */}
        <div className="bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <BookMarked className="w-5 h-5 text-indigo-600" />
              <h3 className="text-xs font-black uppercase tracking-wider text-slate-800 flex items-center gap-1.5">
                Questions to Revise
                {localQuestionsList.length > 0 && (
                  <span className="bg-red-500 text-white text-[10px] font-black px-2.5 py-0.5 rounded-full">
                    {localQuestionsList.length}
                  </span>
                )}
              </h3>
            </div>
          </div>

          <div className="flex flex-col gap-4">
            {localQuestionsList.length === 0 ? (
              <div className="flex flex-col items-center justify-center text-center py-10">
                <CheckCircle2 className="w-12 h-12 text-green-500 mb-2" />
                <p className="text-xs font-extrabold uppercase text-green-700">Great job! No weak questions to review.</p>
              </div>
            ) : (
              localQuestionsList.map((q) => {
                const isRevised = q.revised === true;
                const isExpanded = expandedTextIds[q.questionId] === true;
                const isExplanationExpanded = expandedExplanationIds[q.questionId] === true;

                return (
                  <div 
                    key={q.questionId} 
                    className={`border border-gray-200 rounded-lg p-5 transition-all duration-200 flex flex-col gap-3.5 ${
                      isRevised ? 'border-l-4 border-l-green-500 bg-green-50/10' : 'bg-white shadow-sm'
                    }`}
                  >
                    {/* Top Row Tags */}
                    <div className="flex items-center gap-2">
                      <span className="bg-gray-100 text-gray-600 text-[10px] font-bold px-2.5 py-0.5 rounded-full">
                        Q{q.sequenceNo}
                      </span>
                      <span className="bg-red-100 text-red-600 text-[10px] font-bold px-2.5 py-0.5 rounded-full">
                        Wrong {q.wrongCount}×
                      </span>
                      {isRevised && (
                        <span className="bg-green-100 text-green-700 text-[10px] font-bold px-2.5 py-0.5 rounded-full flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" />
                          Revised
                        </span>
                      )}
                    </div>

                    {/* Question Text with Collapse */}
                    <div className="text-xs text-gray-800 leading-relaxed font-semibold italic">
                      <p className={isExpanded ? '' : 'overflow-hidden line-clamp-2'}>
                        "{q.questionText}"
                      </p>
                      <button
                        type="button"
                        onClick={() => handleToggleTextExpand(q.questionId)}
                        className="text-[10px] text-blue-600 font-extrabold uppercase tracking-wide mt-1 focus:outline-none block hover:underline"
                      >
                        {isExpanded ? 'Show less' : 'Show more'}
                      </button>
                    </div>

                    {/* Explanation Toggle */}
                    {q.explanation && (
                      <div className="flex flex-col gap-1.5 self-start">
                        <button
                          type="button"
                          onClick={() => handleToggleExplanationExpand(q.questionId)}
                          className="text-[10px] text-indigo-600 hover:text-indigo-800 font-black uppercase flex items-center gap-1 focus:outline-none"
                        >
                          <span>{isExplanationExpanded ? 'Hide Explanation ▲' : 'Show Explanation ▼'}</span>
                        </button>

                        {isExplanationExpanded && (
                          <div className="bg-indigo-50 p-3 rounded text-sm text-indigo-900 leading-relaxed font-medium animate-fadeIn">
                            {q.explanation}
                          </div>
                        )}
                      </div>
                    )}

                    {/* Bottom Row Revised Trigger */}
                    <div className="border-t border-gray-100 pt-3 flex justify-end">
                      <button
                        type="button"
                        onClick={() => handleToggleRevised(q.questionId)}
                        className={`inline-flex items-center gap-1 px-3 py-1.5 rounded text-[10px] font-black uppercase tracking-wider cursor-pointer border transition-all duration-150 ${
                          isRevised 
                            ? 'bg-green-600 text-white border-green-600 hover:bg-green-700' 
                            : 'bg-white hover:bg-gray-50 text-gray-500 border-gray-300'
                        }`}
                      >
                        <span>{isRevised ? 'Revised ✓' : 'Mark as Revised ✓'}</span>
                      </button>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* SECTION 5 — RANK & PERCENTILE BANNER */}
        <div className="bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col sm:flex-row gap-6 items-center">
          <div className="flex-1 flex flex-col gap-3">
            <div>
              <h3 className="text-xs font-black uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <Trophy className="w-4 h-4 text-indigo-600" />
                Your Ranking
              </h3>
              <p className="text-[10px] text-gray-400 mt-0.5 font-semibold">Select an attempted exam paper to view candidate-level standings.</p>
            </div>

            {/* Selector Dropdown */}
            <div className="flex items-center gap-2">
              <span className="text-[10px] text-gray-400 font-bold uppercase shrink-0">Select Paper:</span>
              <select
                value={selectedTestId}
                onChange={(e) => setSelectedTestId(e.target.value)}
                className="bg-white border border-gray-300 rounded-[4px] px-2 py-1 text-xs text-gray-700 font-semibold focus:outline-none focus:border-indigo-500 max-w-full"
              >
                {attemptedTests.length === 0 ? (
                  <option value="">No attempts live yet</option>
                ) : (
                  attemptedTests.map(t => (
                    <option key={t.id} value={t.id}>{t.title}</option>
                  ))
                )}
              </select>
            </div>
          </div>

          {/* Standings display */}
          <div className="w-full sm:w-[45%] bg-slate-50 border border-gray-200 rounded-lg p-5 flex items-center justify-center gap-4 relative min-h-[100px] select-none">
            {rankLoading && (
              <div className="absolute inset-0 bg-white/80 backdrop-blur-sm flex items-center justify-center rounded-lg z-10">
                <Loader2 className="w-6 h-6 text-indigo-600 animate-spin" />
              </div>
            )}

            {!selectedTestId ? (
              <span className="text-xs text-gray-400 italic text-center font-medium">You haven't completed any submitted exams yet.</span>
            ) : rankData ? (
              <div className="flex items-center gap-4 flex-1">
                <div className="flex flex-col items-center justify-center shrink-0">
                  <span className="text-[9px] text-gray-400 font-extrabold uppercase leading-none">Rank</span>
                  <div className="text-3xl font-black text-blue-600">#{rankData.rank}</div>
                  <span className="text-[9px] text-gray-400 font-semibold">out of {rankData.totalStudents}</span>
                </div>
                <div className="flex-1 leading-snug">
                  <p className="text-xs text-gray-500 font-medium">
                    You rank <span className="font-extrabold text-blue-600">#{rankData.rank}</span> out of <span className="font-bold text-slate-800">{rankData.totalStudents}</span> students on <span className="font-bold text-slate-700 italic">"{rankData.testTitle}"</span>.
                  </p>
                  <p className="text-[11px] text-gray-400 font-medium mt-1">
                    You scored better than <span className="font-bold text-green-600 bg-green-50 px-1 py-0.5 rounded border border-green-100">{rankData.percentile}%</span> of candidates.
                  </p>
                </div>
              </div>
            ) : (
              <span className="text-xs text-red-500 italic text-center font-medium">You haven't attempted this test yet.</span>
            )}
          </div>
        </div>

      </main>
    </div>
  )
}
