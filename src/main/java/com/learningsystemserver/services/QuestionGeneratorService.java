package com.learningsystemserver.services;

import com.learningsystemserver.Utils.QuestionAlgorithmsFunctions;
import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.GeneratedQuestion;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.exceptions.InvalidInputException;
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
        String topicType = determineTopicType(nameLower);
        switch (topicType) {
            case "addition":
                return createAdditionQuestion(topic, difficultyLevel);
            case "subtraction":
                return createSubtractionQuestion(topic, difficultyLevel);
            case "multiplication":
                return createMultiplicationQuestion(topic, difficultyLevel);
            case "division":
                return createDivisionQuestion(topic, difficultyLevel);
            case "fractions":
                return createFractionsQuestion(topic, difficultyLevel);
            case "rectangle":
                return createRectangleQuestion(topic, difficultyLevel);
            case "circle":
                return createCircleQuestion(topic, difficultyLevel);
            case "triangle":
                return createTriangleQuestion(topic, difficultyLevel);
            case "polygon":
                return createPolygonQuestion(topic, difficultyLevel);
            default:
                return createAdditionQuestion(topic, difficultyLevel);
        }
    }

    private String determineTopicType(String nameLower) {
        if (nameLower.contains("addition")) return "addition";
        if (nameLower.contains("subtraction")) return "subtraction";
        if (nameLower.contains("multiplication")) return "multiplication";
        if (nameLower.contains("division")) return "division";
        if (nameLower.contains("fractions")) return "fractions";
        if (nameLower.contains("rectangle")) return "rectangle";
        if (nameLower.contains("circle")) return "circle";
        if (nameLower.contains("triangle")) return "triangle";
        if (nameLower.contains("polygon")) return "polygon";
        return "addition";
    }

    private GeneratedQuestion createAdditionQuestion(Topic topic, DifficultyLevel difficulty) {
        int[] range = getRangeForDifficulty(difficulty);
        int a = getRandomNumber(range);
        int b = getRandomNumber(range);
        int answer = a + b;
        String questionText = a + " + " + b + " = ?";
        String solutionSteps = QuestionAlgorithmsFunctions.simplifyAddition(a, b, answer);
        return saveQuestion(questionText, solutionSteps, String.valueOf(answer), topic, difficulty);
    }

    private GeneratedQuestion createSubtractionQuestion(Topic topic, DifficultyLevel difficulty) {
        int[] range = getRangeForDifficulty(difficulty);
        int a = getRandomNumber(range);
        int b = getRandomNumber(range);
        if (a < b) {
            int temp = a;
            a = b;
            b = temp;
        }
        int answer = a - b;
        String questionText = a + " - " + b + " = ?";
        String solutionSteps = "Subtract " + b + " from " + a + " to get " + answer + ".";
        return saveQuestion(questionText, solutionSteps, String.valueOf(answer), topic, difficulty);
    }

    private GeneratedQuestion createMultiplicationQuestion(Topic topic, DifficultyLevel difficulty) {
        int[] range = getRangeForDifficulty(difficulty);
        int a = getRandomNumber(range);
        int b = getRandomNumber(range);
        int answer = a * b;
        String questionText = a + " × " + b + " = ?";
        String solutionSteps = QuestionAlgorithmsFunctions.simplifyMultiplication(a, b, answer);
        return saveQuestion(questionText, solutionSteps, String.valueOf(answer), topic, difficulty);
    }

    private GeneratedQuestion createDivisionQuestion(Topic topic, DifficultyLevel difficulty) {
        int[] range = getRangeForDifficulty(difficulty);
        int b;
        int a;
        do {
            b = getRandomNumber(range);
        } while (b == 0);
        int multiplier = getRandomNumber(range);
        a = b * multiplier;
        int answer = a / b;
        String questionText = a + " ÷ " + b + " = ?";
        String solutionSteps = QuestionAlgorithmsFunctions.simplifyDivision(a, b, answer);
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
        String solutionSteps = QuestionAlgorithmsFunctions.simplifyFractions(num1, den1, num2, den2, sumNum, commonDen);
        String correctAnswer = sumNum + "/" + commonDen;
        return saveQuestion(questionText, solutionSteps, correctAnswer, topic, difficulty);
    }

    private GeneratedQuestion createRectangleQuestion(Topic topic, DifficultyLevel difficulty) {
        int length = getRandomNumber(new int[]{1, 20});
        int width = getRandomNumber(new int[]{1, 20});
        int area = length * width;
        int perimeter = 2 * (length + width);
        String questionText = "Rectangle with length " + length + " and width " + width + ". Find its area and perimeter.";
        String solutionSteps = QuestionAlgorithmsFunctions.simplifyRectangle(length, width, area, perimeter);
        String correctAnswer = "Area: " + area + ", Perimeter: " + perimeter;
        return saveQuestion(questionText, solutionSteps, correctAnswer, topic, difficulty);
    }

    private GeneratedQuestion createCircleQuestion(Topic topic, DifficultyLevel difficulty) {
        int radius = getRandomNumber(new int[]{1, 10});
        double pi = 3.14;
        double area = pi * radius * radius;
        double circumference = 2 * pi * radius;
        String questionText = "Circle with radius " + radius + ". Find its area and circumference.";
        String solutionSteps = QuestionAlgorithmsFunctions.simplifyCircle(radius, area, circumference);
        String correctAnswer = "Area: " + String.format("%.2f", area) + ", Circumference: " + String.format("%.2f", circumference);
        return saveQuestion(questionText, solutionSteps, correctAnswer, topic, difficulty);
    }

    private GeneratedQuestion createTriangleQuestion(Topic topic, DifficultyLevel difficulty) {
        int base = getRandomNumber(new int[]{1, 20});
        int height = getRandomNumber(new int[]{1, 20});
        double area = 0.5 * base * height;
        double hypotenuse = Math.sqrt(base * base + height * height);
        String questionText = "Right triangle with base " + base + " and height " + height + ". Find its area and hypotenuse.";
        String solutionSteps = QuestionAlgorithmsFunctions.simplifyTriangle(base, height, area, hypotenuse);
        String correctAnswer = "Area: " + String.format("%.2f", area) + ", Hypotenuse: " + String.format("%.2f", hypotenuse);
        return saveQuestion(questionText, solutionSteps, correctAnswer, topic, difficulty);
    }

    private GeneratedQuestion createPolygonQuestion(Topic topic, DifficultyLevel difficulty) {
        int side = getRandomNumber(new int[]{1, 10});
        double apothem = side / (2 * Math.tan(Math.PI / 5));
        double area = (5 * side * apothem) / 2;
        String questionText = "Regular pentagon with side length " + side + ". Find its approximate area.";
        String solutionSteps = QuestionAlgorithmsFunctions.simplifyPolygon(side, apothem, area);
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

    private int getRandomNumber(int[] range) {
        return random.nextInt(range[1] - range[0] + 1) + range[0];
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
                        String.format("Question with ID %d does not exist.", questionId)
                ));
    }
}

