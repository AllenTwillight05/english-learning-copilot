package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.UserDailyLearningProgress;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDailyLearningProgressRepository extends JpaRepository<UserDailyLearningProgress, Long> {

    Optional<UserDailyLearningProgress> findByUserIdAndPlanDate(Long userId, LocalDate planDate);

    List<UserDailyLearningProgress> findByUserIdAndCompletedTrueOrderByPlanDateDesc(Long userId);

    @Query("""
            SELECT COUNT(progress)
            FROM UserDailyLearningProgress progress
            WHERE progress.userId = :userId
              AND progress.planDate BETWEEN :startDate AND :endDate
              AND (progress.vocabularyCompleted > 0 OR progress.grammarCompleted > 0)
            """)
    long countLearningDaysInRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT progress.planDate
            FROM UserDailyLearningProgress progress
            WHERE progress.userId = :userId
              AND progress.planDate BETWEEN :startDate AND :endDate
              AND (progress.vocabularyCompleted > 0 OR progress.grammarCompleted > 0)
            """)
    List<LocalDate> findLearningDatesInRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
