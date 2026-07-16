package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.dto.VocabularyFavoriteRequest;
import com.englishlearningcopilot.backend.dto.VocabularyFavoriteResponse;
import com.englishlearningcopilot.backend.dto.VocabularyPracticeWordResponse;
import com.englishlearningcopilot.backend.dto.VocabularyRatingRequest;
import com.englishlearningcopilot.backend.dto.VocabularyWordbookWordResponse;
import java.util.List;

public interface VocabularyService {

    List<VocabularyPracticeWordResponse> getPracticeWords(String username, String level);

    List<VocabularyWordbookWordResponse> getWordbookWords(String username);

    void submitRating(String username, VocabularyRatingRequest request);

    VocabularyFavoriteResponse toggleFavorite(String username, VocabularyFavoriteRequest request);
}
