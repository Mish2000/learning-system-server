package com.learningsystemserver.services;

import com.learningsystemserver.dtos.requests.TopicRequest;
import com.learningsystemserver.dtos.responses.TopicResponse;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.learningsystemserver.exceptions.ErrorMessages.TOPIC_DOES_NOT_EXIST;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicResponse createTopic(TopicRequest request) {
        Topic topic = new Topic();
        topic.setName(request.getName());
        topic.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            topicRepository.findById(request.getParentId()).ifPresent(topic::setParentTopic);
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

    public TopicResponse updateTopic(Long id, TopicRequest request) throws InvalidInputException {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException(
                        String.format(TOPIC_DOES_NOT_EXIST.getMessage(), id)
                ));

        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        if (request.getParentId() != null) {
            topicRepository.findById(request.getParentId()).ifPresent(topic::setParentTopic);
        } else {
            topic.setParentTopic(null);
        }

        Topic updated = topicRepository.save(topic);
        return mapToResponse(updated);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteTopic(Long id) {
        topicRepository.findById(id).ifPresent(topic -> {
            topic.setDeleted(true);
            topicRepository.save(topic);
        });
    }

    public List<TopicResponse> getDeletedTopics() {
        return topicRepository.findByDeletedTrueOrderByNameAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @org.springframework.transaction.annotation.Transactional
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


}
