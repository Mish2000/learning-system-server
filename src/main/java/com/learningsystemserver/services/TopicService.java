package com.learningsystemserver.services;

import com.learningsystemserver.dtos.requests.TopicRequest;
import com.learningsystemserver.dtos.responses.TopicResponse;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.learningsystemserver.exceptions.ErrorMessages.TOPIC_DOES_NOT_EXIST;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    @Transactional
    public TopicResponse createTopic(TopicRequest request) throws InvalidInputException {
        validateName(request.getName());

        Topic topic = new Topic();
        topic.setName(request.getName().trim());
        topic.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            topic.setParentTopic(requireActiveTopic(request.getParentId()));
        } else {
            topic.setParentTopic(null);
        }

        Topic saved = topicRepository.save(topic);
        return mapToResponse(saved);
    }

    public TopicResponse getTopic(Long id) throws InvalidInputException {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException(
                        String.format(com.learningsystemserver.exceptions.ErrorMessages.TOPIC_DOES_NOT_EXIST.getMessage(), id)
                ));

        if (topic.isDeleted()) {
            throw new InvalidInputException(
                    String.format(com.learningsystemserver.exceptions.ErrorMessages.TOPIC_DOES_NOT_EXIST.getMessage(), id)
            );
        }

        return mapToResponse(topic);
    }


    public List<TopicResponse> getTopLevelTopics() {
        return topicRepository.findByParentTopicIsNullAndDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TopicResponse> getSubTopics(Long parentId) {
        return topicRepository.findByParentTopicIdAndDeletedFalse(parentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public TopicResponse updateTopic(Long id, TopicRequest request) throws InvalidInputException {
        validateName(request.getName());

        Topic topic = requireActiveTopic(id);

        topic.setName(request.getName().trim());
        topic.setDescription(request.getDescription());
        if (request.getParentId() != null) {
            Topic parent = requireActiveTopic(request.getParentId());
            validateParentAssignment(id, parent);
            topic.setParentTopic(parent);
        } else {
            topic.setParentTopic(null);
        }

        Topic updated = topicRepository.save(topic);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteTopic(Long id) throws InvalidInputException {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> topicDoesNotExist(id));

        if (topic.isDeleted()) {
            return;
        }

        softDeleteTopicTree(topic, new HashSet<>());
        topicRepository.save(topic);
    }

    public List<TopicResponse> getDeletedTopics() {
        return topicRepository.findByDeletedTrueOrderByNameAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public TopicResponse restoreTopic(Long id) throws InvalidInputException {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException(
                        String.format(com.learningsystemserver.exceptions.ErrorMessages.TOPIC_DOES_NOT_EXIST.getMessage(), id)
                ));

        if (!topic.isDeleted()) {
            return mapToResponse(topic);
        }

        if (topic.getParentTopic() != null && topic.getParentTopic().isDeleted()) {
            throw new InvalidInputException(
                    "Cannot restore a subtopic before restoring its parent topic."
            );
        }

        topic.setDeleted(false);
        Topic saved = topicRepository.save(topic);
        return mapToResponse(saved);
    }


    private TopicResponse mapToResponse(Topic topic) {
        TopicResponse resp = new TopicResponse();
        resp.setId(topic.getId());
        resp.setName(topic.getName());
        resp.setDescription(topic.getDescription());
        resp.setParentId(topic.getParentTopic() != null
                ? topic.getParentTopic().getId()
                : null);

        int subCount = topicRepository
                .findByParentTopicIdAndDeletedFalse(topic.getId())
                .size();
        resp.setSubtopicCount(subCount);

        return resp;
    }

    private void validateName(String name) throws InvalidInputException {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Topic name cannot be blank.");
        }
    }

    private Topic requireActiveTopic(Long id) throws InvalidInputException {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> topicDoesNotExist(id));

        if (topic.isDeleted()) {
            throw topicDoesNotExist(id);
        }

        return topic;
    }

    private InvalidInputException topicDoesNotExist(Long id) {
        return new InvalidInputException(
                String.format(TOPIC_DOES_NOT_EXIST.getMessage(), id)
        );
    }

    private void validateParentAssignment(Long topicId, Topic parent) throws InvalidInputException {
        if (parent.getId() != null && parent.getId().equals(topicId)) {
            throw new InvalidInputException("A topic cannot be its own parent.");
        }

        Set<Long> visited = new HashSet<>();
        Topic current = parent;
        while (current != null) {
            Long currentId = current.getId();
            if (currentId != null && currentId.equals(topicId)) {
                throw new InvalidInputException("Assigning this parent would create a topic hierarchy cycle.");
            }
            if (currentId != null && !visited.add(currentId)) {
                throw new InvalidInputException("Topic hierarchy contains a cycle.");
            }
            current = current.getParentTopic();
        }
    }

    private void softDeleteTopicTree(Topic topic, Set<Long> visited) {
        Long topicId = topic.getId();
        if (topicId != null && !visited.add(topicId)) {
            return;
        }

        topic.setDeleted(true);
        if (topic.getSubtopics() == null) {
            return;
        }

        for (Topic subtopic : topic.getSubtopics()) {
            softDeleteTopicTree(subtopic, visited);
        }
    }

}
