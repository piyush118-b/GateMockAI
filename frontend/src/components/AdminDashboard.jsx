import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ShieldAlert, Database, FileText, CheckSquare, Sparkles, RefreshCw, Loader2, ArrowRight } from 'lucide-react'

export default function AdminDashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Syncing state
  const [reingesting, setReingesting] = useState(false);
  const [syncMsg, setSyncMsg] = useState(null);

  // Publishing intermediate trackers
  const [publishingId, setPublishingId] = useState(null);

  const fetchDashboard = () => {
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

  const handlePublish = (testId) => {
    setPublishingId(testId);
    fetch(`/api/admin/tests/${testId}/publish`, { method: 'POST' })
      .then(res => {
        if (!res.ok) throw new Error('Publish action failed.');
        return res.json();
      })
      .then(() => {
        setPublishingId(null);
        fetchDashboard(); // reload metrics
      })
      .catch(err => {
        alert(err.message);
        setPublishingId(null);
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
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans select-none">
      {/* NAVBAR */}
      <nav className="bg-slate-900 text-white h-[64px] px-8 flex justify-between items-center shadow-md select-none shrink-0">
        <div className="flex items-center gap-2.5">
          <div className="bg-blue-600 w-9 h-9 rounded-md flex items-center justify-center font-black text-white text-lg">
            A
          </div>
          <span className="font-extrabold text-sm uppercase tracking-wider text-gray-100">GATE MockAI — Control Center</span>
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
        {/* TOP METRICS GRID */}
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-5">
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex items-center gap-4">
            <div className="bg-blue-50 text-blue-600 p-3.5 rounded-full">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[9px] text-gray-400 font-bold uppercase tracking-wider block leading-none">Total Papers</span>
              <h3 className="text-xl font-black text-gray-800 mt-1">{testsCount}</h3>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex items-center gap-4">
            <div className="bg-green-50 text-green-600 p-3.5 rounded-full">
              <CheckSquare className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[9px] text-gray-400 font-bold uppercase tracking-wider block leading-none">Published Papers</span>
              <h3 className="text-xl font-black text-gray-800 mt-1">{publishedCount}</h3>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex items-center gap-4 col-span-2">
            <div className="bg-indigo-50 text-indigo-600 p-3.5 rounded-full">
              <Database className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <span className="text-[9px] text-gray-400 font-bold uppercase tracking-wider block leading-none">Vector Embeddings (PGVector)</span>
              <h3 className="text-xl font-black text-gray-800 mt-1 truncate">{vectorCount} vectors</h3>
              <p className="text-[9px] text-gray-400 mt-0.5 truncate font-mono">{storePath}</p>
            </div>
          </div>
        </div>

        {/* AI EXAM GENERATOR CONTROL DECKS */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 select-none">
          {/* COMPILER TRIGGER CARDS */}
          <div className="bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 text-indigo-700">
                <Sparkles className="w-5 h-5 animate-pulse" />
                <h3 className="text-sm font-black uppercase tracking-wider">AI RAG Exam Compiler</h3>
              </div>
              <p className="text-xs text-gray-500 mt-2 leading-relaxed">
                Compile a full-length, high-fidelity **65-question GATE paper** matching official CSE weightage allocations. It dynamically retrieves questions from PGVector seed stores using local Ollama model alignments.
              </p>
            </div>

            <Link
              to="/admin/generate/progress"
              className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-2.5 px-4 rounded-[4px] shadow flex items-center justify-center gap-2 cursor-pointer transition-all duration-150 uppercase text-xs tracking-wider"
            >
              <span>Compile Full Paper</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {/* WEIGHTED GENERATOR TRIGGER CARDS */}
          <div className="bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 text-blue-700">
                <Sparkles className="w-5 h-5" />
                <h3 className="text-sm font-black uppercase tracking-wider">Dynamic Weighted syllabus Compiler</h3>
              </div>
              <p className="text-xs text-gray-500 mt-2 leading-relaxed">
                Manually distribute marks across standard computer science syllabus subjects (e.g. Operating Systems, SQL databases) to generate custom AI papers tailored to specialized study benchmarks.
              </p>
            </div>

            <Link
              to="/admin/weighted-generator"
              className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2.5 px-4 rounded-[4px] shadow flex items-center justify-center gap-2 cursor-pointer transition-all duration-150 uppercase text-xs tracking-wider"
            >
              <span>Custom Weightage Builder</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>

        {/* BOTTOM DOUBLE GRID: RAG RE-INGEST SEEDING vs MOCK EXAMS LISTS */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 flex-1">
          {/* SEED RE-INGEST DRAWER */}
          <div className="bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col gap-4 self-start">
            <div className="flex items-center gap-2 text-slate-800">
              <Database className="w-5 h-5 text-indigo-600" />
              <h3 className="text-xs font-black uppercase tracking-wider">PGVector seeding & Sync</h3>
            </div>

            <p className="text-xs text-gray-500 leading-relaxed">
              If the database seed questions are updated or PGVector is cleared, use this command to re-chunk and write high-dimension embeddings back to the Postgres Vector store.
            </p>

            {syncMsg && (
              <div className="bg-green-50 border border-green-200 rounded-md p-3 text-xs text-green-700 font-semibold leading-relaxed">
                {syncMsg}
              </div>
            )}

            <button
              type="button"
              disabled={reingesting}
              onClick={handleReingest}
              className="w-full bg-slate-800 hover:bg-slate-900 active:bg-slate-950 disabled:bg-slate-400 text-white font-extrabold py-2.5 px-4 rounded-[4px] shadow-sm flex items-center justify-center gap-2 cursor-pointer transition-all duration-150 uppercase text-xs tracking-wider"
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
          </div>

          {/* MOCK EXAMS LISTINGS */}
          <div className="lg:col-span-2 bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden flex flex-col flex-1">
            <div className="bg-gray-50 border-b border-gray-200 px-6 py-4 flex items-center gap-2 select-none">
              <FileText className="w-5 h-5 text-gray-600" />
              <h3 className="text-xs font-black text-gray-700 uppercase tracking-wider">Relational mock Exam database</h3>
            </div>

            <div className="overflow-x-auto flex-1">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="bg-gray-100 text-gray-500 uppercase text-[9px] tracking-wider border-b border-gray-200 font-bold">
                    <th className="px-6 py-3">Title Description</th>
                    <th className="px-6 py-3">Specs</th>
                    <th className="px-6 py-3 text-center">Questions</th>
                    <th className="px-6 py-3 text-center">Status</th>
                    <th className="px-6 py-3 text-right">Publish Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 font-sans text-gray-700 font-medium">
                  {tests.map((test) => (
                    <tr key={test.id} className="hover:bg-gray-50/50 transition-colors">
                      <td className="px-6 py-4">
                        <p className="font-extrabold text-gray-800 text-sm leading-tight">{test.title}</p>
                        <p className="text-[9px] text-gray-400 mt-1 font-mono">{test.id.slice(0, 8).toUpperCase()}</p>
                      </td>
                      <td className="px-6 py-4 text-xs text-gray-500 leading-normal">
                        <p>{test.topic || 'All Topics'}</p>
                        <p className="text-[10px] text-gray-400 font-semibold">{test.subject || 'Core syllabus'}</p>
                      </td>
                      <td className="px-6 py-4 text-center font-bold text-gray-800 text-xs">
                        {test.totalQuestions}
                      </td>
                      <td className="px-6 py-4 text-center">
                        <span className={`inline-flex px-2 py-0.5 rounded-full text-[9px] font-extrabold uppercase leading-5 tracking-wide ${
                          test.isPublished 
                            ? 'bg-green-50 text-green-700 border border-green-200' 
                            : 'bg-yellow-50 text-yellow-700 border border-yellow-200'
                        }`}>
                          {test.isPublished ? 'Published' : 'Draft'}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right">
                        {test.isPublished ? (
                          <span className="text-[10px] text-gray-400 font-bold uppercase tracking-wider mr-2 select-none">
                            Online
                          </span>
                        ) : (
                          <button
                            type="button"
                            disabled={publishingId === test.id}
                            onClick={() => handleOptionClick || handlePublish(test.id)}
                            className="bg-blue-600 hover:bg-blue-700 text-white font-extrabold text-[10px] uppercase px-3 py-1.5 rounded-[4px] shadow-sm transition-all duration-150 flex items-center justify-center gap-1 cursor-pointer"
                          >
                            {publishingId === test.id ? (
                              <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            ) : (
                              <span>Publish</span>
                            )}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
