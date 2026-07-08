/**
 * 前后端数据形状说明。
 *
 * 当前项目已经切到 JavaScript，这里不再用 TS interface，而是用示例结构
 * 记录每个接口应该返回哪些字段，方便前端 mock 和后端联调保持一致。
 */
export const contractShapes = {
  dashboardOverview: {
    productTag: "string",
    stackTag: "string",
    headline: "string",
    description: "string",
    primaryActionLabel: "string",
    secondaryActionLabel: "string",
    quickStats: [{ label: "string", value: "string", icon: "microphone" }],
    focusIntensity: "string",
    suggestedDuration: "string"
  },
  speakingCatalog: {
    modes: ["string"],
    scriptPreviewTitle: "string",
    scriptPreviewLines: ["string"],
    scenarios: [
      {
        id: "string",
        title: "string",
        level: "string",
        accent: "string",
        duration: "string",
        summary: "string",
        tone: "blue"
      }
    ]
  },
  vocabularySnapshot: {
    dailyGoal: "string",
    retentionHint: "string",
    cards: [{ id: "string", word: "string", usage: "string", progress: 0, tag: "string" }]
  },
  vocabularyMemory: {
    retentionLabel: "string",
    retentionRate: 0,
    stats: [{ value: "string", label: "string" }]
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
