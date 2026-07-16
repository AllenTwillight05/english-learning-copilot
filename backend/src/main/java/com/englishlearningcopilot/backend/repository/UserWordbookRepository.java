package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.UserWordbook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWordbookRepository extends JpaRepository<UserWordbook, Long> {

    Optional<UserWordbook> findByUserIdAndVocabularyId(Long userId, Long vocabularyId);

    List<UserWordbook> findByUserIdOrderByIdDesc(Long userId);
}
