package com.learningsystemserver.config;

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

        createTopicIfNotExists("Multiplication", "Multiplying numbers",
                DifficultyLevel.EASY, "Arithmetic");

        createTopicIfNotExists("Division", "Dividing numbers",
                DifficultyLevel.EASY, "Arithmetic");

        createTopicIfNotExists("Fractions", "Working with fractions in arithmetic",
                DifficultyLevel.EASY, "Arithmetic");

        createTopicIfNotExists("Geometry", "Basic shapes and geometry fundamentals",
                DifficultyLevel.MEDIUM, null);
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


