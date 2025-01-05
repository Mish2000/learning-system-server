package com.learningsystemserver.services;

import com.learningsystemserver.dtos.TopicRequest;
import com.learningsystemserver.dtos.TopicResponse;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicResponse createTopic(TopicRequest request) {
        Topic topic = new Topic();
        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic.setDifficultyLevel(request.getDifficultyLevel());

        if (request.getParentId() != null) {
            Optional<Topic> parentOpt = topicRepository.findById(request.getParentId());
            parentOpt.ifPresent(topic::setParentTopic);
        }

        Topic saved = topicRepository.save(topic);
        return mapToResponse(saved);
    }

    public TopicResponse getTopic(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found with id " + id));
        return mapToResponse(topic);
    }

    public List<TopicResponse> getAllTopics() {
        List<Topic> topics = topicRepository.findAll();
        return topics.stream().map(this::mapToResponse).toList();
    }

    public TopicResponse updateTopic(Long id, TopicRequest request) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found with id " + id));

        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic.setDifficultyLevel(request.getDifficultyLevel());

        if (request.getParentId() != null) {
            Optional<Topic> parentOpt = topicRepository.findById(request.getParentId());
            parentOpt.ifPresent(topic::setParentTopic);
        } else {
            topic.setParentTopic(null);
        }

        Topic updated = topicRepository.save(topic);
        return mapToResponse(updated);
    }

    public void deleteTopic(Long id) {
        topicRepository.deleteById(id);
    }

    private TopicResponse mapToResponse(Topic topic) {
        TopicResponse resp = new TopicResponse();
        resp.setId(topic.getId());
        resp.setName(topic.getName());
        resp.setDescription(topic.getDescription());
        resp.setDifficultyLevel(topic.getDifficultyLevel().name());
        resp.setParentId(topic.getParentTopic() != null ? topic.getParentTopic().getId() : null);
        return resp;
    }
}
