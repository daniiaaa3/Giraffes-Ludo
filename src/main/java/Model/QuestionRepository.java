
package Model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Loads and manages the educational question bank.
 */
public final class QuestionRepository {

    private final List<Question> questions;

    public QuestionRepository() {
        this.questions = loadQuestions();
    }

    private List<Question> loadQuestions() {

        List<Question> loadedQuestions = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        InputStream input =
                QuestionRepository.class.getResourceAsStream(
                        "/questions.csv");

        if (input == null) {
            throw new IllegalStateException(
                    "questions.csv was not found.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        input, StandardCharsets.UTF_8))) {

            reader.readLine(); // Skip CSV header

            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {

                rowNumber++;

                if (line.isBlank()) {
                    continue;
                }

                List<String> columns = parseCsvLine(line);

                if (columns.size() != 10) {
                    throw new IllegalStateException(
                            "Invalid CSV row: " + rowNumber);
                }

                Question question = new Question(
                        columns.get(0),
                        columns.get(3),
                        QuestionDifficulty.valueOf(
                                columns.get(2).trim()),
                        List.of(
                                columns.get(4),
                                columns.get(5),
                                columns.get(6),
                                columns.get(7)),
                        Integer.parseInt(
                                columns.get(8).trim()),
                        columns.get(9)
                );

                if (!ids.add(question.getId())) {
                    throw new IllegalStateException(
                            "Duplicate question ID: "
                                    + question.getId());
                }

                loadedQuestions.add(question);
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load questions.", e);
        }

        if (loadedQuestions.isEmpty()) {
            throw new IllegalStateException(
                    "The question bank is empty.");
        }

        return List.copyOf(loadedQuestions);
    }

    public List<Question> getAllQuestions() {
        return questions;
    }

    public List<Question> getQuestionsByDifficulty(
            QuestionDifficulty difficulty) {

        Objects.requireNonNull(difficulty);

        return questions.stream()
                .filter(question ->
                        question.getDifficulty() == difficulty)
                .toList();
    }

    public int size() {
        return questions.size();
    }

    /**
     * Parses a CSV row, supporting quoted fields,
     * commas inside fields and escaped quotation marks.
     */
    private List<String> parseCsvLine(String line) {

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char character = line.charAt(i);

            if (character == '"') {

                if (insideQuotes
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {

                    current.append('"');
                    i++;

                } else {
                    insideQuotes = !insideQuotes;
                }

            } else if (character == ',' && !insideQuotes) {

                values.add(current.toString());
                current.setLength(0);

            } else {
                current.append(character);
            }
        }

        if (insideQuotes) {
            throw new IllegalArgumentException(
                    "Unclosed quotation mark in CSV.");
        }

        values.add(current.toString());

        return values;
    }
}
