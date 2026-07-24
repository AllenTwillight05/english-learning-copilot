package com.englishlearningcopilot.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.englishlearningcopilot.backend.dto.DashboardWeeklyOverviewResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingMessageSender;
import com.englishlearningcopilot.backend.repository.SpeakingMessageRepository;
import com.englishlearningcopilot.backend.repository.SpeakingSessionRepository;
import com.englishlearningcopilot.backend.repository.UserDailyLearningProgressRepository;
import com.englishlearningcopilot.backend.repository.UserDailyPracticeLogRepository;
import com.englishlearningcopilot.backend.repository.UserGrammarbookRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.repository.UserWordbookRepository;
import com.englishlearningcopilot.backend.repository.VocabularyRepository;
import com.englishlearningcopilot.backend.service.impl.DashboardServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private SpeakingSessionRepository speakingSessionRepository;

    @Mock
    private SpeakingMessageRepository speakingMessageRepository;

    @Mock
    private UserGrammarbookRepository userGrammarbookRepository;

    @Mock
    private UserWordbookRepository userWordbookRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private LearningPlanService learningPlanService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDailyLearningProgressRepository userDailyLearningProgressRepository;

    @Mock
    private UserDailyPracticeLogRepository userDailyPracticeLogRepository;

    @Test
    void getWeeklyOverviewUsesSpeakingDurationAndAccuracyFromCurrentWeekMessages() {
        DashboardServiceImpl service = new DashboardServiceImpl(
                speakingSessionRepository,
                speakingMessageRepository,
                userGrammarbookRepository,
                userWordbookRepository,
                vocabularyRepository,
                learningPlanService,
                userRepository,
                userDailyLearningProgressRepository,
                userDailyPracticeLogRepository,
                new ObjectMapper()
        );
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setUsername("learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(userDailyLearningProgressRepository.findLearningDatesInRange(eq(7L), any(), any()))
                .thenReturn(List.of(LocalDate.now().with(java.time.DayOfWeek.MONDAY)));
        when(userDailyPracticeLogRepository.countByUserIdAndPlanDateBetweenAndPracticeType(eq(7L), any(), any(), eq("VOCABULARY")))
                .thenReturn(3L);
        when(userDailyPracticeLogRepository.countByUserIdAndPlanDateBetweenAndPracticeType(eq(7L), any(), any(), eq("GRAMMAR")))
                .thenReturn(4L);
        when(speakingMessageRepository.findBySessionUserIdAndSenderAndCreatedAtBetween(
                eq(7L),
                eq(SpeakingMessageSender.USER),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(List.of(
                userMessage(60_000L, 80, 90, LocalDate.now().with(java.time.DayOfWeek.MONDAY).plusDays(1)),
                userMessage(30_000L, 100, 90, LocalDate.now().with(java.time.DayOfWeek.MONDAY).plusDays(1))
        ));

        DashboardWeeklyOverviewResponse overview = service.getWeeklyOverview("learner");

        assertThat(overview.speakingDuration()).isEqualTo("2 min");
        assertThat(overview.pronunciationAccuracy()).isEqualTo("90%");
        assertThat(overview.learningDays()).isEqualTo("2 天");
        assertThat(overview.vocabularyLearned()).isEqualTo("3 词");
        assertThat(overview.grammarPracticed()).isEqualTo("4 题");
    }

    private static SpeakingMessage userMessage(long durationMs, double totalScore, double accuracy, LocalDate date) {
        SpeakingMessage message = new SpeakingMessage();
        message.setSender(SpeakingMessageSender.USER);
        message.setDurationMs(durationMs);
        message.setPronunciationScore(totalScore);
        message.setPronunciationDetail("""
                {"totalScore":%s,"accuracy":%s,"fluency":80,"integrity":80,"speed":0}
                """.formatted(totalScore, accuracy));
        ReflectionTestUtils.setField(message, "createdAt", date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        return message;
    }
}
