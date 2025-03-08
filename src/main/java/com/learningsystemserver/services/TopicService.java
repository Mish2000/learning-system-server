package com.learningsystemserver.services;

import com.learningsystemserver.dtos.requests.TopicRequest;
import com.learningsystemserver.dtos.responses.TopicResponse;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.learningsystemserver.exceptions.ErrorMessages.TOPIC_DOES_NOT_EXIST;

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

    public TopicResponse getTopic(Long id) throws InvalidInputException {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException(
                        String.format(TOPIC_DOES_NOT_EXIST.getMessage(), id)
                ));
        return mapToResponse(topic);
    }

    public List<TopicResponse> getTopLevelTopics() {
        List<Topic> parents = topicRepository.findByParentTopicIsNull();
        return parents.stream().map(this::mapToResponse).toList();
    }

    public List<TopicResponse> getSubTopics(Long parentId) {
        List<Topic> subs = topicRepository.findByParentTopicId(parentId);
        return subs.stream().map(this::mapToResponse).toList();
    }

    public TopicResponse updateTopic(Long id, TopicRequest request) throws InvalidInputException {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException(
                        String.format(TOPIC_DOES_NOT_EXIST.getMessage(), id)
                ));

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

        if (topic.getSubtopics() != null) {
            resp.setSubtopicCount(topic.getSubtopics().size());
        } else {
            resp.setSubtopicCount(0);
        }

        return resp;
    }

}
