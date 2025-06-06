package com.learningsystemserver.repositories;

import com.learningsystemserver.entities.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByParentTopicIsNull();

    List<Topic> findByParentTopicId(Long parentId);

    Optional<Topic> findByName(String name);

    boolean existsByName(String name);

}
