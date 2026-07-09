import {
  dashboardOverviewMock,
  grammarSnapshotMock,
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
      getOverview: () => simulateLatency(dashboardOverviewMock)
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
      getSnapshot: () => simulateLatency(grammarSnapshotMock)
    },
    profile: {
      getSnapshot: () => simulateLatency(profileSnapshotMock)
    }
  };
}
