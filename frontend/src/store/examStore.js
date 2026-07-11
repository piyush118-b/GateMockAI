import { create } from 'zustand'

export const useExamStore = create((set, get) => ({
  test: null,
  attempt: null,
  questions: [],
  activeQuestionIndex: 0,
  activeSection: 0, // 0 = General Aptitude, 1 = Core Subject
  questionStates: [], // 'NOT_VISITED', 'NOT_ANSWERED', 'ANSWERED', 'MARKED', 'MARKED_ANSWERED'
  answersCache: {}, // { questionId: value }
  timeLeft: 10800, // seconds (3 hours default)
  timeSpentMap: {}, // { questionId: seconds }
  activeQuestionStartTime: null,
  fullscreenViolationsCount: 0,
  isFullscreenOverlayActive: false,
  isSubmitModalActive: false,
  isCalculatorVisible: false,
  isLoaded: false,
  isSubmitting: false,
  error: null,

  // Load session from Spring Boot REST endpoints
  loadSession: async (testId) => {
    try {
      set({ error: null });
      
      // 1. Fetch Session Metadata (includes attempt startedAt, remaining duration)
      const sessionRes = await fetch(`/api/exam/${testId}/session`);
      if (!sessionRes.ok) throw new Error('Failed to load exam session metadata.');
      const sessionData = await sessionRes.json();
      
      // 2. Fetch Questions list
      const questionsRes = await fetch(`/api/exam/${testId}/questions`);
      if (!questionsRes.ok) throw new Error('Failed to load exam questions payload.');
      const questionsData = await questionsRes.json();
      
      const totalCount = questionsData.length;
      
      // Construct initial palette states and fill from cached answers
      const initialStates = new Array(totalCount).fill('NOT_VISITED');
      const initialCache = {};
      const initialTimeMap = {};
      
      // Populate already saved answers from database
      if (sessionData.savedAnswers) {
        sessionData.savedAnswers.forEach(ans => {
          initialTimeMap[ans.questionId] = ans.timeSpentSeconds || 0;
          if (ans.natValueEntered !== null) {
            initialCache[ans.questionId] = String(ans.natValueEntered);
            initialStates[ans.sequenceNo - 1] = 'ANSWERED';
          } else if (ans.selectedOptionIds && ans.selectedOptionIds.trim() !== '') {
            initialCache[ans.questionId] = ans.selectedOptionIds;
            initialStates[ans.sequenceNo - 1] = 'ANSWERED';
          }
        });
      }

      // Mark the first question as visited
      if (totalCount > 0 && initialStates[0] === 'NOT_VISITED') {
        initialStates[0] = 'NOT_ANSWERED';
      }

      set({
        test: sessionData.test,
        attempt: sessionData.attempt,
        questions: questionsData,
        timeLeft: sessionData.timeLeftSeconds,
        answersCache: initialCache,
        timeSpentMap: initialTimeMap,
        questionStates: initialStates,
        activeQuestionIndex: 0,
        activeSection: 0,
        activeQuestionStartTime: Date.now(),
        isLoaded: true
      });
    } catch (err) {
      set({ error: err.message, isLoaded: false });
    }
  },

  // Record time spent on the active question
  recordTimeSpent: () => {
    const { activeQuestionStartTime, activeQuestionIndex, questions, timeSpentMap } = get();
    if (!activeQuestionStartTime) return;
    const activeQ = questions[activeQuestionIndex];
    if (!activeQ) return;

    const elapsed = Math.round((Date.now() - activeQuestionStartTime) / 1000);
    const updatedMap = { ...timeSpentMap };
    updatedMap[activeQ.id] = (updatedMap[activeQ.id] || 0) + elapsed;

    set({
      timeSpentMap: updatedMap,
      activeQuestionStartTime: Date.now()
    });
  },

  // Save intermediate responses to server dynamically
  saveActiveResponse: async () => {
    const { questions, activeQuestionIndex, answersCache, test } = get();
    const activeQ = questions[activeQuestionIndex];
    if (!activeQ) return;

    const val = answersCache[activeQ.id] || "";
    
    // Update local state color
    const newStates = [...get().questionStates];
    if (val.trim() !== "") {
      newStates[activeQuestionIndex] = 'ANSWERED';
    } else {
      newStates[activeQuestionIndex] = 'NOT_ANSWERED';
    }
    set({ questionStates: newStates });

    // Sync to backend REST API asynchronously
    try {
      await fetch(`/api/exam/${test.id}/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ questionId: activeQ.id, response: val })
      });
    } catch (err) {
      console.error('Failed to sync intermediate progress to server:', err);
    }
  },

  // Clear selections
  clearActiveResponse: async () => {
    const { questions, activeQuestionIndex, test } = get();
    const activeQ = questions[activeQuestionIndex];
    if (!activeQ) return;

    const newCache = { ...get().answersCache };
    delete newCache[activeQ.id];

    const newStates = [...get().questionStates];
    newStates[activeQuestionIndex] = 'NOT_ANSWERED';

    set({ answersCache: newCache, questionStates: newStates });

    // Sync empty value to server
    try {
      await fetch(`/api/exam/${test.id}/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ questionId: activeQ.id, response: "" })
      });
    } catch (err) {
      console.error('Failed to clear answer on server:', err);
    }
  },

  // Set answer in cache
  setAnswerValue: (questionId, value) => {
    set(state => ({
      answersCache: { ...state.answersCache, [questionId]: value }
    }));
  },

  // Jump directly to a question
  jumpToQuestion: (index) => {
    const { questions, questionStates, recordTimeSpent } = get();
    if (index < 0 || index >= questions.length) return;

    recordTimeSpent();

    const newStates = [...questionStates];
    if (newStates[index] === 'NOT_VISITED') {
      newStates[index] = 'NOT_ANSWERED';
    }

    // Auto-adjust active section based on question index (General Aptitude is Q1-Q10, index 0-9)
    const isFullExam = questions.length > 20;
    const targetSection = (isFullExam && index >= 10) ? 1 : 0;

    set({
      activeQuestionIndex: index,
      activeSection: targetSection,
      questionStates: newStates
    });
  },

  // Save selection and jump to next
  saveAndNext: async () => {
    const { activeQuestionIndex, questions, saveActiveResponse, jumpToQuestion, recordTimeSpent } = get();
    await saveActiveResponse();
    
    if (activeQuestionIndex < questions.length - 1) {
      jumpToQuestion(activeQuestionIndex + 1);
    } else {
      recordTimeSpent();
      set({ isSubmitModalActive: true });
    }
  },

  // Toggle purple marked for review state
  markForReviewAndNext: async () => {
    const { activeQuestionIndex, questions, answersCache, test, jumpToQuestion, recordTimeSpent } = get();
    const activeQ = questions[activeQuestionIndex];
    if (!activeQ) return;

    const val = answersCache[activeQ.id] || "";
    const newStates = [...get().questionStates];
    
    if (val.trim() !== "") {
      newStates[activeQuestionIndex] = 'MARKED_ANSWERED';
    } else {
      newStates[activeQuestionIndex] = 'MARKED';
    }
    
    set({ questionStates: newStates });

    // Sync marked progress to backend
    try {
      await fetch(`/api/exam/${test.id}/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ questionId: activeQ.id, response: val, marked: true })
      });
    } catch (err) {
      console.error('Failed to sync marked answer to server:', err);
    }

    if (activeQuestionIndex < questions.length - 1) {
      jumpToQuestion(activeQuestionIndex + 1);
    } else {
      recordTimeSpent();
    }
  },

  // Decrement timer
  decrementTimer: () => {
    const { timeLeft, submitExam } = get();
    if (timeLeft <= 1) {
      set({ timeLeft: 0 });
      submitExam(true); // Auto submit on timeout
    } else {
      set({ timeLeft: timeLeft - 1 });
    }
  },

  // Submit secure attempt
  submitExam: async (isTimeout = false) => {
    const { test, questions, answersCache, timeSpentMap, isSubmitting, recordTimeSpent } = get();
    if (isSubmitting) return;

    recordTimeSpent();

    set({ isSubmitting: true });

    try {
      // Map all questions to the responses list, including timeSpentSeconds
      const responsesList = questions.map(q => ({
        questionId: q.id,
        response: answersCache[q.id] || "",
        timeSpentSeconds: timeSpentMap[q.id] || 0
      }));

      const res = await fetch(`/api/exam/${test.id}/submit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ responses: responsesList, timeout: isTimeout })
      });

      if (!res.ok) throw new Error('Submission failed.');
      const data = await res.json();
      
      // Clean up local storage and redirect to scorecard
      localStorage.removeItem(`mock_exam_cache_${test.id}`);
      window.location.href = data.redirectUrl;
    } catch (err) {
      set({ error: 'Failed to submit exam paper. Please check connection and try again.', isSubmitting: false });
    }
  },

  // Security violations
  incrementViolations: () => {
    set(state => ({
      fullscreenViolationsCount: state.fullscreenViolationsCount + 1,
      isFullscreenOverlayActive: true
    }));
  },

  closeFullscreenOverlay: () => {
    set({ isFullscreenOverlayActive: false });
  }
}));
