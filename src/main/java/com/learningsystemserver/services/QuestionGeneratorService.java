package com.learningsystemserver.services;

import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.GeneratedQuestion;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.GeneratedQuestionRepository;
import com.learningsystemserver.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

import static com.learningsystemserver.exceptions.ErrorMessages.QUESTION_DOES_NOT_EXIST;

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
        } else if (nameLower.contains("rectangle")) {
            return createRectangleQuestion(topic, difficultyLevel);
        } else if (nameLower.contains("circle")) {
            return createCircleQuestion(topic, difficultyLevel);
        } else if (nameLower.contains("triangle")) {
            return createTriangleQuestion(topic, difficultyLevel);
        } else if (nameLower.contains("polygon")) {
            return createPolygonQuestion(topic, difficultyLevel);
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
        int num1 = random.nextInt(9) + 1;
        int den1 = random.nextInt(9) + 1;
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

    private GeneratedQuestion createRectangleQuestion(Topic topic, DifficultyLevel difficulty) {
        int length = random.nextInt(20) + 1;
        int width = random.nextInt(20) + 1;
        int area = length * width;
        int perimeter = 2 * (length + width);

        String questionText = "Rectangle with length " + length + " and width " + width +
                ". Find its area and perimeter.";
        String solutionSteps = "1) Area = length × width = " + length + " × " + width + " = " + area +
                "\n2) Perimeter = 2 × (length + width) = 2 × (" + length + " + " + width + ") = " + perimeter;
        String correctAnswer = "Area: " + area + ", Perimeter: " + perimeter;

        return saveQuestion(questionText, solutionSteps, correctAnswer, topic, difficulty);
    }

    private GeneratedQuestion createCircleQuestion(Topic topic, DifficultyLevel difficulty) {
        int radius = random.nextInt(10) + 1;
        double pi = 3.14;
        double area = pi * radius * radius;
        double circumference = 2 * pi * radius;

        String questionText = "Circle with radius " + radius + ". Find its area and circumference.";
        String solutionSteps = "1) Area = π × r² = 3.14 × " + radius + "² = " + String.format("%.2f", area) +
                "\n2) Circumference = 2 × π × r = 2 × 3.14 × " + radius + " = " + String.format("%.2f", circumference);
        String correctAnswer = "Area: " + String.format("%.2f", area) + ", Circumference: " + String.format("%.2f", circumference);

        return saveQuestion(questionText, solutionSteps, correctAnswer, topic, difficulty);
    }

    private GeneratedQuestion createTriangleQuestion(Topic topic, DifficultyLevel difficulty) {
        int base = random.nextInt(20) + 1;
        int height = random.nextInt(20) + 1;
        double area = 0.5 * base * height;
        double hypotenuse = Math.sqrt(base * base + height * height);

        String questionText = "Right triangle with base " + base + " and height " + height +
                ". Find its area and hypotenuse.";
        String solutionSteps = "1) Area = ½ × base × height = ½ × " + base + " × " + height + " = " + String.format("%.2f", area) +
                "\n2) Hypotenuse = √(base² + height²) = √(" + base + "² + " + height + "²) = " + String.format("%.2f", hypotenuse);
        String correctAnswer = "Area: " + String.format("%.2f", area) + ", Hypotenuse: " + String.format("%.2f", hypotenuse);

        return saveQuestion(questionText, solutionSteps, correctAnswer, topic, difficulty);
    }

    private GeneratedQuestion createPolygonQuestion(Topic topic, DifficultyLevel difficulty) {
        int side = random.nextInt(10) + 1;
        double apothem = side / (2 * Math.tan(Math.PI / 5));
        double area = (5 * side * apothem) / 2;

        String questionText = "Regular pentagon with side length " + side +
                ". Find its approximate area.";
        String solutionSteps = "1) Apothem = side / (2 × tan(π/5)) = " + String.format("%.2f", apothem) +
                "\n2) Area = (5 × side × apothem) / 2 = (5 × " + side + " × " + String.format("%.2f", apothem) + ") / 2 = " + String.format("%.2f", area);
        String correctAnswer = "Approximate Area: " + String.format("%.2f", area);

        return saveQuestion(questionText, solutionSteps, correctAnswer, topic, difficulty);
    }

    private int[] getRangeForDifficulty(DifficultyLevel difficulty) {
        return switch (difficulty) {
            case EASY -> new int[]{1, 30};
            case MEDIUM -> new int[]{1, 100};
            case ADVANCED -> new int[]{1, 1000};
            default -> new int[]{1, 10};
        };
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

    public GeneratedQuestion getQuestionById(Long questionId) throws InvalidInputException {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new InvalidInputException(
                        String.format(QUESTION_DOES_NOT_EXIST.getMessage(), questionId)
                ));
    }

}

