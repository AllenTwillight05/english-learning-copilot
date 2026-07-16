package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.SpeakingScenario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeakingScenarioRepository extends JpaRepository<SpeakingScenario, String> {

    List<SpeakingScenario> findByActiveTrueOrderByTitleAsc();
}
