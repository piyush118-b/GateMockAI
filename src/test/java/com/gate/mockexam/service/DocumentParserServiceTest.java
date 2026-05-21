package com.gate.mockexam.service;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

public class DocumentParserServiceTest {

    private final DocumentParserService parserService = new DocumentParserService();

    @Test
    public void testParseAnswerKeyToMap_TableRowFormat() {
        String answerKeyText = """
            Q.No. Session Que.Type Sec. Name Key Marks
            1 6 MCQ GA D 1
            2 6 MCQ GA A 1
            6 6 MCQ GA D 2
            17 6 NAT CS 0.125 to 0.125 1
            21 6 NAT CS 13.3 to 13.3 OR 13.5 to 13.5 1
            7 6 MCQ CS MTA 1
            26 6 MCQ CS A 2
            """;

        Map<String, DocumentParserService.AnswerKeyEntry> map = parserService.parseAnswerKeyToMap(answerKeyText);

        assertThat(map).hasSize(7);

        DocumentParserService.AnswerKeyEntry ga1 = map.get("GA_1");
        assertThat(ga1).isNotNull();
        assertThat(ga1.getQuestionNo()).isEqualTo(1);
        assertThat(ga1.getType()).isEqualTo("MCQ");
        assertThat(ga1.getSection()).isEqualTo("GA");
        assertThat(ga1.getCorrectKey()).isEqualTo("D");
        assertThat(ga1.getMarks()).isEqualTo(1.0);

        DocumentParserService.AnswerKeyEntry cs17 = map.get("CS_17");
        assertThat(cs17).isNotNull();
        assertThat(cs17.getQuestionNo()).isEqualTo(17);
        assertThat(cs17.getType()).isEqualTo("NAT");
        assertThat(cs17.getSection()).isEqualTo("CS");
        assertThat(cs17.getCorrectKey()).isEqualTo("0.125 to 0.125");
        assertThat(cs17.getMarks()).isEqualTo(1.0);

        DocumentParserService.AnswerKeyEntry cs7 = map.get("CS_7");
        assertThat(cs7).isNotNull();
        assertThat(cs7.getQuestionNo()).isEqualTo(7);
        assertThat(cs7.getType()).isEqualTo("MCQ");
        assertThat(cs7.getCorrectKey()).isEqualTo("MTA");
    }

    @Test
    public void testParseAnswerKeyToMap_PrefixedManualFormat() {
        String answerKeyText = """
            GA 1: D
            GA 2: A
            CS 17: 0.125 to 0.125
            CS-26: B
            """;

        Map<String, DocumentParserService.AnswerKeyEntry> map = parserService.parseAnswerKeyToMap(answerKeyText);

        assertThat(map).hasSize(4);

        DocumentParserService.AnswerKeyEntry ga1 = map.get("GA_1");
        assertThat(ga1).isNotNull();
        assertThat(ga1.getType()).isEqualTo("MCQ");
        assertThat(ga1.getCorrectKey()).isEqualTo("D");

        DocumentParserService.AnswerKeyEntry cs17 = map.get("CS_17");
        assertThat(cs17).isNotNull();
        assertThat(cs17.getType()).isEqualTo("NAT");
        assertThat(cs17.getCorrectKey()).isEqualTo("0.125 to 0.125");
    }

    @Test
    public void testParseAnswerKeyToMap_SimpleListFormat() {
        String answerKeyText = """
            GENERAL APTITUDE
            1: D
            2: A
            10: C
            COMPUTER SCIENCE
            1: A
            2: A
            17: 0.125 to 0.125
            """;

        Map<String, DocumentParserService.AnswerKeyEntry> map = parserService.parseAnswerKeyToMap(answerKeyText);

        // GA 1, 2, 10
        // Since simple list pattern maps globalOrSecNo > 10 to CS, 10 is GA, 17 is CS.
        // Wait, for 1 and 2 under COMPUTER SCIENCE, they are globalOrSecNo <= 10.
        // In the parser logic:
        // if globalOrSecNo <= 10, it uses currentSection.
        // For the first part: currentSection is GA, so we get GA_1, GA_2, GA_10.
        // For the second part: currentSection is CS, so we get CS_1, CS_2, CS_17.
        assertThat(map.get("GA_1").getCorrectKey()).isEqualTo("D");
        assertThat(map.get("GA_2").getCorrectKey()).isEqualTo("A");
        assertThat(map.get("GA_10").getCorrectKey()).isEqualTo("C");

        assertThat(map.get("CS_1").getCorrectKey()).isEqualTo("A");
        assertThat(map.get("CS_2").getCorrectKey()).isEqualTo("A");
        assertThat(map.get("CS_17").getCorrectKey()).isEqualTo("0.125 to 0.125");
    }
}
