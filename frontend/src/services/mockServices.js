import {
  dashboardCommunityLearningTrendsMock,
  dashboardOverviewMock,
  dashboardRecommendedTaskMock,
  dashboardStudyPlanMock,
  dashboardWeeklyOverviewMock,
  grammarNotebookQuestionsMock,
  grammarOverviewMock,
  grammarPracticeQuestionsMock,
  grammarProgressMock,
  grammarSnapshotMock,
  grammarTopicsMock,
  profileSnapshotMock,
  speakingCatalogMock,
  vocabularyMemoryMock,
  vocabularyPracticeProgressMock,
  vocabularyPracticeWordsMock,
  vocabularySnapshotMock,
  vocabularyWordbookWordsMock
} from "./mockData";

function simulateLatency(value, delay = 120) {
  return new Promise((resolve) => {
    window.setTimeout(() => {
      resolve(structuredClone(value));
    }, delay);
  });
}

// 默认使用的 mock 服务。mock 数据尽量贴近 contracts.js，方便后端接口未完成时并行开发。
export function createMockServices() {
  return {
    dashboard: {
      getOverview: () => simulateLatency(dashboardOverviewMock),
      getRecommendedTask: () => simulateLatency(dashboardRecommendedTaskMock),
      getStudyPlan: () => simulateLatency(dashboardStudyPlanMock),
      getWeeklyOverview: () => simulateLatency(dashboardWeeklyOverviewMock),
      getCommunityLearningTrends: () => simulateLatency(dashboardCommunityLearningTrendsMock)
    },
    speaking: {
      getCatalog: () => simulateLatency(speakingCatalogMock)
    },
    vocabulary: {
      getSnapshot: () => simulateLatency(vocabularySnapshotMock),
      getVocabularyMemory: () => simulateLatency(vocabularyMemoryMock),
      getVocabularyPracticeProgress: () => simulateLatency(vocabularyPracticeProgressMock),
      getVocabularyPracticeWords: () => simulateLatency(vocabularyPracticeWordsMock),
      getVocabularyWordbookWords: () => simulateLatency(vocabularyWordbookWordsMock)
    },
    grammar: {
      getNotebookQuestions: () => simulateLatency(grammarNotebookQuestionsMock),
      getOverview: () => simulateLatency(grammarOverviewMock),
      getPracticeQuestions: ({ category } = {}) => {
        const questions = category
          ? grammarPracticeQuestionsMock.filter((question) => question.grammar_category === category)
          : grammarPracticeQuestionsMock;

        return simulateLatency(questions);
      },
      getProgress: () => simulateLatency(grammarProgressMock),
      getTopics: () => simulateLatency(grammarTopicsMock),
      getSnapshot: () => simulateLatency(grammarSnapshotMock)
    },
    profile: {
      getSnapshot: () => simulateLatency(profileSnapshotMock)
    }
  };
}
