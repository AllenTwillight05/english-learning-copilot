import {
  dashboardOverviewMock,
  grammarSnapshotMock,
  profileSnapshotMock,
  speakingCatalogMock,
  vocabularyMemoryMock,
  vocabularySnapshotMock
} from "./mockData";

function simulateLatency(value, delay = 120) {
  return new Promise((resolve) => {
    window.setTimeout(() => {
      resolve(structuredClone(value));
    }, delay);
  });
}

// Mock implementation used by default. Keep mock payloads close to contracts so
// frontend members can develop pages before backend endpoints are ready.
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
      getVocabularyMemory: () => simulateLatency(vocabularyMemoryMock)
    },
    grammar: {
      getSnapshot: () => simulateLatency(grammarSnapshotMock)
    },
    profile: {
      getSnapshot: () => simulateLatency(profileSnapshotMock)
    }
  };
}
