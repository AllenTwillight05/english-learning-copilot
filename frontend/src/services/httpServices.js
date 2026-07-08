import { API_ENDPOINTS } from "./endpoints";
import { getJson } from "./httpClient";

function withBaseUrl(baseUrl, path) {
  return `${baseUrl}${path}`;
}

// HTTP implementation of AppServices. The object shape mirrors mockServices so
// pages can switch data sources through environment variables only.
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
      getVocabularyMemory: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.vocabularyMemory))
    },
    grammar: {
      getSnapshot: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.grammarSnapshot))
    },
    profile: {
      getSnapshot: () => getJson(withBaseUrl(baseUrl, API_ENDPOINTS.profileSnapshot))
    }
  };
}
