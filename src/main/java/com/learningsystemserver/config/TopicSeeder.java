package com.learningsystemserver.config;

import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TopicSeeder implements CommandLineRunner {
    private final TopicRepository topicRepository;

    @Override
    public void run(String... args) throws Exception {
        if (topicRepository.count() == 0) {
            Topic arithmetic = Topic.builder()
                    .name("Arithmetic")
                    .description("Basic arithmetic operations")
                    .difficultyLevel(DifficultyLevel.BASIC)
                    .build();
            topicRepository.save(arithmetic);

            Topic addition = Topic.builder()
                    .name("Addition")
                    .description("Adding numbers")
                    .difficultyLevel(DifficultyLevel.EASY)
                    .parentTopic(arithmetic)
                    .build();
            topicRepository.save(addition);

            Topic geometry = Topic.builder()
                    .name("Geometry")
                    .description("Shapes, perimeter, area")
                    .difficultyLevel(DifficultyLevel.MEDIUM)
                    .build();
            topicRepository.save(geometry);

        }
    }
}
