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
      total: 0
    },
    grammar: {
      completed: 0,
      total: 0
    },
    streakDays: 0
  },
  dashboardWeeklyOverview: {
    speakingDuration: "string",
    pronunciationAccuracy: "string",
    learningDays: "string",
    vocabularyLearned: "string",
    grammarPracticed: "string"
  },
  dashboardCommunityLearningTrends: {
    speaking: [{ topic: "string", description: "string" }],
    vocabulary: [{ word: "string", briefTranslation: "string" }],
    grammar: [{ topic: "string" }]
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
      targetTurns: 6,
      scoringRubric: "string",
      prompts: [{ role: "coach", text: "string" }],
      feedback: {
        totalScore: 0,
        pronunciation: 0,
        fluency: 0,
        speed: "string",
        issueSentences: ["string"],
        suggestions: ["string"]
      }
    }
  ],
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
    total: 0
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
      tag: "string",
      us_audio: "string",
      favorited: true
    }
  ],
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
  grammarProgress: {
    completed: 0,
    total: 0
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
      notes: ["string"]
    },
    dailyPlan: {
      autoPilotEnabled: true,
      weeklyImprovement: "string",
      items: [{ id: "string", time: "string", task: "string", meta: "string", done: false }],
      progress: [{ id: "string", label: "string", value: 0, tone: "default" }]
    }
  }
};
