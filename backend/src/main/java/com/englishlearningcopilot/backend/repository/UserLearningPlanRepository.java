package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.UserLearningPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLearningPlanRepository extends JpaRepository<UserLearningPlan, Long> {

    Optional<UserLearningPlan> findByUserId(Long userId);
}
