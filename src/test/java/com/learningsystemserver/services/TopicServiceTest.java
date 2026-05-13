package com.learningsystemserver.services;

import com.learningsystemserver.dtos.requests.TopicRequest;
import com.learningsystemserver.dtos.responses.TopicResponse;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @InjectMocks
    private TopicService topicService;

    @Test
    void createTopicRejectsBlankName() {
        assertThatThrownBy(() -> topicService.createTopic(topicRequest("   ", null)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Topic name cannot be blank.");

        verify(topicRepository, never()).save(any());
    }

    @Test
    void createTopicRejectsMissingParent() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.createTopic(topicRequest("Linear Equations", 99L)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("There is no topic with the given ID: 99");

        verify(topicRepository, never()).save(any());
    }

    @Test
    void createTopicRejectsDeletedParent() {
        Topic parent = topic(1L, "Algebra");
        parent.setDeleted(true);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> topicService.createTopic(topicRequest("Linear Equations", 1L)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("There is no topic with the given ID: 1");

        verify(topicRepository, never()).save(any());
    }

    @Test
    void createTopicAssignsActiveParent() throws InvalidInputException {
        Topic parent = topic(1L, "Algebra");
        when(topicRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(topicRepository.findByParentTopicIdAndDeletedFalse(2L)).thenReturn(List.of());

        TopicResponse response = topicService.createTopic(topicRequest("  Linear Equations  ", 1L));

        ArgumentCaptor<Topic> savedTopic = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(savedTopic.capture());
        assertThat(savedTopic.getValue().getName()).isEqualTo("Linear Equations");
        assertThat(savedTopic.getValue().getParentTopic()).isSameAs(parent);
        assertThat(response.getParentId()).isEqualTo(1L);
    }

    @Test
    void updateTopicRejectsDeletedTarget() {
        Topic topic = topic(1L, "Algebra");
        topic.setDeleted(true);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        assertThatThrownBy(() -> topicService.updateTopic(1L, topicRequest("Algebra", null)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("There is no topic with the given ID: 1");

        verify(topicRepository, never()).save(any());
    }

    @Test
    void updateTopicRejectsSelfParent() {
        Topic topic = topic(1L, "Algebra");
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        assertThatThrownBy(() -> topicService.updateTopic(1L, topicRequest("Algebra", 1L)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("A topic cannot be its own parent.");

        verify(topicRepository, never()).save(any());
    }

    @Test
    void updateTopicRejectsHierarchyCycle() {
        Topic parent = topic(1L, "Algebra");
        Topic child = topic(2L, "Linear Equations");
        Topic grandchild = topic(3L, "Systems");
        child.setParentTopic(parent);
        grandchild.setParentTopic(child);

        when(topicRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(topicRepository.findById(3L)).thenReturn(Optional.of(grandchild));

        assertThatThrownBy(() -> topicService.updateTopic(1L, topicRequest("Algebra", 3L)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Assigning this parent would create a topic hierarchy cycle.");

        verify(topicRepository, never()).save(any());
    }

    @Test
    void deleteTopicRejectsMissingId() {
        when(topicRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.deleteTopic(50L))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("There is no topic with the given ID: 50");

        verify(topicRepository, never()).save(any());
    }

    @Test
    void deleteTopicRecursivelySoftDeletesChildren() throws InvalidInputException {
        Topic parent = topic(1L, "Algebra");
        Topic child = topic(2L, "Linear Equations");
        Topic grandchild = topic(3L, "Systems");
        parent.getSubtopics().add(child);
        child.getSubtopics().add(grandchild);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(parent));

        topicService.deleteTopic(1L);

        assertThat(parent.isDeleted()).isTrue();
        assertThat(child.isDeleted()).isTrue();
        assertThat(grandchild.isDeleted()).isTrue();
        verify(topicRepository).save(parent);
    }

    @Test
    void restoreTopicStillRejectsSubtopicBeforeParent() {
        Topic parent = topic(1L, "Algebra");
        parent.setDeleted(true);
        Topic child = topic(2L, "Linear Equations");
        child.setDeleted(true);
        child.setParentTopic(parent);
        when(topicRepository.findById(2L)).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> topicService.restoreTopic(2L))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Cannot restore a subtopic before restoring its parent topic.");

        verify(topicRepository, never()).save(any());
    }

    private static Topic topic(Long id, String name) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setName(name);
        topic.setDescription(name + " description");
        return topic;
    }

    private static TopicRequest topicRequest(String name, Long parentId) {
        TopicRequest request = new TopicRequest();
        request.setName(name);
        request.setDescription("Description");
        request.setParentId(parentId);
        return request;
    }
}
