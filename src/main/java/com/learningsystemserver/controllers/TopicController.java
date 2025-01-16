package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.TopicRequest;
import com.learningsystemserver.dtos.TopicResponse;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.services.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @PostMapping
    public ResponseEntity<TopicResponse> createTopic(@RequestBody TopicRequest request) {
        TopicResponse created = topicService.createTopic(request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TopicResponse> getTopic(@PathVariable Long id) throws InvalidInputException {
        TopicResponse topic = topicService.getTopic(id);
        return ResponseEntity.ok(topic);
    }

    @GetMapping
    public ResponseEntity<List<TopicResponse>> getTopics(
            @RequestParam(required = false) Long parentId) {
        if (parentId == null) {
            List<TopicResponse> parents = topicService.getTopLevelTopics();
            return ResponseEntity.ok(parents);
        } else {
            List<TopicResponse> subtopics = topicService.getSubTopics(parentId);
            return ResponseEntity.ok(subtopics);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TopicResponse> updateTopic(@PathVariable Long id, @RequestBody TopicRequest request) throws InvalidInputException {
        TopicResponse updated = topicService.updateTopic(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }

}

