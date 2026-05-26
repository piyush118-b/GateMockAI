import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ShieldAlert, Database, FileText, CheckSquare, Sparkles, RefreshCw, Loader2, ArrowRight, PlusCircle, Pencil, Trash2, CheckCircle, XCircle, AlertTriangle } from 'lucide-react'

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

  const handleTogglePublish = (testId, isCurrentlyPublished) => {
    setPublishingId(testId);
    const endpoint = isCurrentlyPublished 
      ? `/api/admin/tests/${testId}/unpublish` 
      : `/api/admin/tests/${testId}/publish`;
    fetch(endpoint, { method: 'POST' })
      .then(res => {
        if (!res.ok) throw new Error(isCurrentlyPublished ? 'Unpublish action failed.' : 'Publish action failed.');
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
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans select-none">
      {/* NAVBAR */}
      <nav className="bg-slate-900 text-white h-[64px] px-8 flex justify-between items-center shadow-md select-none shrink-0">
        <div className="flex items-center gap-2.5">
          <div className="bg-blue-600 w-9 h-9 rounded-md flex items-center justify-center font-black text-white text-lg">
            A
          </div>
          <span className="font-extrabold text-sm uppercase tracking-wider text-gray-100">GATE MockAI — Control Center</span>
        </div>

        <div className="flex items-center gap-6">
          <Link 
            to="/admin/analytics"
            className="text-xs uppercase font-extrabold text-gray-400 hover:text-white transition-colors"
          >
            Analytics Dashboard
          </Link>
          <a 
            href="/logout"
            className="text-xs uppercase font-extrabold text-gray-400 hover:text-white transition-colors"
          >
            Sign Out
          </a>
        </div>
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
            <div className="flex items-center gap-4 flex-1 min-w-0">
              <div className="bg-indigo-50 text-indigo-600 p-3.5 rounded-full shrink-0">
                <Database className="w-5 h-5" />
              </div>
              <div className="flex-1 min-w-0">
                <span className="text-[9px] text-gray-400 font-bold uppercase tracking-wider block leading-none">Vector Embeddings (PGVector)</span>
                <h3 className="text-xl font-black text-gray-800 mt-1 truncate">{vectorCount} vectors</h3>
                <p className="text-[9px] text-gray-400 mt-0.5 truncate font-mono">{storePath}</p>
              </div>
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
              <h3 className="text-xs font-black uppercase tracking-wider">PGVector RAG & Seeding</h3>
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

            <div className="border-t border-gray-100 pt-4 flex flex-col gap-2">
              <span className="text-[10px] text-gray-400 font-bold uppercase tracking-wider block">Ingestion Engine</span>
              <p className="text-[11px] text-gray-500 leading-relaxed">
                Upload official exam papers and matching answer key sheets to chunk, auto-classify, explain, and index new RAG sources.
              </p>
              <Link
                to="/admin/rag"
                className="w-full bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-extrabold py-2.5 px-4 rounded-[4px] shadow-sm flex items-center justify-center gap-2 cursor-pointer transition-all duration-150 uppercase text-xs tracking-wider"
              >
                <PlusCircle className="w-4 h-4" />
                <span>Ingest Past Papers (RAG)</span>
              </Link>
            </div>
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
                    <th className="px-6 py-3 text-right">Actions</th>
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
                          (test.isPublished || test.published) 
                            ? 'bg-green-50 text-green-700 border border-green-200' 
                            : 'bg-yellow-50 text-yellow-700 border border-yellow-200'
                        }`}>
                          {(test.isPublished || test.published) ? 'Published' : 'Draft'}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* Publish / Unpublish Toggle */}
                          <button
                            type="button"
                            disabled={publishingId === test.id}
                            onClick={() => handleTogglePublish(test.id, test.isPublished || test.published)}
                            className={`p-1.5 rounded transition-all duration-150 border ${
                              (test.isPublished || test.published)
                                ? 'text-amber-600 bg-amber-50 hover:bg-amber-100 border-amber-200'
                                : 'text-green-600 bg-green-50 hover:bg-green-100 border-green-200'
                            }`}
                            title={(test.isPublished || test.published) ? 'Unpublish / Set to Draft' : 'Publish / Go Live'}
                          >
                            {publishingId === test.id ? (
                              <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (test.isPublished || test.published) ? (
                              <XCircle className="w-4 h-4" />
                            ) : (
                              <CheckCircle className="w-4 h-4" />
                            )}
                          </button>

                          {/* Edit Button */}
                          <Link
                            to={`/admin/tests/${test.id}/edit`}
                            className="p-1.5 text-indigo-600 bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 rounded transition-all duration-150"
                            title="Edit Paper"
                          >
                            <Pencil className="w-4 h-4" />
                          </Link>

                          {/* Delete Button */}
                          <button
                            type="button"
                            onClick={() => setDeleteConfirmTest(test)}
                            className="p-1.5 text-red-600 bg-red-50 hover:bg-red-100 border border-red-200 rounded transition-all duration-150"
                            title="Delete Paper"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </main>

      {/* CUSTOM DELETE CONFIRMATION MODAL */}
      {deleteConfirmTest && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="bg-white border border-slate-200 rounded-xl max-w-md w-full shadow-2xl p-6 flex flex-col gap-4 transform transition-all scale-100">
            <div className="flex items-center gap-3 text-red-600">
              <div className="bg-red-50 p-2 rounded-full">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <h3 className="font-extrabold text-base uppercase tracking-wider text-slate-800">
                Delete Mock Paper?
              </h3>
            </div>
            
            <div className="text-sm text-slate-500 leading-relaxed">
              Are you sure you want to permanently delete <strong className="text-slate-800 font-bold">"{deleteConfirmTest.title}"</strong>?
              <p className="mt-2 text-xs text-red-500 font-medium">
                This action is irreversible. All student progress, grades, and associated exam attempts will be deleted.
              </p>
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={deletingId === deleteConfirmTest.id}
                onClick={() => setDeleteConfirmTest(null)}
                className="px-4 py-2 text-xs font-extrabold uppercase tracking-wide rounded-md text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deletingId === deleteConfirmTest.id}
                onClick={() => handleDeleteTest(deleteConfirmTest.id)}
                className="px-4 py-2 text-xs font-extrabold uppercase tracking-wide rounded-md text-white bg-red-600 hover:bg-red-700 active:bg-red-800 transition-all duration-150 flex items-center justify-center gap-1.5 cursor-pointer shadow-md"
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
    </div>
  )
}
