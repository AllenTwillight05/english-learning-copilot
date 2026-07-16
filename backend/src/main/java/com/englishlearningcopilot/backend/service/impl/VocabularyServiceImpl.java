package com.englishlearningcopilot.backend.service.impl;

import com.englishlearningcopilot.backend.dto.VocabularyFavoriteRequest;
import com.englishlearningcopilot.backend.dto.VocabularyFavoriteResponse;
import com.englishlearningcopilot.backend.dto.VocabularyPracticeWordResponse;
import com.englishlearningcopilot.backend.dto.VocabularyRatingRequest;
import com.englishlearningcopilot.backend.dto.VocabularyWordbookWordResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserWordbook;
import com.englishlearningcopilot.backend.entity.Vocabulary;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.repository.UserWordbookRepository;
import com.englishlearningcopilot.backend.repository.VocabularyRepository;
import com.englishlearningcopilot.backend.service.VocabularyService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VocabularyServiceImpl implements VocabularyService {

    private static final String UNUSED_TAG = "__unused_vocabulary_tag__";

    private final VocabularyRepository vocabularyRepository;
    private final UserRepository userRepository;
    private final UserWordbookRepository userWordbookRepository;

    public VocabularyServiceImpl(
            VocabularyRepository vocabularyRepository,
            UserRepository userRepository,
            UserWordbookRepository userWordbookRepository
    ) {
        this.vocabularyRepository = vocabularyRepository;
        this.userRepository = userRepository;
        this.userWordbookRepository = userWordbookRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabularyPracticeWordResponse> getPracticeWords(String username, String level) {
        PageRequest limit = PageRequest.of(0, 10);
        String[] tags = getLevelTags(level);

        if (username == null) {
            return vocabularyRepository.findRandomPracticeWordsByTags(
                            tags[0], tags[1], tags[2], tags[3], limit
                    ).stream()
                    .map(VocabularyPracticeWordResponse::from)
                    .toList();
        }

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));

        return vocabularyRepository.findRandomUnlearnedPracticeWordsByTags(
                        user.getId(), tags[0], tags[1], tags[2], tags[3], limit
                ).stream()
                .map(VocabularyPracticeWordResponse::from)
                .toList();
    }

    private String[] getLevelTags(String level) {
        String normalizedLevel = level == null ? "starter" : level.toLowerCase(Locale.ROOT);
        return switch (normalizedLevel) {
            case "starter" -> paddedTags("zk", "gk");
            case "basic" -> paddedTags("cet4");
            case "intermediate" -> paddedTags("cet6");
            case "advanced" -> paddedTags("ky", "toefl", "gre", "ielts");
            default -> throw new IllegalArgumentException("Unsupported vocabulary practice level: " + level);
        };
    }

    private String[] paddedTags(String... tags) {
        String[] padded = {UNUSED_TAG, UNUSED_TAG, UNUSED_TAG, UNUSED_TAG};
        System.arraycopy(tags, 0, padded, 0, tags.length);
        return padded;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabularyWordbookWordResponse> getWordbookWords(String username) {
        AppUser user = getCurrentUser(username);
        List<UserWordbook> wordbookRows = userWordbookRepository.findByUserIdOrderByIdDesc(user.getId());
        List<Long> vocabularyIds = wordbookRows.stream()
                .map(UserWordbook::getVocabularyId)
                .toList();
        Map<Long, Vocabulary> vocabularyById = vocabularyRepository.findAllById(vocabularyIds).stream()
                .collect(Collectors.toMap(Vocabulary::getId, Function.identity()));

        return wordbookRows.stream()
                .map(wordbook -> {
                    Vocabulary vocabulary = vocabularyById.get(wordbook.getVocabularyId());
                    return vocabulary == null
                            ? null
                            : VocabularyWordbookWordResponse.from(vocabulary, wordbook.isFavorited());
                })
                .filter(response -> response != null)
                .toList();
    }

    @Override
    @Transactional
    public void submitRating(String username, VocabularyRatingRequest request) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));
        Long vocabularyId = request.vocabularyId();

        if (!vocabularyRepository.existsById(vocabularyId)) {
            throw new ResourceNotFoundException("Vocabulary word was not found.");
        }

        if (userWordbookRepository.findByUserIdAndVocabularyId(user.getId(), vocabularyId).isPresent()) {
            return;
        }

        UserWordbook wordbook = new UserWordbook();
        wordbook.setUserId(user.getId());
        wordbook.setVocabularyId(vocabularyId);
        userWordbookRepository.save(wordbook);
    }

    @Override
    @Transactional
    public VocabularyFavoriteResponse toggleFavorite(String username, VocabularyFavoriteRequest request) {
        AppUser user = getCurrentUser(username);
        Long vocabularyId = request.vocabularyId();

        if (!vocabularyRepository.existsById(vocabularyId)) {
            throw new ResourceNotFoundException("Vocabulary word was not found.");
        }

        UserWordbook wordbook = userWordbookRepository.findByUserIdAndVocabularyId(user.getId(), vocabularyId)
                .orElseGet(() -> {
                    UserWordbook newWordbook = new UserWordbook();
                    newWordbook.setUserId(user.getId());
                    newWordbook.setVocabularyId(vocabularyId);
                    return newWordbook;
                });

        wordbook.setFavorited(!wordbook.isFavorited());
        UserWordbook savedWordbook = userWordbookRepository.save(wordbook);

        return new VocabularyFavoriteResponse(vocabularyId, savedWordbook.isFavorited());
    }

    private AppUser getCurrentUser(String username) {
        if (username == null) {
            throw new BadCredentialsException("Authentication is required.");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));
    }
}
