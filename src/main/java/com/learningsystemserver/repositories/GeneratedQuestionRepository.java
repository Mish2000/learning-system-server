package com.learningsystemserver.repositories;

import com.learningsystemserver.entities.GeneratedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratedQuestionRepository extends JpaRepository<GeneratedQuestion, Long> {
}
