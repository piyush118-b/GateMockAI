import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ShieldAlert, Database, FileText, CheckSquare, Sparkles, RefreshCw, Loader2, ArrowRight, PlusCircle, Pencil, Trash2, CheckCircle, XCircle, AlertTriangle, Check } from 'lucide-react'

export default function AdminDashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Syncing state
  const [reingesting, setReingesting] = useState(false);
  const [syncMsg, setSyncMsg] = useState(null);

  // Action intermediate trackers
  const [publishingId, setPublishingId] = useState(null);
  const [deleteConfirmTest, setDeleteConfirmTest] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  // Gemini token usage telemetry
  const [tokenUsage, setTokenUsage] = useState(null);
  const [quotaExceeded, setQuotaExceeded] = useState(false);

  // Gemini status and Toast state
  const [geminiStatus, setGeminiStatus] = useState(null);
  const [toast, setToast] = useState(null);

  // Review queue pending count
  const [reviewCount, setReviewCount] = useState(0);

  const showToast = (message) => {
    setToast(message);
    setTimeout(() => {
      setToast(null);
    }, 4000);
  };

  const fetchDashboard = () => {
    // Fetch today's token usage details
    fetch('/api/admin/token-usage/today')
      .then(res => {
        if (res.status === 429) {
          setQuotaExceeded(true);
          return res.json();
        }
        if (!res.ok) throw new Error('Failed to load token usage stats.');
        return res.json();
      })
      .then(json => {
        if (json) {
          setTokenUsage(json);
          if (json.totalTokens >= json.limitTokens) {
            setQuotaExceeded(true);
          }
        }
      })
      .catch(err => {
        console.warn('Failed to load token usage details:', err.message);
      });

    // Fetch Gemini API status
    fetch('/api/admin/gemini-status')
      .then(res => {
        if (!res.ok) throw new Error('Failed to load Gemini status.');
        return res.json();
      })
      .then(json => {
        setGeminiStatus(json);
      })
      .catch(err => {
        console.warn('Failed to load Gemini API status:', err.message);
      });

    fetch('/api/admin/review/queue/count')
      .then(res => res.ok ? res.json() : { unresolvedCount: 0 })
      .then(json => setReviewCount(json.unresolvedCount || 0))
      .catch(() => {});

    fetch('/api/admin/dashboard')
      .then(res => {
        if (!res.ok) throw new Error('Failed to load admin dashboard REST metrics.');
        return res.json();
      })
      .then(json => {
        setData(json);
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchDashboard();
  }, []);

  const handlePublishTest = (testId) => {
    setPublishingId(testId);
    fetch(`/api/admin/mock-tests/${testId}/publish`, { method: 'PUT' })
      .then(res => {
        if (!res.ok) throw new Error('Publish action failed.');
        return res.json();
      })
      .then((updatedTest) => {
        setPublishingId(null);
        showToast("Paper published — students can now see it");
        setData(prevData => {
          if (!prevData) return prevData;
          return {
            ...prevData,
            publishedCount: prevData.publishedCount + 1,
            tests: prevData.tests.map(t => t.id === testId ? { ...t, isPublished: true, published: true } : t)
          };
        });
      })
      .catch(err => {
        alert(err.message);
        setPublishingId(null);
      });
  };

  const handleDeleteTest = (testId) => {
    setDeletingId(testId);
    fetch(`/api/admin/tests/${testId}`, { method: 'DELETE' })
      .then(res => {
        if (!res.ok) throw new Error('Delete action failed.');
        return res.json();
      })
      .then(() => {
        setDeletingId(null);
        setDeleteConfirmTest(null);
        fetchDashboard(); // reload metrics
      })
      .catch(err => {
        alert(err.message);
        setDeletingId(null);
      });
  };

  const handleReingest = () => {
    setReingesting(true);
    setSyncMsg(null);
    fetch('/api/admin/rag/reingest', { method: 'POST' })
      .then(res => {
        if (!res.ok) throw new Error('Reingestion failed.');
        return res.json();
      })
      .then(json => {
        setReingesting(false);
        setSyncMsg(json.message);
        fetchDashboard(); // reload vectors
      })
      .catch(err => {
        alert(err.message);
        setReingesting(false);
      });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-2">
          <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
          <span className="text-sm font-semibold text-gray-600">Loading Administrative Portal...</span>
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

  const { testsCount, publishedCount, vectorCount, storePath, tests } = data;

  return (
    <div className="admin-theme min-h-screen bg-[#070b13] text-slate-100 flex flex-col font-sans select-none relative overflow-x-hidden">
      {/* Ambient background glows */}
      <div className="absolute top-[-20%] left-[-10%] w-[600px] h-[600px] rounded-full bg-indigo-600/10 blur-[120px] pointer-events-none z-0" />
      <div className="absolute bottom-[-10%] right-[-5%] w-[500px] h-[500px] rounded-full bg-purple-600/5 blur-[120px] pointer-events-none z-0" />

      {/* NAVBAR */}
      <nav className="bg-slate-950/70 border-b border-slate-800/80 backdrop-blur-md text-white h-[64px] px-8 flex justify-between items-center shadow-lg shadow-slate-950/20 select-none shrink-0 relative z-10">
        <div className="flex items-center gap-3">
          <div className="bg-gradient-to-tr from-indigo-500 to-purple-600 w-9 h-9 rounded-lg flex items-center justify-center font-black text-white text-lg shadow-md shadow-indigo-500/20">
            G
          </div>
          <div className="flex flex-col">
            <span className="font-black text-sm uppercase tracking-wider text-white leading-tight">GATE MockAI</span>
            <span className="text-[9px] text-indigo-400 font-extrabold uppercase tracking-widest leading-none">Administrative Portal</span>
          </div>
        </div>

        <div className="flex items-center gap-6">
          <Link 
            to="/admin/review-queue"
            className="relative text-xs uppercase font-black text-slate-400 hover:text-white transition-colors flex items-center gap-2"
          >
            Review Queue
            {reviewCount > 0 && (
              <span className="absolute -top-2 -right-3.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-[9px] font-black rounded-full min-w-[16px] h-4 flex items-center justify-center px-1 shadow-md shadow-orange-500/20 animate-pulse">
                {reviewCount}
              </span>
            )}
          </Link>
          <Link 
            to="/admin/analytics"
            className="text-xs uppercase font-black text-slate-400 hover:text-white transition-colors"
          >
            Analytics
          </Link>
          <a 
            href="/logout"
            className="text-xs uppercase font-black text-rose-400 hover:text-rose-300 transition-colors"
          >
            Sign Out
          </a>
        </div>
      </nav>

      {/* DASHBOARD BODY */}
      <main className="max-w-6xl w-full mx-auto px-6 py-8 flex flex-col gap-6 flex-1 relative z-10">
        
        {/* API QUOTA BANNERS */}
        {quotaExceeded && (
          <div className="bg-red-950/60 border border-red-800/80 rounded-xl p-4 flex items-start gap-3 text-red-200 shadow-xl shadow-red-950/30 backdrop-blur-md animate-fade-in">
            <XCircle className="w-5 h-5 shrink-0 mt-0.5 text-red-500" />
            <div>
              <p className="font-extrabold text-sm uppercase tracking-wide">🚫 Daily token limit reached</p>
              <p className="text-xs text-red-300 mt-1">All AI operations (past paper ingestion and test generation) are paused until midnight IST.</p>
            </div>
          </div>
        )}
        {!quotaExceeded && tokenUsage && tokenUsage.remainingTokens < 50000 && (
          <div className="bg-amber-950/60 border border-amber-800/80 rounded-xl p-4 flex items-start gap-3 text-amber-200 shadow-xl shadow-amber-950/30 backdrop-blur-md animate-pulse">
            <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5 text-amber-500" />
            <div>
              <p className="font-extrabold text-sm uppercase tracking-wide">⚠ Gemini quota nearly exhausted</p>
              <p className="text-xs text-amber-300 mt-1">Only {tokenUsage.remainingTokens.toLocaleString()} tokens left today. Ingestion and generation are paused until midnight.</p>
            </div>
          </div>
        )}

        {/* TOP METRICS GRID */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          
          {/* Card 1: Total Papers */}
          <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-5 shadow-xl shadow-slate-950/10 flex items-center gap-4 glow-card transition-all duration-300">
            <div className="bg-blue-500/10 border border-blue-500/20 text-blue-400 p-3 rounded-lg shrink-0">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider block leading-none">Total Papers</span>
              <h3 className="text-2xl font-black text-white mt-1.5">{testsCount}</h3>
            </div>
          </div>

          {/* Card 2: Published */}
          <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-5 shadow-xl shadow-slate-950/10 flex items-center gap-4 glow-card transition-all duration-300">
            <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 p-3 rounded-lg shrink-0">
              <CheckSquare className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider block leading-none">Published Papers</span>
              <h3 className="text-2xl font-black text-white mt-1.5">{publishedCount}</h3>
            </div>
          </div>

          {/* Card 3: Vectors */}
          <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-5 shadow-xl shadow-slate-950/10 flex items-center gap-4 glow-card transition-all duration-300">
            <div className="bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 p-3 rounded-lg shrink-0">
              <Database className="w-5 h-5" />
            </div>
            <div className="min-w-0 flex-1">
              <span className="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider block leading-none">Vector Embeddings</span>
              <h3 className="text-2xl font-black text-white mt-1.5 truncate">{vectorCount}</h3>
              <p className="text-[9px] text-slate-400 mt-0.5 truncate font-mono">PostgreSQL · pgvector</p>
            </div>
          </div>

          {/* Card 4: Tokens */}
          <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-5 shadow-xl shadow-slate-950/10 flex flex-col justify-between min-h-[96px] glow-card transition-all duration-300">
            <div className="flex items-center gap-4">
              <div className="bg-purple-500/10 border border-purple-500/20 text-purple-400 p-3 rounded-lg shrink-0">
                <Sparkles className="w-5 h-5 animate-pulse" />
              </div>
              <div className="flex-1 min-w-0">
                <span className="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider block leading-none">API TOKENS TODAY</span>
                <h3 className="text-lg font-black text-white mt-1 truncate">
                  {tokenUsage ? `${(tokenUsage.totalTokens || 0).toLocaleString()} / ${(tokenUsage.limitTokens || 500000) / 1000}K` : '0 / 500K'}
                </h3>
              </div>
            </div>
            {tokenUsage && (
              <div className="mt-2.5">
                <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                  <div 
                    className={`h-full rounded-full transition-all duration-300 ${
                      (tokenUsage.totalTokens / tokenUsage.limitTokens) > 0.8 ? 'bg-red-500' :
                      (tokenUsage.totalTokens / tokenUsage.limitTokens) > 0.5 ? 'bg-amber-500' : 'bg-gradient-to-r from-emerald-500 to-indigo-500'
                    }`}
                    style={{ width: `${Math.min(100, ((tokenUsage.totalTokens || 0) / (tokenUsage.limitTokens || 500000)) * 100)}%` }}
                  />
                </div>
                <div className="flex justify-between items-center mt-1 text-[9px] text-slate-400 font-bold font-mono">
                  <span>Est: ${tokenUsage.estimatedCostUsd ? tokenUsage.estimatedCostUsd.toFixed(4) : '0.0000'}</span>
                  <span>{Math.round(((tokenUsage.totalTokens || 0) / (tokenUsage.limitTokens || 500000)) * 100)}%</span>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* AI EXAM GENERATOR CONTROL DECKS */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 select-none">
          
          {/* Card 1: Compiler */}
          <div className="bg-slate-900/30 border border-slate-800 rounded-xl p-6 shadow-xl shadow-slate-950/20 flex flex-col justify-between gap-5 relative overflow-hidden group hover:border-indigo-500/40 transition-all duration-300">
            <div className="absolute top-0 right-0 w-24 h-24 bg-indigo-500/5 rounded-full blur-2xl pointer-events-none group-hover:bg-indigo-500/10 transition-all duration-300" />
            <div>
              <div className="flex items-center gap-2.5 text-indigo-400">
                <Sparkles className="w-5 h-5 animate-pulse" />
                <h3 className="text-sm font-black uppercase tracking-wider">AI RAG Exam Compiler</h3>
              </div>
              <p className="text-xs text-slate-400 mt-2.5 leading-relaxed">
                Compile a full-length, high-fidelity 65-question GATE paper matching official CSE weightage allocations. Dynamically retrieves questions from PGVector semantic stores and reranks using Gemini AI.
              </p>
            </div>

            <Link
              to="/admin/generate/progress"
              className="bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-500 hover:to-indigo-600 text-white font-bold py-2.5 px-4 rounded-lg shadow-md shadow-indigo-950/50 flex items-center justify-center gap-2 cursor-pointer transition-all duration-200 uppercase text-xs tracking-wider"
            >
              <span>Compile Full Paper</span>
              <ArrowRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
            </Link>
          </div>

          {/* Card 2: Weighted syllabuses */}
          <div className="bg-slate-900/30 border border-slate-800 rounded-xl p-6 shadow-xl shadow-slate-950/20 flex flex-col justify-between gap-5 relative overflow-hidden group hover:border-blue-500/40 transition-all duration-300">
            <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 rounded-full blur-2xl pointer-events-none group-hover:bg-blue-500/10 transition-all duration-300" />
            <div>
              <div className="flex items-center gap-2.5 text-blue-400">
                <Sparkles className="w-5 h-5" />
                <h3 className="text-sm font-black uppercase tracking-wider">Dynamic Weighted syllabus Compiler</h3>
              </div>
              <p className="text-xs text-slate-400 mt-2.5 leading-relaxed">
                Manually distribute marks across standard computer science syllabus subjects (e.g. Operating Systems, SQL databases) to generate custom AI papers tailored to specialized study benchmarks.
              </p>
            </div>

            <Link
              to="/admin/weighted-generator"
              className="bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-500 hover:to-blue-600 text-white font-bold py-2.5 px-4 rounded-lg shadow-md shadow-blue-950/50 flex items-center justify-center gap-2 cursor-pointer transition-all duration-200 uppercase text-xs tracking-wider"
            >
              <span>Custom Weightage Builder</span>
              <ArrowRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
            </Link>
          </div>
        </div>

        {/* BOTTOM DOUBLE GRID: RAG RE-INGEST SEEDING vs MOCK EXAMS LISTS */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 flex-1 items-start">
          
          {/* SEED RE-INGEST DRAWER */}
          <div className="bg-slate-900/30 border border-slate-800 rounded-xl p-6 shadow-xl shadow-slate-950/15 flex flex-col gap-4 self-start">
            <div className="flex items-center gap-2 text-slate-100">
              <Database className="w-5 h-5 text-indigo-400" />
              <h3 className="text-xs font-black uppercase tracking-wider">PGVector RAG & Seeding</h3>
            </div>

            <p className="text-xs text-slate-400 leading-relaxed">
              If the database seed questions are updated or PGVector is cleared, use this command to re-chunk and write high-dimension embeddings back to the Postgres Vector store.
            </p>

            {syncMsg && (
              <div className="bg-emerald-950/50 border border-emerald-800/80 rounded-lg p-3 text-xs text-emerald-300 font-semibold leading-relaxed">
                {syncMsg}
              </div>
            )}

            <button
              type="button"
              disabled={reingesting}
              onClick={handleReingest}
              className="w-full bg-slate-800 hover:bg-slate-700 active:bg-slate-900 disabled:bg-slate-900 disabled:text-slate-500 text-white font-black py-2.5 px-4 rounded-lg shadow flex items-center justify-center gap-2 cursor-pointer transition-all duration-200 uppercase text-xs tracking-wider"
            >
              {reingesting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin text-white" />
                  <span>Embedding seed Bank...</span>
                </>
              ) : (
                <>
                  <RefreshCw className="w-4 h-4" />
                  <span>Seeding PGVector</span>
                </>
              )}
            </button>

            <div className="border-t border-slate-800/80 pt-4 flex flex-col gap-2.5">
              <span className="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider block">Ingestion Engine</span>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Upload official GATE exam papers. Gemini AI extracts, solves, and enriches all questions in one pass — no answer keys required.
              </p>
              <div className="flex flex-col gap-2 mt-1">
                <Link
                  to="/admin/rag"
                  className="bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-500 hover:to-indigo-600 text-white font-black py-2.5 px-4 rounded-lg shadow-md shadow-indigo-950/50 flex items-center justify-center gap-2 cursor-pointer transition-all duration-200 uppercase text-xs tracking-wider text-center"
                >
                  <PlusCircle className="w-4 h-4" />
                  <span>Ingest Past Papers (RAG)</span>
                </Link>
                {geminiStatus && (
                  <span className={`px-2.5 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-wider flex items-center gap-1.5 border justify-center ${
                    geminiStatus.connected
                      ? 'bg-emerald-950/40 text-emerald-400 border-emerald-800/50'
                      : 'bg-red-950/40 text-red-400 border-red-800/50'
                  }`}>
                    <span className={`w-1.5 h-1.5 rounded-full ${geminiStatus.connected ? 'bg-emerald-500' : 'bg-red-500'}`} />
                    {geminiStatus.connected ? 'Gemini 3.5 Flash Connected' : 'API Key Not Set'}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* MOCK EXAMS LISTINGS */}
          <div className="lg:col-span-2 bg-slate-900/30 border border-slate-800 rounded-xl shadow-xl shadow-slate-950/15 overflow-hidden flex flex-col">
            <div className="bg-slate-950/40 border-b border-slate-800/80 px-6 py-4 flex items-center gap-2.5 select-none">
              <FileText className="w-5 h-5 text-slate-400" />
              <h3 className="text-xs font-black text-slate-300 uppercase tracking-wider">Relational mock Exam database</h3>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="bg-slate-950/20 text-slate-400 uppercase text-[9px] tracking-wider border-b border-slate-800 font-extrabold">
                    <th className="px-6 py-4">Title Description</th>
                    <th className="px-6 py-4">Specs</th>
                    <th className="px-6 py-4 text-center">Questions</th>
                    <th className="px-6 py-4 text-center">Status</th>
                    <th className="px-6 py-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80 text-slate-300 font-medium">
                  {tests.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="px-6 py-10 text-center text-slate-500 text-xs font-medium">
                        No papers found in the database. Ingest one to get started!
                      </td>
                    </tr>
                  ) : (
                    tests.map((test) => (
                      <tr key={test.id} className="hover:bg-slate-900/20 transition-colors">
                        <td className="px-6 py-4">
                          <p className="font-extrabold text-white text-sm leading-tight">{test.title}</p>
                          <p className="text-[9px] text-slate-500 mt-1 font-mono">{test.id.slice(0, 8).toUpperCase()}</p>
                        </td>
                        <td className="px-6 py-4 text-xs text-slate-400 leading-normal">
                          <p className="font-semibold">{test.topic || 'All Topics'}</p>
                          <p className="text-[10px] text-slate-500 mt-0.5">{test.subject || 'Core syllabus'}</p>
                        </td>
                        <td className="px-6 py-4 text-center font-black text-white text-xs">
                          {test.totalQuestions}
                        </td>
                        <td className="px-6 py-4 text-center">
                          <span className={`inline-flex px-2.5 py-1 rounded-full text-[9px] font-black uppercase tracking-wide leading-none ${
                            (test.isPublished || test.published) 
                              ? 'bg-emerald-950/50 text-emerald-400 border border-emerald-800/50' 
                              : 'bg-amber-950/50 text-amber-400 border border-amber-800/50'
                          }`}>
                            {(test.isPublished || test.published) ? 'Published' : 'Draft'}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              type="button"
                              disabled={publishingId === test.id || (test.isPublished || test.published)}
                              onClick={() => handlePublishTest(test.id)}
                              className={`p-1.5 rounded-lg transition-all duration-200 border ${
                                (test.isPublished || test.published)
                                  ? 'text-slate-600 bg-slate-900/30 border-slate-800/50 cursor-not-allowed'
                                  : 'text-emerald-400 bg-emerald-500/10 hover:bg-emerald-500/20 border-emerald-500/20'
                              }`}
                              title={(test.isPublished || test.published) ? 'Paper Published' : 'Publish / Go Live'}
                            >
                              {publishingId === test.id ? (
                                <Loader2 className="w-4 h-4 animate-spin" />
                              ) : (test.isPublished || test.published) ? (
                                <Check className="w-4 h-4" />
                              ) : (
                                <CheckCircle className="w-4 h-4" />
                              )}
                            </button>

                            {/* Edit Button */}
                            <Link
                              to={`/admin/tests/${test.id}/edit`}
                              className="p-1.5 text-indigo-400 bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/20 rounded-lg transition-all duration-200"
                              title="Edit Paper"
                            >
                              <Pencil className="w-4 h-4" />
                            </Link>

                            {/* Delete Button */}
                            <button
                              type="button"
                              onClick={() => setDeleteConfirmTest(test)}
                              className="p-1.5 text-red-400 bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 rounded-lg transition-all duration-200"
                              title="Delete Paper"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </main>

      {/* CUSTOM DELETE CONFIRMATION MODAL */}
      {deleteConfirmTest && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-md w-full shadow-2xl p-6 flex flex-col gap-4 transform transition-all scale-100">
            <div className="flex items-center gap-3 text-red-400">
              <div className="bg-red-500/10 border border-red-500/20 p-2 rounded-lg">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <h3 className="font-black text-base uppercase tracking-wider text-white">
                Delete Mock Paper?
              </h3>
            </div>
            
            <div className="text-sm text-slate-400 leading-relaxed">
              Are you sure you want to permanently delete <strong className="text-white font-bold">"{deleteConfirmTest.title}"</strong>?
              <p className="mt-2 text-xs text-red-400 font-medium bg-red-950/20 border border-red-950 p-2.5 rounded-lg">
                This action is irreversible. All student progress, grades, and associated exam attempts will be deleted.
              </p>
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={deletingId === deleteConfirmTest.id}
                onClick={() => setDeleteConfirmTest(null)}
                className="px-4 py-2 text-xs font-black uppercase tracking-wide rounded-md text-slate-400 bg-slate-800 hover:bg-slate-700 transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deletingId === deleteConfirmTest.id}
                onClick={() => handleDeleteTest(deleteConfirmTest.id)}
                className="px-4 py-2 text-xs font-black uppercase tracking-wide rounded-md text-white bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 transition-all duration-200 flex items-center justify-center gap-1.5 cursor-pointer shadow-md shadow-red-950/50"
              >
                {deletingId === deleteConfirmTest.id ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Deleting...</span>
                  </>
                ) : (
                  <>
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Delete Paper</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-5 right-5 bg-slate-950 text-white px-4 py-3 rounded-lg shadow-xl flex items-center gap-2.5 z-50 animate-fade-in border border-slate-800 font-sans">
          <CheckCircle className="w-5 h-5 text-emerald-500 shrink-0" />
          <span className="text-xs font-bold">{toast}</span>
        </div>
      )}
    </div>
  )
}

