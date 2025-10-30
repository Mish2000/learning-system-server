package com.learningsystemserver.repositories;

import com.learningsystemserver.entities.UserSubtopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubtopicProgressRepository extends JpaRepository<UserSubtopicProgress, Long> {

    Optional<UserSubtopicProgress> findByUserIdAndSubtopicId(Long userId, Long subtopicId);

    List<UserSubtopicProgress> findByUserId(Long userId);
}
