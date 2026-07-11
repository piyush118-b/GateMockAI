package com.gate.mockexam.service;

import com.gate.mockexam.entity.*;
import com.gate.mockexam.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SpacedRepetitionService {

    @Autowired 
    private AttemptAnswerRepository attemptAnswerRepository;
    
    @Autowired 
    private MockTestRepository mockTestRepository;
    
    @Autowired 
    private QuestionRepository questionRepository;
    
    @Autowired
    private OptionRepository optionRepository;

    /**
     * Call this after grading each AttemptAnswer.
     * quality: 0 = complete blackout, 3 = correct with difficulty, 5 = perfect
     */
    @Transactional
    public void updateSchedule(AttemptAnswer aa, int quality) {
        if (quality >= 3) {
            if (aa.getRepetitions() == 0) {
                aa.setIntervalDays(1);
            } else if (aa.getRepetitions() == 1) {
                aa.setIntervalDays(6);
            } else {
                aa.setIntervalDays((int) Math.round(aa.getIntervalDays() * aa.getEaseFactor()));
            }
            aa.setRepetitions(aa.getRepetitions() + 1);
        } else {
            aa.setRepetitions(0);
            aa.setIntervalDays(1);
        }

        double newEF = aa.getEaseFactor() + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        aa.setEaseFactor(Math.max(1.3, newEF));
        aa.setNextReview(LocalDate.now().plusDays(aa.getIntervalDays()));

        attemptAnswerRepository.save(aa);
    }

    /**
     * Maps grading result to SM-2 quality score automatically.
     */
    public int toQuality(AttemptAnswer aa) {
        if (aa.getMarksAwarded() == null || aa.getMarksAwarded().compareTo(BigDecimal.ZERO) <= 0) {
            return 1; // wrong
        }
        if (aa.getMarksAwarded().compareTo(aa.getQuestion().getMarks()) < 0) {
            return 3; // partial
        }
        return 5; // full marks
    }

    /**
     * Generate a revision MockTest from questions due today for this user.
     * Zero Gemini calls — reuses existing Question entities.
     */
    @Transactional
    public MockTest generateRevisionTest(UUID userId) {
        List<AttemptAnswer> due = attemptAnswerRepository.findDueForReview(userId, LocalDate.now());

        if (due.isEmpty()) {
            return null; // nothing due today
        }

        List<Question> dueQuestions = due.stream()
            .map(AttemptAnswer::getQuestion)
            .distinct()
            .limit(30) // cap at 30 per revision session
            .collect(Collectors.toList());

        MockTest revision = new MockTest();
        revision.setTitle("Revision Session — " + LocalDate.now());
        revision.setTopic("Spaced Repetition Review");
        revision.setSubject("Computer Science");
        revision.setBranch("CSE");
        revision.setYearLabel("2026");
        revision.setDurationMinutes(60);
        revision.setPublished(false);

        BigDecimal totalMarks = dueQuestions.stream()
            .map(Question::getMarks)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        revision.setTotalMarks(totalMarks);

        final MockTest savedRevision = mockTestRepository.save(revision);

        for (Question original : dueQuestions) {
            Question clone = Question.builder()
                .test(savedRevision)
                .questionText(original.getQuestionText())
                .type(original.getType())
                .marks(original.getMarks())
                .negativeMarks(original.getNegativeMarks())
                .correctNatValue(original.getCorrectNatValue())
                .natTolerance(original.getNatTolerance())
                .sequenceNo(savedRevision.getQuestions().size() + 1)
                .explanation(original.getExplanation())
                .imageUrl(original.getImageUrl())
                .imageAltText(original.getImageAltText())
                .build();
            
            clone = questionRepository.save(clone);
            
            if (original.getOptions() != null) {
                for (Option originalOpt : original.getOptions()) {
                    Option optClone = Option.builder()
                        .question(clone)
                        .optionLabel(originalOpt.getOptionLabel())
                        .optionText(originalOpt.getOptionText())
                        .isCorrect(originalOpt.isCorrect())
                        .build();
                    optionRepository.save(optClone);
                    clone.getOptions().add(optClone);
                }
            }
            savedRevision.getQuestions().add(clone);
        }

        return mockTestRepository.save(savedRevision);
    }
}
