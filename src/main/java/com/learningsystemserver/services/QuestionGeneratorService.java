package com.learningsystemserver.services;

import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.GeneratedQuestion;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.repositories.GeneratedQuestionRepository;
import com.learningsystemserver.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class QuestionGeneratorService {

    private final TopicRepository topicRepository;
    private final GeneratedQuestionRepository questionRepository;
    private final Random random = new Random();

    public GeneratedQuestion generateQuestion(Long topicId, DifficultyLevel difficultyLevel) {
        Topic topic = null;
        if (topicId != null) {
            topic = topicRepository.findById(topicId).orElse(null);
        }

        if (topic == null) {
            return createAdditionQuestion(null, difficultyLevel);
        }

        String nameLower = topic.getName().toLowerCase();

        if (nameLower.contains("addition")) {
            return createAdditionQuestion(topic, difficultyLevel);
        } else if (nameLower.contains("multiplication")) {
            return createMultiplicationQuestion(topic, difficultyLevel);
        } else if (nameLower.contains("division")) {
            return createDivisionQuestion(topic, difficultyLevel);
        } else if (nameLower.contains("fractions")) {
            return createFractionsQuestion(topic, difficultyLevel);
        } else if (nameLower.contains("geometry")) {
            return createGeometryQuestion(topic, difficultyLevel);
        } else {
            return createAdditionQuestion(topic, difficultyLevel);
        }
    }

    private GeneratedQuestion createAdditionQuestion(Topic topic, DifficultyLevel difficulty) {
        int[] range = getRangeForDifficulty(difficulty);
        int a = random.nextInt(range[1] - range[0] + 1) + range[0];
        int b = random.nextInt(range[1] - range[0] + 1) + range[0];
        int answer = a + b;

        String questionText = a + " + " + b + " = ?";
        String solutionSteps = "1) Add " + a + " and " + b
                + "\n2) The result is " + answer + ".";

        return saveQuestion(questionText, solutionSteps, String.valueOf(answer), topic, difficulty);
    }

    private GeneratedQuestion createMultiplicationQuestion(Topic topic, DifficultyLevel difficulty) {
        int[] range = getRangeForDifficulty(difficulty);
        int a = random.nextInt(range[1] - range[0] + 1) + range[0];
        int b = random.nextInt(range[1] - range[0] + 1) + range[0];
        int answer = a * b;

        String questionText = a + " × " + b + " = ?";
        String solutionSteps = "1) Multiply " + a + " by " + b
                + "\n2) The result is " + answer + ".";

        return saveQuestion(questionText, solutionSteps, String.valueOf(answer), topic, difficulty);
    }

    private GeneratedQuestion createDivisionQuestion(Topic topic, DifficultyLevel difficulty) {
        int[] range = getRangeForDifficulty(difficulty);
        int b;
        int a;

        do {
            b = random.nextInt(range[1] - range[0] + 1) + range[0];
        } while (b == 0);

        int multiplier = random.nextInt(range[1] - range[0] + 1) + range[0];
        a = b * multiplier;
        int answer = a / b;

        String questionText = a + " ÷ " + b + " = ?";
        String solutionSteps = "1) Divide " + a + " by " + b
                + "\n2) The result is " + answer + ".";

        return saveQuestion(questionText, solutionSteps, String.valueOf(answer), topic, difficulty);
    }

    private GeneratedQuestion createFractionsQuestion(Topic topic, DifficultyLevel difficulty) {
        int num1 = random.nextInt(9) + 1; // 1..9
        int den1 = random.nextInt(9) + 1; // 1..9
        int num2 = random.nextInt(9) + 1;
        int den2 = random.nextInt(9) + 1;

        int commonDen = den1 * den2;
        int newNum1 = num1 * den2;
        int newNum2 = num2 * den1;
        int sumNum = newNum1 + newNum2;

        String questionText = "(" + num1 + "/" + den1 + ") + (" + num2 + "/" + den2 + ") = ?";
        String solutionSteps = "1) Common denominator: " + den1 + " * " + den2 + " = " + commonDen
                + "\n2) Convert each fraction: " + num1 + "/" + den1
                + " = " + newNum1 + "/" + commonDen + " and " + num2 + "/" + den2
                + " = " + newNum2 + "/" + commonDen
                + "\n3) Add numerators: " + newNum1 + " + " + newNum2 + " = " + sumNum
                + "\n4) Final fraction: " + sumNum + "/" + commonDen
                + " (You may simplify further).";

        String correctAnswer = sumNum + "/" + commonDen;

        return saveQuestion(questionText, solutionSteps, correctAnswer, topic, difficulty);
    }

    private GeneratedQuestion createGeometryQuestion(Topic topic, DifficultyLevel difficulty) {
        int shapeType = random.nextInt(2);

        if (shapeType == 0) {
            int length = random.nextInt(9) + 1;
            int width = random.nextInt(9) + 1;
            boolean doArea = random.nextBoolean();

            if (doArea) {
                int area = length * width;
                String questionText = "A rectangle has length=" + length + " and width=" + width
                        + ". What is its area?";
                String solutionSteps = "1) Area of rectangle = length × width"
                        + "\n2) So: " + length + " × " + width + " = " + area + ".";
                return saveQuestion(questionText, solutionSteps, String.valueOf(area), topic, difficulty);
            } else {
                int perimeter = 2 * (length + width);
                String questionText = "A rectangle has length=" + length + " and width=" + width
                        + ". What is its perimeter?";
                String solutionSteps = "1) Perimeter of rectangle = 2 × (length + width)"
                        + "\n2) So: 2 × (" + length + " + " + width + ") = " + perimeter + ".";
                return saveQuestion(questionText, solutionSteps, String.valueOf(perimeter), topic, difficulty);
            }
        } else {
            int radius = random.nextInt(9) + 1;
            boolean doArea = random.nextBoolean();
            double pi = 3.14;

            if (doArea) {
                double area = pi * radius * radius;
                String questionText = "A circle has radius=" + radius + ". What is its area?";
                String solutionSteps = "1) Area of circle = π × r²"
                        + "\n2) r² = " + radius + " × " + radius + " = " + (radius * radius)
                        + "\n3) So area ≈ 3.14 × " + (radius * radius)
                        + " = " + area + ".";
                return saveQuestion(questionText, solutionSteps, String.format("%.2f", area), topic, difficulty);
            } else {
                double circumference = 2 * pi * radius;
                String questionText = "A circle has radius=" + radius + ". What is its circumference?";
                String solutionSteps = "1) Circumference of circle = 2 × π × r"
                        + "\n2) So circumference ≈ 2 × 3.14 × " + radius
                        + " = " + circumference + ".";
                return saveQuestion(questionText, solutionSteps, String.format("%.2f", circumference), topic, difficulty);
            }
        }
    }

    private int[] getRangeForDifficulty(DifficultyLevel difficulty) {
        switch (difficulty) {
            case EASY:
                return new int[]{1, 30};
            case MEDIUM:
                return new int[]{1, 100};
            case ADVANCED:
                return new int[]{1, 1000};
            default:
                return new int[]{1, 10};
        }
    }

    private GeneratedQuestion saveQuestion(
            String questionText,
            String solutionSteps,
            String correctAnswer,
            Topic topic,
            DifficultyLevel difficulty
    ) {
        GeneratedQuestion generated = GeneratedQuestion.builder()
                .questionText(questionText)
                .solutionSteps(solutionSteps)
                .correctAnswer(correctAnswer)
                .topic(topic)
                .difficultyLevel(difficulty)
                .build();

        return questionRepository.save(generated);
    }

    public GeneratedQuestion getQuestionById(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));
    }
}

