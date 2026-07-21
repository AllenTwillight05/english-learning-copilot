package com.englishlearningcopilot.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingMessageSender;
import com.englishlearningcopilot.backend.entity.SpeakingScenario;
import com.englishlearningcopilot.backend.entity.SpeakingSession;
import com.englishlearningcopilot.backend.entity.SpeakingSessionStatus;
import com.englishlearningcopilot.backend.entity.UserRole;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = "debug=false")
class SpeakingRepositoryTest {

    @Autowired
    private SpeakingScenarioRepository scenarioRepository;

    @Autowired
    private SpeakingSessionRepository sessionRepository;

    @Autowired
    private SpeakingMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsOnlyActiveScenariosOrderedByTitle() {
        scenarioRepository.save(scenario("z-scenario", "Zoo", true));
        scenarioRepository.save(scenario("a-scenario", "Airport", true));
        scenarioRepository.save(scenario("hidden", "Hidden", false));

        List<SpeakingScenario> scenarios = scenarioRepository.findByActiveTrueOrderByTitleAsc();

        assertThat(scenarios).extracting(SpeakingScenario::getId)
                .containsExactly("a-scenario", "z-scenario");
    }

    @Test
    void findsSessionHistoryByUsernameNewestFirst() {
        AppUser user = userRepository.save(user("learner"));
        SpeakingScenario scenario = scenarioRepository.save(scenario("business-opening", "Business Opening", true));
        SpeakingSession older = sessionRepository.save(session(user, scenario, Instant.parse("2026-07-20T00:00:00Z")));
        SpeakingSession newer = sessionRepository.save(session(user, scenario, Instant.parse("2026-07-21T00:00:00Z")));

        List<SpeakingSession> sessions = sessionRepository.findByUserUsernameOrderByStartedAtDesc("learner");

        assertThat(sessions).extracting(SpeakingSession::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findsMessagesByTurnThenCreatedTime() {
        AppUser user = userRepository.save(user("learner"));
        SpeakingScenario scenario = scenarioRepository.save(scenario("business-opening", "Business Opening", true));
        SpeakingSession session = sessionRepository.save(session(user, scenario, Instant.parse("2026-07-21T00:00:00Z")));
        SpeakingMessage secondTurn = messageRepository.save(message(session, SpeakingMessageSender.USER, "second", 2));
        SpeakingMessage firstTurn = messageRepository.save(message(session, SpeakingMessageSender.AGENT, "first", 1));

        List<SpeakingMessage> messages = messageRepository.findBySessionIdOrderByTurnIndexAscCreatedAtAsc(session.getId());

        assertThat(messages).extracting(SpeakingMessage::getId)
                .containsExactly(firstTurn.getId(), secondTurn.getId());
    }

    private static AppUser user(String username) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hashed-password");
        user.setDisplayName("Learner");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        return user;
    }

    private static SpeakingScenario scenario(String id, String title, boolean active) {
        SpeakingScenario scenario = new SpeakingScenario();
        scenario.setId(id);
        scenario.setTitle(title);
        scenario.setDescription("description");
        scenario.setDifficulty("starter");
        scenario.setAccent("US");
        scenario.setDuration("5 min");
        scenario.setSummary("summary");
        scenario.setTone("friendly");
        scenario.setGoal("goal");
        scenario.setKeywords("greeting,meeting");
        scenario.setRolePrompt("role prompt");
        scenario.setOpeningMessage("Hello");
        scenario.setTargetTurns(3);
        scenario.setScoringRubric("rubric");
        scenario.setActive(active);
        return scenario;
    }

    private static SpeakingSession session(AppUser user, SpeakingScenario scenario, Instant startedAt) {
        SpeakingSession session = new SpeakingSession();
        session.setUser(user);
        session.setScenario(scenario);
        session.setStatus(SpeakingSessionStatus.ACTIVE);
        session.setStartedAt(startedAt);
        session.setTargetTurns(3);
        session.setCurrentTurn(0);
        return session;
    }

    private static SpeakingMessage message(
            SpeakingSession session,
            SpeakingMessageSender sender,
            String content,
            int turnIndex
    ) {
        SpeakingMessage message = new SpeakingMessage();
        message.setSession(session);
        message.setSender(sender);
        message.setContent(content);
        message.setTurnIndex(turnIndex);
        return message;
    }
}
