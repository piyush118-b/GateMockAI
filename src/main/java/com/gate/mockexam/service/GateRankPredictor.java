package com.gate.mockexam.service;

import org.springframework.stereotype.Component;

@Component
public class GateRankPredictor {

    // Based on GATE CS 2019-2024 official results
    // Format: { normalizedScoreMin, normalizedScoreMax, rankMin, rankMax }
    private static final int[][] CUTOFF_TABLE = {
        { 85, 100,   1,    50 },
        { 75,  85,  51,   200 },
        { 65,  75, 201,   600 },
        { 55,  65, 601,  1500 },
        { 45,  55,1501,  3500 },
        { 35,  45,3501,  7000 },
        { 25,  35,7001, 15000 },
        {  0,  25,15001,99999 }
    };

    /**
     * @param rawScore    student's raw score from the mock test
     * @param totalMarks  total marks of the mock test (usually 100)
     * @return            human-readable rank prediction string
     */
    public RankPrediction predict(double rawScore, double totalMarks) {
        double normalizedScore = totalMarks > 0 ? (rawScore / totalMarks) * 100.0 : 0.0;

        for (int[] row : CUTOFF_TABLE) {
            if (normalizedScore >= row[0] && normalizedScore <= row[1]) {
                return new RankPrediction(
                    normalizedScore,
                    row[2],
                    row[3],
                    categorize(normalizedScore)
                );
            }
        }
        return new RankPrediction(normalizedScore, 99999, 99999, "Below qualifying cutoff");
    }

    private String categorize(double score) {
        if (score >= 75) return "IIT / IISc (General)";
        if (score >= 60) return "NIT+ / IIIT Hyderabad";
        if (score >= 45) return "NIT / Good PSU";
        if (score >= 35) return "Qualifying";
        return "Below cutoff";
    }

    public record RankPrediction(
        double normalizedScore,
        int rankMin,
        int rankMax,
        String category
    ) {}
}
