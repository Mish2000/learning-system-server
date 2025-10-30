package com.learningsystemserver.repositories;

import com.learningsystemserver.entities.UserQuestionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserQuestionHistoryRepository extends JpaRepository<UserQuestionHistory, Long> {

    // Recent attempts (per user + subtopic) for rolling accuracy
    List<UserQuestionHistory> findTop10ByUserIdAndQuestion_Topic_IdOrderByAttemptTimeDesc(Long userId, Long subtopicId);
}
