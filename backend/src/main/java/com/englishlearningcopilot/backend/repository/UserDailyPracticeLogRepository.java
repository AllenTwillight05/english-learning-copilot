package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.UserDailyPracticeLog;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDailyPracticeLogRepository extends JpaRepository<UserDailyPracticeLog, Long> {

    boolean existsByUserIdAndPlanDateAndPracticeTypeAndItemId(
            Long userId,
            LocalDate planDate,
            String practiceType,
            String itemId
    );

    long countByUserIdAndPlanDateBetweenAndPracticeType(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String practiceType
    );
}
