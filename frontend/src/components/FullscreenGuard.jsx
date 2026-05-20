import React, { useEffect } from 'react'
import { useExamStore } from '../store/examStore'
import { ShieldAlert, Maximize2 } from 'lucide-react'

export default function FullscreenGuard() {
  const { 
    fullscreenViolationsCount, 
    isFullscreenOverlayActive, 
    incrementViolations, 
    closeFullscreenOverlay, 
    isLoaded,
    test
  } = useExamStore()

  // Track fullscreen changes and focus loss
  useEffect(() => {
    if (!isLoaded || !test) return;

    const handleFullscreenChange = () => {
      const isCurrentlyFullscreen = !!(
        document.fullscreenElement ||
        document.webkitFullscreenElement ||
        document.mozFullScreenElement ||
        document.msFullscreenElement
      );

      if (!isCurrentlyFullscreen) {
        // Exited fullscreen!
        incrementViolations();
      }
    };

    const handleVisibilityChange = () => {
      if (document.hidden) {
        // User switched tabs or minimized browser!
        incrementViolations();
      }
    };

    const handleWindowBlur = () => {
      // User clicked outside the browser window!
      incrementViolations();
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
    document.addEventListener('mozfullscreenchange', handleFullscreenChange);
    document.addEventListener('MSFullscreenChange', handleFullscreenChange);
    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('blur', handleWindowBlur);

    // Initial fullscreen request wrapper
    const initialRequest = setTimeout(() => {
      if (!document.fullscreenElement) {
        incrementViolations();
      }
    }, 1000);

    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
      document.removeEventListener('mozfullscreenchange', handleFullscreenChange);
      document.removeEventListener('MSFullscreenChange', handleFullscreenChange);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('blur', handleWindowBlur);
      clearTimeout(initialRequest);
    };
  }, [isLoaded, test, incrementViolations]);

  // Handle re-entering fullscreen
  const handleReEnterFullscreen = async () => {
    try {
      const elem = document.documentElement;
      if (elem.requestFullscreen) {
        await elem.requestFullscreen();
      } else if (elem.webkitRequestFullscreen) {
        await elem.webkitRequestFullscreen();
      } else if (elem.msRequestFullscreen) {
        await elem.msRequestFullscreen();
      }
      closeFullscreenOverlay();
    } catch (err) {
      console.error("Failed to re-enter fullscreen mode:", err);
    }
  };

  if (!isFullscreenOverlayActive) return null;

  return (
    <div className="fixed inset-0 bg-gray-900/95 backdrop-blur-md z-[99999] flex items-center justify-center p-4 select-none">
      <div className="bg-white border-4 border-red-600 rounded-lg p-8 max-w-xl text-center shadow-2xl animate-pulse">
        <div className="flex justify-center mb-4">
          <div className="bg-red-100 p-4 rounded-full text-red-600">
            <ShieldAlert className="w-16 h-16" />
          </div>
        </div>

        <h2 className="text-2xl font-black text-red-700 uppercase tracking-tight">
          Proctor Alert: security breach detected
        </h2>
        
        <p className="mt-3 text-gray-700 font-medium">
          A secure fullscreen exit or an external focus-shift event was captured by the proctored exam engine.
        </p>

        <div className="my-5 bg-red-50 border border-red-200 rounded-md p-4 text-left">
          <p className="text-sm font-semibold text-red-800">
            Audit Flagged Information:
          </p>
          <ul className="list-disc list-inside text-xs text-red-700 mt-1 space-y-1 font-mono">
            <li>Incidents Counted: {fullscreenViolationsCount}</li>
            <li>Event Target: Secured Console Boundary</li>
            <li>Action Taken: Logged to Proctor Database</li>
          </ul>
        </div>

        <p className="text-xs text-gray-500 leading-normal mb-6">
          To resume taking your exam, please click the button below to re-enter secure fullscreen proctored mode. 
          Standard operating procedures restrict active tabs to the GateMockAI web client interface.
        </p>

        <button
          type="button"
          onClick={handleReEnterFullscreen}
          className="w-full bg-red-600 hover:bg-red-700 text-white font-bold py-3.5 px-6 rounded-md shadow-lg flex items-center justify-center gap-2 cursor-pointer transition-all duration-200 text-base border border-red-700 uppercase tracking-wide"
        >
          <Maximize2 className="w-5 h-5" />
          Re-Enter Secure Fullscreen & Resume
        </button>
      </div>
    </div>
  )
}
