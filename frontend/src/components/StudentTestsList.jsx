import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { PlayCircle, ShieldCheck, Clock, FileText, Award, ChevronLeft, Loader2, LogOut } from 'lucide-react'

export default function StudentTestsList() {
  const [tests, setTests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Proctor instructions checklist popup
  const [selectedTest, setSelectedTest] = useState(null);

  const navigate = useNavigate();

  useEffect(() => {
    fetch('/api/student/tests')
      .then(res => {
        if (res.status === 401 || res.status === 403) {
          navigate('/login');
          return null;
        }
        if (!res.ok) throw new Error('Failed to load mock exams.');
        return res.json();
      })
      .then(json => {
        if (json) {
          setTests(json);
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

  const handleLaunchClick = (test) => {
    setSelectedTest(test);
  };

  const handleConfirmLaunch = async () => {
    if (!selectedTest) return;
    
    // Request fullscreen on document
    try {
      const elem = document.documentElement;
      if (elem.requestFullscreen) {
        await elem.requestFullscreen();
      } else if (elem.webkitRequestFullscreen) {
        await elem.webkitRequestFullscreen();
      } else if (elem.msRequestFullscreen) {
        await elem.msRequestFullscreen();
      }
    } catch (err) {
      console.warn("Fullscreen request bypass/denied:", err);
    }
    
    const targetId = selectedTest.id;
    setSelectedTest(null);
    navigate(`/student/tests/${targetId}/login`);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-2">
          <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
          <span className="text-sm font-semibold text-gray-600">Loading Available Exams...</span>
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

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans select-none">
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
          <Link to="/student/dashboard" className="text-xs uppercase font-extrabold text-gray-400 hover:text-white transition-colors pb-1">
            My Progress
          </Link>
          <Link to="/student/tests" className="text-xs uppercase font-extrabold text-blue-500 border-b-2 border-blue-500 pb-1">
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

      {/* BODY */}
      <main className="max-w-6xl w-full mx-auto px-6 py-8 flex-1 flex flex-col gap-6">
        <div className="border-b border-gray-200 pb-4">
          <h2 className="text-2xl font-black uppercase text-gray-800 tracking-tight">Available GATE Mock Exams</h2>
          <p className="text-xs text-gray-400 font-medium mt-1">
            Choose a mock paper from the seed bank or compiled AI papers below. All sessions run under strict fullscreen proctoring controls.
          </p>
        </div>

        {tests.length === 0 ? (
          <div className="bg-white border border-gray-200 rounded-lg p-16 text-center shadow-sm flex flex-col items-center">
            <FileText className="w-12 h-12 text-gray-300 mx-auto" />
            <h3 className="text-gray-800 font-extrabold text-lg mt-4">No exams published yet</h3>
            <p className="text-gray-500 font-medium text-xs mt-1">The admin is preparing mock papers. Check back soon!</p>
            
            <div className="mt-8 flex flex-col items-center gap-3">
              <span className="text-[10px] text-gray-400 font-bold uppercase tracking-wider">Coming soon:</span>
              <div className="flex flex-wrap justify-center gap-2">
                {["Operating Systems", "Algorithms", "Computer Networks"].map((topic, index) => (
                  <span 
                    key={index} 
                    className="inline-flex bg-gray-50 text-gray-500 text-[10px] font-extrabold uppercase px-2.5 py-1 border border-gray-200 rounded-full tracking-wide font-sans"
                  >
                    {topic}
                  </span>
                ))}
              </div>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {tests.map((test) => (
              <div 
                key={test.id} 
                className="bg-white border border-gray-200 hover:border-blue-400 rounded-lg p-5 shadow-sm hover:shadow-md transition-all duration-200 flex flex-col justify-between group"
              >
                {/* HEAD DETAILS */}
                <div>
                  <div className="flex justify-between items-start gap-2 mb-3">
                    <span className="inline-flex bg-blue-50 text-blue-700 text-[10px] font-extrabold uppercase px-2 py-0.5 border border-blue-200 rounded-full tracking-wide">
                      {test.branch || 'GATE Exam'}
                    </span>
                    <span className="text-[10px] text-gray-400 font-bold tracking-tight">
                      {test.yearLabel || 'Practice Paper'}
                    </span>
                  </div>

                  <h3 className="text-base font-extrabold text-gray-800 tracking-tight group-hover:text-blue-700 leading-snug line-clamp-2">
                    {test.title}
                  </h3>

                  <p className="text-xs text-gray-400 italic mt-1 truncate">
                    Subject: {test.subject || 'All Subjects'} • Topic: {test.topic || 'General Syllabus'}
                  </p>
                </div>

                {/* SPEC SHEET */}
                <div className="my-5 border-t border-b border-gray-100 py-3.5 flex justify-between text-xs text-gray-600 font-medium select-none">
                  <div className="flex items-center gap-1.5">
                    <Clock className="w-4 h-4 text-gray-400" />
                    <span>{test.durationMinutes} Min</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <FileText className="w-4 h-4 text-gray-400" />
                    <span>{test.totalQuestions} Questions</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Award className="w-4 h-4 text-gray-400" />
                    <span>{test.totalMarks} Marks</span>
                  </div>
                </div>

                {/* LAUNCH BUTTON */}
                <button
                  type="button"
                  onClick={() => handleLaunchClick(test)}
                  className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-2.5 px-4 rounded-[4px] shadow cursor-pointer transition-all duration-150 uppercase text-xs tracking-wider flex items-center justify-center gap-2"
                >
                  <PlayCircle className="w-4 h-4" />
                  Launch Secure Exam
                </button>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* PROCTOR INSTRUCTIONS POPUP OVERLAY */}
      {selectedTest && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[9999] flex items-center justify-center p-4">
          <div className="bg-white border-2 border-gray-300 rounded-[4px] max-w-lg w-full shadow-2xl overflow-hidden font-sans">
            {/* HEAD */}
            <div className="bg-slate-900 text-white px-5 py-3.5 flex items-center gap-2 font-bold text-sm uppercase tracking-wide">
              <ShieldCheck className="w-5 h-5 text-green-400" />
              <span>Proctored Session Launch Authorization</span>
            </div>

            {/* CONTENT */}
            <div className="p-6">
              <h3 className="text-gray-800 font-extrabold text-sm uppercase">Secure Examination Parameters</h3>
              <p className="text-xs text-gray-400 font-semibold mt-1">
                {selectedTest.title} ({selectedTest.totalQuestions} Questions • {selectedTest.durationMinutes} Minutes)
              </p>

              <div className="mt-4 bg-yellow-50 border border-yellow-200 rounded-md p-4">
                <span className="text-xs font-bold text-yellow-800 uppercase block mb-1">Critical Candidate Instructions:</span>
                <ul className="list-decimal list-inside text-xs text-yellow-700 space-y-1.5 leading-relaxed">
                  <li>This test will launch in **Secure Fullscreen Mode**.</li>
                  <li>Do **NOT** exit fullscreen, press Alt-Tab, minimize the window, or shift focus. Doing so will increment **security infraction flags** logged to the proctor audit trail.</li>
                  <li>Copy-paste actions, screenshots, and right-click behaviors are strictly blocked.</li>
                  <li>A virtual keypad is provided for NAT entries. Keyboard injections are monitored.</li>
                  <li>Ensure a stable network connection. Progress is cached continuously to the server.</li>
                </ul>
              </div>

              <p className="text-xs text-gray-500 mt-4 leading-normal">
                By clicking **Agree and Launch** below, you authorize the GateMockAI exam proctoring client engine to monitor and log window events for the duration of this mock exam.
              </p>
            </div>

            {/* ACTION FOOTER */}
            <div className="bg-gray-50 border-t border-gray-200 px-5 py-4 flex justify-end gap-3 select-none">
              <button
                type="button"
                onClick={() => setSelectedTest(null)}
                className="bg-white hover:bg-gray-100 border border-gray-300 text-gray-700 font-bold py-2 px-4 rounded-[4px] shadow-sm cursor-pointer transition-all duration-150 text-xs uppercase"
              >
                Cancel
              </button>
              
              <button
                type="button"
                onClick={handleConfirmLaunch}
                className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-5 rounded-[4px] shadow-md cursor-pointer transition-all duration-150 text-xs uppercase flex items-center gap-1.5"
              >
                <ShieldCheck className="w-4 h-4" />
                <span>Agree & Launch secure Exam</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
