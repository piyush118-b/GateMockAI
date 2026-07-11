import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChevronLeft, Loader2, Sparkles, Sliders, CheckCircle2, AlertTriangle } from 'lucide-react'

export default function WeightedGenerator() {
  const navigate = useNavigate();

  const [branches, setBranches] = useState([]);
  const [selectedBranchIdx, setSelectedBranchIdx] = useState(0);
  const [yearLabel, setYearLabel] = useState('2026 Practice Paper');
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Subject weightages state map: { subjectName: weight }
  const [weightages, setWeightages] = useState({});

  useEffect(() => {
    fetch('/api/admin/branches')
      .then(res => {
        if (!res.ok) throw new Error('Failed to load branches.');
        return res.json();
      })
      .then(json => {
        setBranches(json);
        if (json.length > 0) {
          // Initialize weightages with standard CSE distribution
          const initial = {};
          const firstBranch = json[0];
          firstBranch.subjects.forEach((subj) => {
            initial[subj.name] = subj.defaultMarksWeightage;
          });
          setWeightages(initial);
        }
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });
  }, []);

  const activeBranch = branches[selectedBranchIdx];

  const handleWeightChange = (subjName, val) => {
    setWeightages(prev => ({
      ...prev,
      [subjName]: Math.max(0, parseInt(val) || 0)
    }));
  };

  const handleBranchChange = (e) => {
    const idx = parseInt(e.target.value);
    setSelectedBranchIdx(idx);
    const targetBranch = branches[idx];
    const initial = {};
    targetBranch.subjects.forEach((subj) => {
      initial[subj.name] = subj.defaultMarksWeightage;
    });
    setWeightages(initial);
  };

  // Enforce NTA GATE Syllabus mark laws:
  // General Aptitude = 15 Marks
  // Engineering Math = 13 Marks (or core math)
  // Remaining Core Topics = 72 Marks
  // Total = 100 Marks!
  const coreSubjects = activeBranch ? activeBranch.subjects.filter(
    s => s.name !== "General Aptitude" && s.name !== "Engineering Mathematics"
  ) : [];

  const coreTotal = coreSubjects.reduce((sum, subj) => sum + (weightages[subj.name] || 0), 0);
  const totalMarks = coreTotal + 15 + 13; 

  const isMarksValid = coreTotal === 72;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!isMarksValid) {
      alert("Error: Total core syllabus subject marks must sum to exactly 72 marks.");
      return;
    }

    const finalWeightages = {
      ...weightages,
      "General Aptitude": 15,
      "Engineering Mathematics": 13
    };

    const weightagesJson = JSON.stringify(finalWeightages);
    const query = new URLSearchParams({
      branchCode: activeBranch.code,
      yearLabel: yearLabel,
      weightagesJson: weightagesJson
    }).toString();

    navigate(`/admin/generate/progress?${query}`);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-2">
          <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
          <span className="text-sm font-semibold text-gray-600">Loading Syllabus Configurator...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4 font-sans">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md text-center">
          <h3 className="text-red-700 font-extrabold text-lg uppercase">Configurator Init Failed</h3>
          <p className="text-sm text-red-600 mt-2 font-medium">{error}</p>
          <Link 
            to="/admin/dashboard"
            className="mt-4 bg-red-600 hover:bg-red-700 text-white font-bold text-xs uppercase px-4 py-2.5 rounded-[4px] inline-block"
          >
            Back to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans select-none">
      {/* NAVBAR */}
      <nav className="bg-slate-900 text-white h-[64px] px-8 flex justify-between items-center shadow-md select-none shrink-0">
        <div className="flex items-center gap-2.5">
          <Link 
            to="/admin/dashboard"
            className="hover:bg-white/10 p-1.5 rounded-full text-gray-400 hover:text-white transition-colors mr-1"
          >
            <ChevronLeft className="w-5 h-5" />
          </Link>
          <span className="font-extrabold text-sm uppercase tracking-wider text-gray-100">Weighted Syllabus Configurator</span>
        </div>
      </nav>

      {/* FORM CONTAINER */}
      <main className="max-w-4xl w-full mx-auto px-6 py-8 flex-1 flex flex-col gap-6">
        <div className="border-b border-gray-200 pb-4 select-none">
          <h2 className="text-2xl font-black uppercase text-gray-800 tracking-tight">AI Dynamic Weightage Generator</h2>
          <p className="text-xs text-gray-400 font-medium mt-1">
            Build custom test papers by allocating exact marks weightages to discrete syllabus modules. Total core marks must aggregate to **72 marks**.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
          {/* LEFT COLUMN: BASIC CONTROLS & MARK SUM TOTALIZER */}
          <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm flex flex-col gap-5">
            <span className="text-[10px] text-gray-400 font-extrabold uppercase tracking-wider border-b border-gray-100 pb-2 block">
              Syllabus Metadata
            </span>

            {/* BRANCH INPUT */}
            <div className="flex flex-col">
              <label className="text-[10px] text-gray-500 font-extrabold uppercase tracking-wider mb-1">Target Academic Branch</label>
              <select
                value={selectedBranchIdx}
                onChange={handleBranchChange}
                className="border border-gray-300 rounded-[4px] px-3 py-2 text-sm bg-white text-gray-700 outline-none focus:border-blue-500 font-bold"
              >
                {branches.map((b, idx) => (
                  <option key={b.id} value={idx}>{b.name} ({b.code})</option>
                ))}
              </select>
            </div>

            {/* YEAR LABEL */}
            <div className="flex flex-col">
              <label className="text-[10px] text-gray-500 font-extrabold uppercase tracking-wider mb-1">Paper Year/Label</label>
              <input
                type="text"
                value={yearLabel}
                onChange={(e) => setYearLabel(e.target.value)}
                className="border border-gray-300 rounded-[4px] px-3 py-2 text-sm text-gray-700 outline-none focus:border-blue-500 font-bold"
                required
              />
            </div>

            {/* TOTALIZER GAUGES */}
            <div className="border-t border-gray-100 pt-4 flex flex-col gap-3">
              <span className="text-[10px] text-gray-400 font-extrabold uppercase tracking-wider block mb-1">Total Marks Summary</span>
              
              <div className="flex justify-between text-xs text-gray-600 font-medium">
                <span>General Aptitude (Fixed):</span>
                <span className="font-mono font-bold text-gray-800">15 Marks</span>
              </div>
              <div className="flex justify-between text-xs text-gray-600 font-medium">
                <span>Engineering Math (Fixed):</span>
                <span className="font-mono font-bold text-gray-800">13 Marks</span>
              </div>
              <div className="flex justify-between text-xs text-gray-600 font-medium border-b border-gray-100 pb-2">
                <span>Core CS Syllabus Subjects:</span>
                <span className={`font-mono font-bold ${isMarksValid ? 'text-green-700' : 'text-orange-600'}`}>{coreTotal} / 72 Marks</span>
              </div>

              <div className="flex justify-between items-center text-sm font-extrabold py-1">
                <span className="text-slate-800 uppercase tracking-wide text-xs">Total Exam Marks:</span>
                <span className={`font-mono text-base ${isMarksValid ? 'text-green-700' : 'text-orange-600'}`}>{totalMarks} / 100</span>
              </div>

              {isMarksValid ? (
                <div className="bg-green-50 border border-green-200 rounded-md p-3 text-xs text-green-700 font-semibold flex items-center gap-2">
                  <CheckCircle2 className="w-4.5 h-4.5 text-green-600 shrink-0" />
                  <span>Marks sum valid. Ready to compile!</span>
                </div>
              ) : (
                <div className="bg-orange-50 border border-orange-200 rounded-md p-3 text-xs text-orange-700 font-semibold flex items-center gap-2">
                  <AlertTriangle className="w-4.5 h-4.5 text-orange-600 shrink-0" />
                  <span>Sum of CS subjects must be exactly 72 marks (current: {coreTotal}).</span>
                </div>
              )}
            </div>

            <button
              type="submit"
              disabled={!isMarksValid}
              className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-bold py-2.5 px-4 rounded-[4px] shadow cursor-pointer transition-all duration-150 uppercase text-xs tracking-wider flex items-center justify-center gap-1.5"
            >
              <Sparkles className="w-4.5 h-4.5 text-yellow-300" />
              <span>Compile dynamic Paper</span>
            </button>
          </div>

          {/* RIGHT COLUMN: LIST OF CORE SUBJECT MARKS SLIDERS */}
          {activeBranch && (
            <div className="lg:col-span-2 bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col gap-5">
              <div className="flex items-center gap-2 text-slate-800 border-b border-gray-100 pb-3">
                <Sliders className="w-5 h-5 text-indigo-600" />
                <h3 className="text-xs font-black uppercase tracking-wider">Subject mark distributions</h3>
              </div>

              <div className="flex flex-col gap-6">
                {coreSubjects.map((subj) => {
                  const val = weightages[subj.name] || 0;
                  
                  return (
                    <div key={subj.id} className="flex flex-col gap-1.5 font-sans">
                      <div className="flex justify-between items-center text-xs font-bold text-gray-700">
                        <span>{subj.name}</span>
                        <span className="font-mono text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-[4px] border border-indigo-100">{val} Marks</span>
                      </div>
                      
                      <div className="flex gap-4 items-center">
                        <input
                          type="range"
                          min="0"
                          max="20"
                          value={val}
                          onChange={(e) => handleWeightChange(subj.name, e.target.value)}
                          className="flex-1 accent-indigo-600 cursor-pointer h-1.5 bg-gray-200 rounded-lg appearance-none"
                        />
                        <input
                          type="number"
                          min="0"
                          max="30"
                          value={val}
                          onChange={(e) => handleWeightChange(subj.name, e.target.value)}
                          className="w-16 border border-gray-300 rounded-[4px] px-2 py-1 text-center font-mono font-bold text-xs bg-white text-gray-700"
                        />
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
        </form>
      </main>
    </div>
  )
}
