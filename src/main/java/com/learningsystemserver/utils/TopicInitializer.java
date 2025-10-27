package com.learningsystemserver.utils;

import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.repositories.TopicRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TopicInitializer implements CommandLineRunner {
    private final TopicRepository topicRepository;

    public TopicInitializer(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Override
    public void run(String... args) {

        createTopicIfNotExists("Arithmetic", "Basic arithmetic operations",
                DifficultyLevel.BASIC, null);

        createTopicIfNotExists("Addition", "Adding numbers",
                DifficultyLevel.EASY, "Arithmetic");

        createTopicIfNotExists("Subtraction", "Subtracting numbers",
                DifficultyLevel.EASY, "Arithmetic");

        createTopicIfNotExists("Multiplication", "Multiplying numbers",
                DifficultyLevel.EASY, "Arithmetic");

        createTopicIfNotExists("Division", "Dividing numbers",
                DifficultyLevel.EASY, "Arithmetic");

        createTopicIfNotExists("Fractions", "Working with fractions in arithmetic",
                DifficultyLevel.EASY, "Arithmetic");

        createTopicIfNotExists("Geometry", "Basic shapes and geometry fundamentals",
                DifficultyLevel.MEDIUM, null);

        createTopicIfNotExists("Rectangle", "Problems related to rectangles",
                DifficultyLevel.EASY, "Geometry");

        createTopicIfNotExists("Circle", "Problems related to circles",
                DifficultyLevel.EASY, "Geometry");

        createTopicIfNotExists("Triangle", "Problems related to triangles",
                DifficultyLevel.EASY, "Geometry");

        createTopicIfNotExists("Polygon", "Problems related to polygons",
                DifficultyLevel.EASY, "Geometry");

    }

    private void createTopicIfNotExists(String name,
                                        String description,
                                        DifficultyLevel difficulty,
                                        String parentName) {
        if (topicRepository.existsByName(name)) {
            return;
        }

        Topic topic = new Topic();
        topic.setName(name);
        topic.setDescription(description);
        topic.setDifficultyLevel(difficulty);

        if (parentName != null) {
            topicRepository.findByName(parentName).ifPresent(topic::setParentTopic);
        }

        topicRepository.save(topic);
    }
}


