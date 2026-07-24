import React, { useEffect, useState, useRef } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ChevronLeft, Loader2, Sparkles, Terminal, AlertCircle, CheckCircle2 } from 'lucide-react'

export default function SseProgressCompiler() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [logs, setLogs] = useState([]);
  const [status, setStatus] = useState('compiling'); // compiling, success, error
  const [errorMessage, setErrorMessage] = useState(null);
  const [newTestId, setNewTestId] = useState(null);
  
  // Cache the original generation query parameters in state
  const [params, setParams] = useState({
    branchCode: null,
    yearLabel: null,
    weightagesJson: null
  });
  
  const [connectTrigger, setConnectTrigger] = useState(0);
  const logsEndRef = useRef(null);

  // Cache parameters on mount
  useEffect(() => {
    setParams({
      branchCode: searchParams.get('branchCode'),
      yearLabel: searchParams.get('yearLabel'),
      weightagesJson: searchParams.get('weightagesJson')
    });
  }, [searchParams]);

  useEffect(() => {
    // Only connect if compiling status is active
    if (status !== 'compiling') return;

    const { branchCode, yearLabel, weightagesJson } = params;
    let sseUrl = '/admin/tests/generate/progress';
    
    if (branchCode && yearLabel && weightagesJson) {
      const q = new URLSearchParams({
        branchCode,
        yearLabel,
        weightagesJson
      }).toString();
      sseUrl = `/admin/tests/generate/progress/weighted?${q}`;
    }

    setLogs([
      "[System] Initializing Gemini AI generation pipeline...",
      "[System] Querying PGVector store for semantic context..."
    ]);

    const eventSource = new EventSource(sseUrl);

    eventSource.addEventListener('progress', (e) => {
      let data = e.data;
      if (data.startsWith('"') && data.endsWith('"')) {
        data = JSON.parse(data);
      }
      try {
        const parsed = JSON.parse(data);
        if (parsed && parsed.message) {
          data = parsed.message;
        }
      } catch (err) {
        // Not a JSON string, keep it as is
      }
      setLogs(prev => [...prev, data]);
    });

    eventSource.addEventListener('complete', (e) => {
      let testPath = e.data; // e.g. "/admin/tests/UUID"
      let testId = testPath.split('/').pop();
      setNewTestId(testId);

      setLogs(prev => [
        ...prev, 
        "[Success] AI Mock Test Paper Compiled Successfully!", 
        "[System] Writing indexes to mock exam repository..."
      ]);
      setStatus('success');
      eventSource.close();
    });

    eventSource.addEventListener('error', (e) => {
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
  }, [params, connectTrigger, status]);

  // Scroll to bottom on new logs
  useEffect(() => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  // Handler for retry compilation
  const handleRetry = () => {
    setLogs([]);
    setErrorMessage(null);
    setNewTestId(null);
    setStatus('compiling');
    setConnectTrigger(prev => prev + 1); // increment key to trigger reconnection
  };

  // Handler for compiling another paper
  const handleCompileAnother = () => {
    setLogs([]);
    setStatus('compiling');
    setErrorMessage(null);
    setNewTestId(null);
    navigate('/admin/tests/generate');
  };

  // Class parser for premium log line color-coding
  const getLogColorClass = (line) => {
    if (typeof line !== 'string') return 'text-[#9CA3AF] font-medium';
    
    if (line.startsWith('[System]')) {
      return 'text-[#67E8F9] font-bold'; // Cyan
    } else if (line.startsWith('[Gemini]')) {
      return 'text-[#A5B4FC] font-semibold'; // Indigo
    } else if (line.startsWith('[Success]')) {
      return 'text-[#86EFAC] font-extrabold'; // Green
    } else if (line.startsWith('[Error]')) {
      return 'text-[#FCA5A5] font-bold'; // Red
    } else if (line.startsWith('[Warning]')) {
      return 'text-[#FCD34D] font-semibold'; // Amber
    }
    return 'text-[#9CA3AF] font-medium'; // Grey
  };

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
          <h2 className="text-xl font-black uppercase text-gray-100 tracking-tight">AI EXAM GENERATION PANEL</h2>
          <p className="text-xs text-gray-400 font-medium mt-1 leading-relaxed">
            Gemini 3.5 Flash generates questions from your PGVector knowledge base. Semantic retrieval + few-shot prompting ensures GATE-quality output.
          </p>
        </div>

        {/* TERMINAL VIEW */}
        <div className="flex-1 bg-black border border-gray-800 rounded-lg p-5 shadow-2xl flex flex-col font-mono text-sm overflow-hidden min-h-[360px] relative">
          {/* TERMINAL HEADER */}
          <div className="flex justify-between items-center border-b border-gray-900 pb-3 mb-4 select-none">
            <div className="flex gap-2">
              <span className="w-3 h-3 bg-red-500 rounded-full" />
              <span className="w-3 h-3 bg-yellow-500 rounded-full" />
              <span className="w-3 h-3 bg-green-500 rounded-full" />
            </div>
            <div className="flex items-center gap-1.5 text-xs text-gray-500 uppercase tracking-widest font-extrabold">
              <Terminal className="w-3.5 h-3.5 text-gray-400" />
              <span>✦ GEMINI GENERATION LOGS</span>
            </div>
          </div>

          {/* LOGS INNER */}
          <div className="flex-1 overflow-y-auto space-y-2 pr-1 font-mono text-xs select-none">
            {logs.map((log, idx) => (
              <div key={idx} className={`leading-relaxed whitespace-pre-wrap ${getLogColorClass(log)}`}>
                {log}
              </div>
            ))}
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

        {/* SUCCESS STATE BLOCK */}
        {status === 'success' && (
          <div className="bg-green-950/60 border border-green-700/50 rounded-lg p-5 flex flex-col gap-4 text-green-400 animate-fade-in select-none shadow-[0_0_15px_rgba(34,197,94,0.1)]">
            <div className="flex items-center gap-4">
              <CheckCircle2 className="w-10 h-10 text-green-500 shrink-0" />
              <div>
                <h4 className="font-extrabold text-sm uppercase tracking-wider leading-none">✓ Exam paper compiled successfully</h4>
                <p className="text-xs text-green-500/80 mt-1.5">
                  Dynamic exam paper generated relationally and indexed to vector store successfully.
                </p>
              </div>
            </div>
            
            <div className="flex flex-wrap gap-3 border-t border-green-700/30 pt-3.5 font-sans">
              <Link
                to={`/admin/tests/${newTestId}/edit`}
                className="bg-green-600 hover:bg-green-700 active:bg-green-800 text-white font-extrabold text-xs uppercase px-4 py-2.5 rounded-[4px] shadow tracking-wider transition-colors inline-block"
              >
                View and publish → [GO TO EDITOR]
              </Link>
              <button
                type="button"
                onClick={handleCompileAnother}
                className="bg-transparent hover:bg-green-800/30 border border-green-600/50 text-green-300 font-extrabold text-xs uppercase px-4 py-2.5 rounded-[4px] tracking-wider transition-colors"
              >
                [COMPILE ANOTHER PAPER]
              </button>
            </div>
          </div>
        )}

        {/* ERROR STATE BLOCK */}
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
              onClick={handleRetry}
              className="bg-red-600 hover:bg-red-700 active:bg-red-800 text-white font-extrabold text-xs uppercase px-4 py-2.5 rounded-[4px] shadow shrink-0 tracking-wider transition-colors"
            >
              Retry Compilation
            </button>
          </div>
        )}
      </main>
    </div>
  )
}
