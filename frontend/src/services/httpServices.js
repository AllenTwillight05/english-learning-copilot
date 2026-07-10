import { API_ENDPOINTS } from "./endpoints";
import { getJson } from "./httpClient";

function withBaseUrl(baseUrl, path) {
  return `${baseUrl}${path}`;
}

// 真实接口服务实现，结构要和 mockServices 保持一致，页面才能只通过环境变量切换数据源。
export function createHttpServices(baseUrl = "") {
  return {
    dashboard: {
      getOverview: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.dashboardOverview))
    },
    speaking: {
      getCatalog: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.speakingCatalog))
    },
    vocabulary: {
      getSnapshot: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.vocabularySnapshot)),
      getVocabularyMemory: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.vocabularyMemory)),
      getVocabularyPracticeProgress: () =>
        getJson(withBaseUrl(baseUrl, API_ENDPOINTS.vocabularyPracticeProgress)),
      getVocabularyPracticeWords: () =>
        getJson(withBaseUrl(baseUrl, API_ENDPOINTS.vocabularyPracticeWords)),
      getVocabularyWordbookWords: () =>
        getJson(withBaseUrl(baseUrl, API_ENDPOINTS.vocabularyWordbookWords))
    },
    grammar: {
      getNotebookQuestions: () =>
        getJson(withBaseUrl(baseUrl, API_ENDPOINTS.grammarNotebookQuestions)),
      getOverview: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.grammarOverview)),
      getPracticeQuestions: ({ category } = {}) => {
        const query = category ? `?category=${encodeURIComponent(category)}` : "";
        return getJson(withBaseUrl(baseUrl, `${API_ENDPOINTS.grammarPracticeQuestions}${query}`));
      },
      getProgress: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.grammarProgress)),
      getTopics: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.grammarTopics)),
      getSnapshot: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.grammarSnapshot))
    },
    profile: {
      getSnapshot: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.profileSnapshot))
    }
  };
}
