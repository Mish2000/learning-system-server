package com.learningsystemserver.services;

import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.entities.UserSubtopicProgress;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.TopicRepository;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.repositories.UserSubtopicProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final UserSubtopicProgressRepository progressRepository;

    @Transactional
    public int seedForNewUser(Long userId) throws InvalidInputException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidInputException("User not found for id=" + userId));

        List<Topic> allTopics = topicRepository.findAll();
        List<Topic> leafSubtopics = allTopics.stream()
                .filter(t -> t.getParentTopic() != null)
                .filter(t -> t.getSubtopics() == null || t.getSubtopics().isEmpty())
                .toList();

        int created = 0;
        for (Topic subtopic : leafSubtopics) {
            boolean exists = progressRepository.findByUserIdAndSubtopicId(user.getId(), subtopic.getId()).isPresent();
            if (!exists) {
                UserSubtopicProgress row = UserSubtopicProgress.builder()
                        .user(user)
                        .subtopic(subtopic)
                        .currentDifficulty(DifficultyLevel.BASIC)
                        .correctStreak(0)
                        .wrongStreak(0)
                        .attemptsSinceLastChange(0)
                        .lastUpdatedAt(LocalDateTime.now())
                        .build();
                progressRepository.save(row);
                created++;
            }
        }
        return created;
    }

    @Transactional
    public int backfillMissingForUser(Long userId) throws InvalidInputException {
        return seedForNewUser(userId);
    }
}
