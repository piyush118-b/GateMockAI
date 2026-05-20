import React, { useEffect, useState, useRef } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ChevronLeft, Loader2, Sparkles, Terminal, AlertCircle, CheckCircle2 } from 'lucide-react'

export default function SseProgressCompiler() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [logs, setLogs] = useState([]);
  const [status, setStatus] = useState('compiling'); // compiling, success, error
  const [errorMessage, setErrorMessage] = useState(null);

  const logsEndRef = useRef(null);

  useEffect(() => {
    // Determine target SSE path based on query parameters
    const branchCode = searchParams.get('branchCode');
    const yearLabel = searchParams.get('yearLabel');
    const weightagesJson = searchParams.get('weightagesJson');

    let sseUrl = '/admin/tests/generate/progress';
    
    if (branchCode && yearLabel && weightagesJson) {
      const q = new URLSearchParams({
        branchCode,
        yearLabel,
        weightagesJson
      }).toString();
      sseUrl = `/admin/tests/generate/progress/weighted?${q}`;
    }

    setLogs(["[System] Initializing RAG compiler threads...", "[System] Querying local high-dimensional vector embeddings..."]);

    const eventSource = new EventSource(sseUrl);

    eventSource.addEventListener('progress', (e) => {
      let data = e.data;
      // Clean JSON double quotes if inlined
      if (data.startsWith('"') && data.endsWith('"')) {
        data = JSON.parse(data);
      }
      setLogs(prev => [...prev, data]);
    });

    eventSource.addEventListener('complete', (e) => {
      setLogs(prev => [...prev, "[Success] AI Mock Test Paper Compiled Successfully!", "[System] Writing indexes to mock exam repository..."]);
      setStatus('success');
      eventSource.close();

      // Pause a couple seconds for reading validation before redirecting back to Admin dashboard
      setTimeout(() => {
        navigate('/admin/dashboard');
      }, 3500);
    });

    eventSource.addEventListener('error', (e) => {
      // Sometimes standard connection closures trigger error events on complete,
      // so only handle as error if we are not already successful
      if (status !== 'success') {
        let msg = "Generative compile failed: Connection closed/timed out.";
        if (e.data) {
          msg = e.data;
        }
        setLogs(prev => [...prev, `[Error] ${msg}`]);
        setStatus('error');
        setErrorMessage(msg);
      }
      eventSource.close();
    });

    return () => {
      eventSource.close();
    };
  }, [searchParams, navigate]);

  // Scroll to bottom on new logs
  useEffect(() => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  return (
    <div className="min-h-screen bg-slate-950 text-gray-100 flex flex-col font-sans select-none">
      {/* NAVBAR */}
      <nav className="bg-slate-900 border-b border-gray-800 text-white h-[64px] px-8 flex justify-between items-center select-none shrink-0">
        <div className="flex items-center gap-2.5">
          <Link 
            to="/admin/dashboard"
            className="hover:bg-white/10 p-1.5 rounded-full text-gray-400 hover:text-white transition-colors mr-1"
          >
            <ChevronLeft className="w-5 h-5" />
          </Link>
          <span className="font-extrabold text-sm uppercase tracking-wider text-gray-100 flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-indigo-400" />
            AI Paper Compiler Stream
          </span>
        </div>
      </nav>

      {/* CORE BODY CONTAINER */}
      <main className="max-w-4xl w-full mx-auto px-6 py-8 flex-1 flex flex-col gap-6 overflow-hidden">
        <div className="border-b border-gray-800 pb-4">
          <h2 className="text-xl font-black uppercase text-gray-100 tracking-tight">Dynamic Exam Compilation Panel</h2>
          <p className="text-xs text-gray-400 font-medium mt-1 leading-relaxed">
            Streaming real-time generation metrics from seed question banks. Wait for completion logs to compile relational tables.
          </p>
        </div>

        {/* TERMINAL VIEW */}
        <div className="flex-1 bg-black border border-gray-800 rounded-lg p-5 shadow-2xl flex flex-col font-mono text-sm overflow-hidden min-h-[360px] relative">
          {/* TERMINAL HEADER HEADER */}
          <div className="flex justify-between items-center border-b border-gray-900 pb-3 mb-4 select-none">
            <div className="flex gap-2">
              <span className="w-3 h-3 bg-red-500 rounded-full" />
              <span className="w-3 h-3 bg-yellow-500 rounded-full" />
              <span className="w-3 h-3 bg-green-500 rounded-full" />
            </div>
            <div className="flex items-center gap-1.5 text-xs text-gray-500 uppercase tracking-widest font-extrabold">
              <Terminal className="w-3.5 h-3.5 text-gray-400" />
              <span>Compilation Terminal logs</span>
            </div>
          </div>

          {/* LOGS INNER */}
          <div className="flex-1 overflow-y-auto space-y-2 pr-1 font-mono text-xs select-none">
            {logs.map((log, idx) => {
              let color = 'text-gray-300';
              if (log.includes('[Error]')) {
                color = 'text-red-500 font-bold';
              } else if (log.includes('[Success]')) {
                color = 'text-green-400 font-extrabold';
              } else if (log.includes('[System]')) {
                color = 'text-cyan-400 font-bold';
              } else if (log.includes('[RAG Compiler]')) {
                color = 'text-indigo-400 font-semibold';
              }
              return (
                <div key={idx} className={`leading-relaxed whitespace-pre-wrap ${color}`}>
                  {log}
                </div>
              )
            })}
            <div ref={logsEndRef} />
          </div>

          {/* COMPILING FLOATER INDICATORS */}
          {status === 'compiling' && (
            <div className="absolute right-6 bottom-6 bg-slate-900 border border-indigo-500/30 px-4 py-2 rounded-md flex items-center gap-2 text-xs font-semibold text-indigo-400 select-none shadow-[0_0_12px_rgba(99,102,241,0.15)] animate-pulse">
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Active LLM Inference...</span>
            </div>
          )}
        </div>

        {/* BOTTOM PERFORMANCE OR ERROR BLOCKS */}
        {status === 'success' && (
          <div className="bg-green-950/60 border border-green-700/50 rounded-lg p-5 flex items-center gap-4 text-green-400 animate-fade-in select-none shadow-[0_0_15px_rgba(34,197,94,0.1)]">
            <CheckCircle2 className="w-10 h-10 text-green-500 shrink-0" />
            <div>
              <h4 className="font-extrabold text-sm uppercase tracking-wider leading-none">Paper compiled successfully!</h4>
              <p className="text-xs text-green-500/80 mt-1.5">
                Dynamic exam paper generated relationally. Redirecting back to the Admin Dashboard shortly...
              </p>
            </div>
          </div>
        )}

        {status === 'error' && (
          <div className="bg-red-950/60 border border-red-700/50 rounded-lg p-5 flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between text-red-400 animate-fade-in select-none shadow-[0_0_15px_rgba(239,68,68,0.1)]">
            <div className="flex gap-4 items-center">
              <AlertCircle className="w-10 h-10 text-red-500 shrink-0" />
              <div>
                <h4 className="font-extrabold text-sm uppercase tracking-wider leading-none">AI Exam compilation failed</h4>
                <p className="text-xs text-red-400/80 mt-1.5">
                  Reason: {errorMessage || 'Inference connection closed or timed out.'}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={() => window.location.reload()}
              className="bg-red-600 hover:bg-red-700 active:bg-red-800 text-white font-extrabold text-xs uppercase px-4 py-2 rounded-[4px] shadow shrink-0"
            >
              Retry Compilation
            </button>
          </div>
        )}
      </main>
    </div>
  )
}
