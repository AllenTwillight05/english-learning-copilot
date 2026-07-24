/**
 * 前后端数据形状说明。
 *
 * 当前项目已经切到 JavaScript，这里不再用 TS interface，而是用示例结构
 * 记录每个接口应该返回哪些字段，方便前端 mock 和后端联调保持一致。
 */
export const contractShapes = {
  authResponse: {
    token: "string",
    user: {
      id: 1,
      username: "string",
      email: "string",
      displayName: "string",
      role: "USER",
      enabled: true,
      createdAt: "2026-07-09T00:00:00Z",
      updatedAt: "2026-07-09T00:00:00Z",
      lastLoginAt: null
    }
  },
  authLoginRequest: {
    account: "string",
    password: "string"
  },
  authRegisterRequest: {
    username: "string",
    email: "string",
    password: "string",
    displayName: "string"
  },
  dashboardOverview: {
    primaryActionLabel: "string",
    secondaryActionLabel: "string",
    quickStats: [{ label: "string", value: "string", icon: "microphone" }],
    focusIntensity: "string",
    suggestedDuration: "string"
  },
  dashboardRecommendedTask: {
    topic: "string",
    suggestedDuration: "string",
    intensity: "string"
  },
  dashboardStudyPlan: {
    speaking: {
      completed: 0,
      total: 0
    },
    vocabulary: {
      completed: 0,
      total: 0,
      remaining: 0,
      done: false
    },
    grammar: {
      completed: 0,
      total: 0,
      remaining: 0,
      done: false
    },
    streakDays: 0,
    allDone: false
  },
  dashboardWeeklyOverview: {
    speakingDuration: "string",
    pronunciationAccuracy: "string",
    learningDays: "string",
    vocabularyLearned: "string",
    grammarPracticed: "string"
  },
  dashboardCommunityLearningTrends: {
    speaking: [
      {
        rank: 1,
        scenarioId: "string",
        topic: "string",
        description: "string",
        learnerCount: 0
      }
    ],
    vocabulary: [
      {
        rank: 1,
        vocabularyId: 1,
        word: "string",
        briefTranslation: "string",
        learnerCount: 0
      }
    ],
    grammar: [
      {
        rank: 1,
        grammarCategory: "string",
        learnerCount: 0
      }
    ]
  },
  speakingScenarios: [
    {
      id: "string",
      title: "string",
      description: "string",
      difficulty: "B2",
      level: "B2",
      accent: "string",
      duration: "string",
      summary: "string",
      tone: "blue",
      goal: "string",
      keywords: ["string"],
      openingMessage: "string",
      sampleDialogue: "Coach: string\nLearner: string",
      targetTurns: 6,
      scoringRubric: "string",
      prompts: [{ role: "coach", text: "string" }],
      feedback: {
        totalScore: 0,
        pronunciation: 0,
        fluency: 0,
        integrity: 0,
        speed: "string",
        issueSentences: ["string"],
        suggestions: ["string"]
      }
    }
  ],
  speakingCreateSessionRequest: {
    scenarioId: "string",
    selectedTopic: "string"
  },
  speakingSession: {
    id: 1,
    userId: 1,
    scenario: { id: "string", title: "string" },
    status: "ACTIVE",
    startedAt: "2026-07-09T00:00:00Z",
    completedAt: null,
    currentTurn: 0,
    targetTurns: 6,
    messages: [
      {
        id: 1,
        sender: "AGENT",
        content: "string",
        spokenText: "string",
        audioUrl: null,
        autoPlay: true,
        transcribedText: null,
        pronunciationScore: null,
        pronunciationDetail: null,
        instantTip: null,
        turnIndex: 0,
        createdAt: "2026-07-09T00:00:00Z"
      }
    ],
    feedback: {
      totalScore: 0,
      pronunciation: 0,
      fluency: 0,
      integrity: 0,
      speed: "string",
      issueSentences: ["string"],
      suggestions: ["string"],
      scenarioTitle: "string",
      totalTurns: 0,
      averagePronunciationScore: 0,
      turns: [
        {
          turnIndex: 0,
          userText: "string",
          agentText: "string",
          score: {
            totalScore: 0,
            accuracy: 0,
            fluency: 0,
            integrity: 0,
            speed: 0
          }
        }
      ],
      agentOverallComment: "string"
    }
  },
  vocabularySnapshot: {
    dailyGoal: "string",
    retentionHint: "string",
    cards: [{ id: "string", word: "string", usage: "string", progress: 0, tag: "string" }]
  },
  vocabularyMemory: {
    retentionRate: 0,
    stats: [{ value: "string", label: "string" }]
  },
  vocabularyPracticeProgress: {
    completed: 0,
    total: 0,
    remaining: 0,
    done: false
  },
  vocabularyPracticeWords: [
    {
      id: "string",
      word: "string",
      phonetic: "string",
      definition: "string",
      briefTranslation: "string",
      translation: "string",
      collins: "string",
      oxford: "string",
      tag: "string",
      bnc: "string",
      frq: "string",
      exchange: "string",
      uk_audio: "string",
      us_audio: "string",
      favorited: false,
      chineseOptions: ["string"],
      englishOptions: ["string"]
    }
  ],
  reviewVocabulary: [
    {
      id: "string",
      word: "string",
      phonetic: "string",
      definition: "string",
      briefTranslation: "string",
      translation: "string",
      collins: "string",
      oxford: "string",
      tag: "string",
      bnc: "string",
      frq: "string",
      exchange: "string",
      uk_audio: "string",
      us_audio: "string",
      chineseOptions: ["string"],
      englishOptions: ["string"]
    }
  ],
  vocabularyWordbookWords: [
    {
      id: "string",
      word: "string",
      phonetic: "string",
      definition: "string",
      briefTranslation: "string",
      translation: "string",
      tag: "string",
      us_audio: "string",
      favorited: true
    }
  ],
  vocabularyFavoriteResponse: {
    vocabularyId: 0,
    favorited: true
  },
  grammarSnapshot: {
    focus: "string",
    topics: [
      {
        id: "string",
        title: "string",
        summary: "string",
        examples: ["string"],
        progress: 0,
        tag: "string"
      }
    ]
  },
  grammarOverview: {
    masteryRate: 0,
    stats: [{ value: "string", label: "string" }]
  },
  grammarNotebookQuestions: [
    {
      id: 0,
      question_text: "string",
      options: ["string"],
      answer: "A",
      grammar_category: "string",
      explanation: "string",
      wrong: true,
      favorited: true
    }
  ],
  grammarPracticeResultRequest: {
    grammarQuestionId: 0,
    incorrect: true
  },
  grammarRatingRequest: {
    grammarQuestionId: 0,
    score: 1
  },
  grammarFavoriteRequest: {
    grammarQuestionId: 0
  },
  grammarFavoriteResponse: {
    grammarQuestionId: 0,
    favorited: true
  },
  grammarPracticeQuestions: [
    {
      id: 0,
      question_text: "string",
      options: ["string"],
      answer: "A",
      grammar_category: "string",
      explanation: "string"
    }
  ],
  reviewGrammar: [
    {
      id: 0,
      question_text: "string",
      options: ["string"],
      answer: "A",
      grammar_category: "string",
      explanation: "string"
    }
  ],
  grammarProgress: {
    completed: 0,
    total: 0,
    remaining: 0,
    done: false
  },
  grammarTopics: [
    {
      id: "string",
      title: "string",
      summary: "string",
      examples: ["string"],
      progress: 0,
      tag: "string"
    }
  ],
  profileSnapshot: {
    learnerName: "string",
    level: "string",
    streak: "string",
    feedback: {
      statusLabel: "string",
      playbackActionLabel: "string",
      metrics: [{ key: "string", label: "string", value: "string" }],
      notes: ["string"],
      scenarioTitle: "string",
      completedAt: "2026-07-09T00:00:00Z",
      totalScore: 0,
      pronunciation: 0,
      fluency: 0,
      integrity: 0,
      issueSentence: "string"
    },
    dailyPlan: {
      autoPilotEnabled: true,
      weeklyImprovement: "string",
      dailyVocabularyGoal: 0,
      dailyGrammarGoal: 0,
      vocabulary: {
        completed: 0,
        total: 0,
        remaining: 0,
        done: false
      },
      grammar: {
        completed: 0,
        total: 0,
        remaining: 0,
        done: false
      },
      allDone: false,
      items: [{ id: "string", time: "string", task: "string", meta: "string", done: false }],
      progress: [{ id: "string", label: "string", value: 0, tone: "default" }]
    }
  },
  learningPlanRequest: {
    dailyVocabularyGoal: 0,
    dailyGrammarGoal: 0
  },
  learningPlanResponse: {
    dailyVocabularyGoal: 0,
    dailyGrammarGoal: 0,
    enabled: true
  },
  dailyLearningStatus: {
    date: "2026-07-20",
    vocabulary: {
      completed: 0,
      total: 0,
      remaining: 0,
      done: false
    },
    grammar: {
      completed: 0,
      total: 0,
      remaining: 0,
      done: false
    },
    allDone: false,
    streakDays: 0
  }
};
