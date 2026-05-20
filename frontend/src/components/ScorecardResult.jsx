import React, { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Award, ChevronLeft, Loader2, CheckCircle2, XCircle, HelpCircle, BarChart3, TrendingUp, BookOpen, AlertCircle } from 'lucide-react'
import LatexRenderer from './LatexRenderer'

export default function ScorecardResult() {
  const { attemptId } = useParams();
  
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Active question being reviewed
  const [activeIndex, setActiveIndex] = useState(0);

  useEffect(() => {
    fetch(`/api/student/attempts/${attemptId}/result`)
      .then(res => {
        if (!res.ok) throw new Error('Failed to load exam attempt scorecard.');
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
  }, [attemptId]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-2">
          <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
          <span className="text-sm font-semibold text-gray-600">Generating Scorecard Analysis...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4 font-sans">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md text-center">
          <h3 className="text-red-700 font-extrabold text-lg uppercase">Scorecard Lookup Failed</h3>
          <p className="text-sm text-red-600 mt-2 font-medium">{error}</p>
          <Link 
            to="/student/dashboard"
            className="mt-4 bg-red-600 hover:bg-red-700 text-white font-bold text-xs uppercase px-4 py-2.5 rounded-[4px] inline-block"
          >
            Back to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  const { attempt, test, correctCount, incorrectCount, skippedCount, accuracy, questions } = data;
  const activeQ = questions[activeIndex];

  // Helper to determine palette color based on question review status
  const getQuestionPaletteStyle = (q) => {
    const isCorrect = q.userAnswer?.isCorrect;
    const selected = q.userAnswer?.selectedOptionIds || q.userAnswer?.natValueEntered !== null;

    if (isCorrect === true) {
      return 'bg-green-500 hover:bg-green-600 text-white border-green-600';
    } else if (isCorrect === false) {
      return 'bg-red-500 hover:bg-red-600 text-white border-red-600';
    } else {
      return 'bg-gray-400 hover:bg-gray-500 text-white border-gray-500';
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans select-none">
      {/* NAVBAR */}
      <nav className="bg-slate-900 text-white h-[64px] px-8 flex justify-between items-center shadow-md select-none shrink-0">
        <div className="flex items-center gap-2.5">
          <Link 
            to="/student/dashboard"
            className="hover:bg-white/10 p-1.5 rounded-full text-gray-400 hover:text-white transition-colors mr-1"
          >
            <ChevronLeft className="w-5 h-5" />
          </Link>
          <span className="font-extrabold text-sm uppercase tracking-wider text-gray-100">Exam Performance Scorecard</span>
        </div>
      </nav>

      {/* DASHBOARD CONTAINER */}
      <div className="flex-1 max-w-6xl w-full mx-auto px-6 py-8 flex flex-col gap-6 overflow-y-auto">
        {/* SCORE CARD HEADER */}
        <div className="bg-white border border-gray-200 rounded-lg p-6 shadow-sm flex flex-col md:flex-row gap-6 md:items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="bg-blue-50 text-blue-600 p-4 rounded-full">
              <Award className="w-10 h-10" />
            </div>
            <div>
              <h2 className="text-xl font-black text-gray-800 uppercase tracking-tight leading-tight">{test.title}</h2>
              <p className="text-xs text-gray-400 font-semibold mt-1 uppercase tracking-wider">
                Attempt Finalized on {new Date(attempt.submittedAt).toLocaleString()}
              </p>
            </div>
          </div>

          {/* MAIN STATS GROUP */}
          <div className="flex gap-6 items-center border-t md:border-t-0 md:border-l border-gray-100 pt-4 md:pt-0 md:pl-6">
            <div className="text-center">
              <span className="text-[10px] text-gray-400 font-bold uppercase tracking-wider block">Marks Acquired</span>
              <span className="text-2xl font-black text-blue-700 font-mono mt-0.5 block">
                {attempt.score?.toFixed(2)} <span className="text-xs text-gray-400 font-normal">/ {test.totalMarks}</span>
              </span>
            </div>

            <div className="text-center">
              <span className="text-[10px] text-gray-400 font-bold uppercase tracking-wider block">Accuracy Rate</span>
              <span className="text-2xl font-black text-indigo-700 font-mono mt-0.5 block">
                {accuracy.toFixed(1)}%
              </span>
            </div>
          </div>
        </div>

        {/* METRICS PILLS */}
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
          <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm flex items-center gap-3">
            <CheckCircle2 className="w-8 h-8 text-green-500 shrink-0" />
            <div>
              <p className="text-[9px] text-gray-400 font-bold uppercase tracking-wider leading-none">Correct Questions</p>
              <h4 className="text-lg font-black text-gray-800 mt-1">{correctCount}</h4>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm flex items-center gap-3">
            <XCircle className="w-8 h-8 text-red-500 shrink-0" />
            <div>
              <p className="text-[9px] text-gray-400 font-bold uppercase tracking-wider leading-none">Incorrect Questions</p>
              <h4 className="text-lg font-black text-gray-800 mt-1">{incorrectCount}</h4>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm flex items-center gap-3">
            <HelpCircle className="w-8 h-8 text-gray-400 shrink-0" />
            <div>
              <p className="text-[9px] text-gray-400 font-bold uppercase tracking-wider leading-none">Skipped / Unattempted</p>
              <h4 className="text-lg font-black text-gray-800 mt-1">{skippedCount}</h4>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm flex items-center gap-3">
            <TrendingUp className="w-8 h-8 text-indigo-500 shrink-0" />
            <div>
              <p className="text-[9px] text-gray-400 font-bold uppercase tracking-wider leading-none">Total Questions</p>
              <h4 className="text-lg font-black text-gray-800 mt-1">{questions.length}</h4>
            </div>
          </div>
        </div>

        {/* REVIEW SECTOR SPLIT */}
        <div className="flex-1 bg-white border border-gray-200 rounded-lg shadow-sm flex overflow-hidden min-h-[480px]">
          {/* LEFT REVIEW GRID PALETTE */}
          <div className="w-[280px] border-r border-gray-200 bg-gray-50 p-4 flex flex-col shrink-0 overflow-y-auto">
            <span className="text-[10px] text-gray-500 font-extrabold uppercase tracking-wider border-b border-gray-200 pb-1.5 mb-4">
              Review Palette Selector
            </span>

            <div className="grid grid-cols-4 gap-2">
              {questions.map((q, idx) => (
                <button
                  key={q.id}
                  type="button"
                  onClick={() => setActiveIndex(idx)}
                  className={`h-9 border text-xs flex items-center justify-center font-bold rounded-[4px] cursor-pointer shadow-sm transition-all duration-150 ${
                    getQuestionPaletteStyle(q)
                  } ${
                    activeIndex === idx 
                      ? 'ring-4 ring-blue-500 ring-offset-1 scale-105 z-10 font-black' 
                      : ''
                  }`}
                >
                  {idx + 1}
                </button>
              ))}
            </div>

            <div className="mt-6 border-t border-gray-200 pt-4 flex flex-col gap-2.5 text-[10px] text-gray-600 font-medium">
              <span className="font-bold text-gray-400 uppercase tracking-wider block mb-1">Color Legend:</span>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 bg-green-500 rounded-[4px] border border-green-600" />
                <span>Correct Answer</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 bg-red-500 rounded-[4px] border border-red-600" />
                <span>Incorrect Answer</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 bg-gray-400 rounded-[4px] border border-gray-500" />
                <span>Skipped / Unattempted</span>
              </div>
            </div>
          </div>

          {/* RIGHT REVIEW QUESTION DETAILS VIEW */}
          {activeQ && (
            <div className="flex-1 flex flex-col overflow-y-auto">
              {/* HEADER QUESTION INFO */}
              <div className="bg-slate-950 text-white px-6 py-3 flex justify-between items-center text-xs font-semibold select-none shrink-0 border-b border-gray-800">
                <span>Question review: No. {activeQ.sequenceNo}</span>
                <div className="flex gap-3">
                  <span>Type: <span className="text-cyan-400 font-bold">{activeQ.type}</span></span>
                  <span>Marks: <span className="text-green-400 font-bold">{activeQ.marks}</span></span>
                </div>
              </div>

              {/* CARD CONTAINER */}
              <div className="flex-1 p-6 flex flex-col gap-6 overflow-y-auto">
                {/* QUESTION TEXT */}
                <div className="text-base text-gray-800 leading-relaxed font-normal bg-gray-50 border border-gray-100 p-4 rounded-[4px]">
                  <LatexRenderer text={activeQ.questionText} />
                  {activeQ.imagePath && (
                    <div className="mt-4 border border-gray-200 rounded-[4px] p-1.5 bg-white max-w-lg">
                      <img src={activeQ.imagePath} alt="" className="max-h-80 object-contain" />
                    </div>
                  )}
                </div>

                {/* STUDENT ANSWER HIGHLIGHTER HEADER */}
                <div className="flex items-center gap-3">
                  {activeQ.userAnswer?.isCorrect === true && (
                    <span className="inline-flex items-center gap-1.5 bg-green-50 border border-green-200 text-green-700 px-3 py-1.5 rounded-[4px] font-extrabold text-xs uppercase shadow-sm">
                      <CheckCircle2 className="w-4 h-4 text-green-600" />
                      <span>Your Answer: Correct (+{activeQ.marks} marks)</span>
                    </span>
                  )}
                  {activeQ.userAnswer?.isCorrect === false && (
                    <span className="inline-flex items-center gap-1.5 bg-red-50 border border-red-200 text-red-700 px-3 py-1.5 rounded-[4px] font-extrabold text-xs uppercase shadow-sm">
                      <XCircle className="w-4 h-4 text-red-600" />
                      <span>
                        Your Answer: Incorrect ({activeQ.negativeMarks > 0 ? `-${activeQ.negativeMarks} negative marks` : '0 negative marks'})
                      </span>
                    </span>
                  )}
                  {activeQ.userAnswer?.isCorrect === null && (
                    <span className="inline-flex items-center gap-1.5 bg-gray-50 border border-gray-200 text-gray-600 px-3 py-1.5 rounded-[4px] font-extrabold text-xs uppercase shadow-sm">
                      <HelpCircle className="w-4 h-4 text-gray-500" />
                      <span>Your Answer: Unattempted (0.00 marks)</span>
                    </span>
                  )}
                </div>

                {/* OPTIONS SELECTION VIEW OR NAT ENTRY VALUE DISPLAY */}
                <div className="flex-1">
                  {activeQ.type === 'NAT' ? (
                    <div className="flex flex-col gap-3">
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-xl">
                        <div className="border border-gray-200 rounded-[4px] p-4 bg-gray-50 flex flex-col justify-center">
                          <span className="text-[10px] text-gray-400 font-bold uppercase tracking-wider">Official Correct Range</span>
                          <span className="font-mono text-base font-extrabold text-green-700 mt-1 select-none">
                            {activeQ.correctNatValue} <span className="text-xs text-gray-400 font-sans font-normal">± {activeQ.natTolerance || 0.01}</span>
                          </span>
                        </div>

                        <div className="border border-gray-200 rounded-[4px] p-4 bg-gray-50 flex flex-col justify-center">
                          <span className="text-[10px] text-gray-400 font-bold uppercase tracking-wider">Your Value Entered</span>
                          <span className={`font-mono text-base font-extrabold mt-1 select-none ${
                            activeQ.userAnswer?.isCorrect ? 'text-green-700' : activeQ.userAnswer?.natValueEntered !== null ? 'text-red-700' : 'text-gray-500 italic'
                          }`}>
                            {activeQ.userAnswer?.natValueEntered !== null ? activeQ.userAnswer.natValueEntered : 'Not Entered'}
                          </span>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="flex flex-col gap-3">
                      {activeQ.options.map((opt) => {
                        const isOfficialCorrect = opt.isCorrect;
                        
                        // Check if student selected this option ID
                        const userSel = activeQ.userAnswer?.selectedOptionIds || "";
                        const isStudentSelected = userSel.split(',').includes(opt.id);

                        let cardStyle = 'border-gray-200 bg-white';
                        let outlineLabel = 'bg-white border-gray-300 text-gray-500';

                        if (isOfficialCorrect) {
                          cardStyle = 'border-green-500 bg-green-50/20 shadow-[0_0_8px_rgba(34,197,94,0.15)]';
                          outlineLabel = 'bg-green-600 border-green-600 text-white';
                        } else if (isStudentSelected && !isOfficialCorrect) {
                          cardStyle = 'border-red-500 bg-red-50/20 shadow-[0_0_8px_rgba(239,68,68,0.15)]';
                          outlineLabel = 'bg-red-600 border-red-600 text-white';
                        }

                        return (
                          <div
                            key={opt.id}
                            className={`w-full text-left flex items-start gap-4 p-4 border rounded-[4px] relative ${cardStyle}`}
                          >
                            <div className="flex items-center mt-0.5">
                              <div className={`w-5 h-5 flex items-center justify-center border font-bold text-xs uppercase ${
                                activeQ.type === 'MCQ' ? 'rounded-full' : 'rounded-[2px]'
                              } ${outlineLabel}`}>
                                {opt.optionLabel}
                              </div>
                            </div>

                            <div className="flex-1 text-sm text-gray-700 pt-0.5 select-none">
                              <LatexRenderer text={opt.optionText} />
                              {opt.imagePath && (
                                <div className="mt-3 border border-gray-200 rounded-[4px] p-1.5 bg-white max-w-sm">
                                  <img src={opt.imagePath} alt="" className="max-h-48 object-contain" />
                                </div>
                              )}
                            </div>

                            {/* SMALL ABSOLUTE STATUS BADGES */}
                            <div className="absolute right-4 top-4 flex gap-1.5">
                              {isOfficialCorrect && (
                                <span className="bg-green-100 text-green-800 text-[9px] font-extrabold uppercase px-2 py-0.5 rounded-[2px] border border-green-200 tracking-wide">
                                  Official Correct
                                </span>
                              )}
                              {isStudentSelected && (
                                <span className={`${
                                  isOfficialCorrect ? 'bg-blue-100 text-blue-800 border-blue-200' : 'bg-red-100 text-red-800 border-red-200'
                                } text-[9px] font-extrabold uppercase px-2 py-0.5 rounded-[2px] border tracking-wide`}>
                                  Your Choice
                                </span>
                              )}
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  )}
                </div>

                {/* DETAILED EXPLANATION DRAWER */}
                {activeQ.explanation && (
                  <div className="mt-4 bg-slate-50 border border-slate-200 rounded-md p-5 select-none">
                    <div className="flex items-center gap-2 text-slate-800 font-extrabold text-xs uppercase tracking-wider mb-2">
                      <BookOpen className="w-4 h-4 text-indigo-600 animate-pulse" />
                      <span>Academic Solution & Explanation Analysis</span>
                    </div>
                    <div className="text-sm text-slate-700 leading-relaxed font-sans mt-1">
                      <LatexRenderer text={activeQ.explanation} />
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
