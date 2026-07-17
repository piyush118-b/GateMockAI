package com.gate.mockexam.pipeline.extraction;

import com.gate.mockexam.pipeline.extraction.dto.ExtractedQuestionDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates a single extracted question against the GATE platform business rules.
 * Validation happens BEFORE any data is written to the database.
 *
 * Any question that fails validation is flagged for manual review
 * and excluded from the current pipeline run.
 */
@Component
public class QuestionValidator {

    /**
     * Validates an extracted question and returns a list of violation messages.
     * An empty list means the question is valid.
     */
    public List<String> validate(ExtractedQuestionDto question, String correctAnswer) {
        List<String> violations = new ArrayList<>();

        // Rule 1: Question text must exist and be non-trivial
        if (question.getQuestionText() == null || question.getQuestionText().trim().length() < 5) {
            violations.add("Question text is missing or too short");
        }

        // Rule 2: Question number must be positive
        if (question.getQuestionNumber() == null || question.getQuestionNumber() <= 0) {
            violations.add("Question number is missing or invalid: " + question.getQuestionNumber());
        }

        // Rule 3: Question type must be MCQ, MSQ, or NAT
        if (question.getQuestionType() == null ||
                (!question.getQuestionType().equalsIgnoreCase("MCQ") &&
                 !question.getQuestionType().equalsIgnoreCase("MSQ") &&
                 !question.getQuestionType().equalsIgnoreCase("NAT"))) {
            violations.add("Invalid question type: " + question.getQuestionType());
        }

        String type = question.getQuestionType();
        if (type != null) {
            if (type.equalsIgnoreCase("MCQ")) {
                // Rule 4: MCQ must have exactly 4 options
                if (question.getOptions() == null || question.getOptions().size() != 4) {
                    violations.add("MCQ question " + question.getQuestionNumber() +
                                   " must have exactly 4 options, found: " +
                                   (question.getOptions() == null ? 0 : question.getOptions().size()));
                }
                // Rule 5: MCQ must have a valid answer
                if (correctAnswer != null) {
                    String ans = correctAnswer.trim().toUpperCase();
                    if (!ans.equals("A") && !ans.equals("B") && !ans.equals("C") && !ans.equals("D")) {
                        violations.add("MCQ answer must be A/B/C/D, got: " + correctAnswer);
                    }
                }
            }

            if (type.equalsIgnoreCase("MSQ")) {
                // Rule 6: MSQ must have at least 4 options
                if (question.getOptions() == null || question.getOptions().size() < 4) {
                    violations.add("MSQ question " + question.getQuestionNumber() +
                                   " must have at least 4 options");
                }
                // Rule 7: MSQ answer must be comma-separated letters
                if (correctAnswer != null && !correctAnswer.trim().isEmpty()) {
                    String[] parts = correctAnswer.split(",");
                    for (String part : parts) {
                        String p = part.trim().toUpperCase();
                        if (!p.equals("A") && !p.equals("B") && !p.equals("C") && !p.equals("D")) {
                            violations.add("MSQ answer contains invalid option label: " + p);
                        }
                    }
                }
            }

            if (type.equalsIgnoreCase("NAT")) {
                // Rule 8: NAT should have no options
                if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                    violations.add("NAT question should not have options");
                }
            }
        }

        // Rule 9: Marks must be positive
        if (question.getMarks() != null && question.getMarks() <= 0) {
            violations.add("Marks must be positive, got: " + question.getMarks());
        }

        return violations;
    }
}
