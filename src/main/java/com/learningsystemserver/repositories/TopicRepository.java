package com.learningsystemserver.repositories;

import com.learningsystemserver.entities.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    // active (not deleted)
    List<Topic> findByParentTopicIsNullAndDeletedFalse();
    List<Topic> findByParentTopicIdAndDeletedFalse(Long parentId);
    List<Topic> findByDeletedFalse();

    // deleted (for restore UI)
    List<Topic> findByDeletedTrueOrderByNameAsc();

    Optional<Topic> findByName(String name);
    boolean existsByName(String name);
}
