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
  reviewGrammarMock,
  reviewVocabularyMock,
  speakingCatalogMock,
  vocabularyMemoryMock,
  vocabularyPracticeProgressMock,
  vocabularyPracticeWordsMock,
  vocabularySnapshotMock,
  vocabularyWordbookWordsMock
} from "./mockData";
import { getStoredAuth } from "./authStorage";

const mockUsers = [
  {
    id: 1,
    username: "learner",
    email: "learner@example.com",
    displayName: "Demo Learner",
    role: "USER",
    enabled: true,
    createdAt: "2026-07-09T00:00:00Z",
    updatedAt: "2026-07-09T00:00:00Z",
    lastLoginAt: null,
    password: "Password123"
  }
];

function toUserResponse(user) {
  const { password, ...safeUser } = user;
  return safeUser;
}

function createMockAuthResponse(user) {
  return {
    token: `mock-token-${user.id}-${Date.now()}`,
    user: toUserResponse({
      ...user,
      lastLoginAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    })
  };
}

function createMockAuthError(message, status = 401) {
  const error = new Error(message);
  error.status = status;
  return error;
}

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
    auth: {
      register: (payload) => {
        const duplicatedUser = mockUsers.find(
          (user) => user.username === payload.username || user.email === payload.email
        );

        if (duplicatedUser) {
          return Promise.reject(createMockAuthError("Username or email is already registered.", 409));
        }

        const now = new Date().toISOString();
        const user = {
          id: mockUsers.length + 1,
          username: payload.username,
          email: payload.email,
          displayName: payload.displayName,
          role: "USER",
          enabled: true,
          createdAt: now,
          updatedAt: now,
          lastLoginAt: now,
          password: payload.password
        };

        mockUsers.push(user);

        return simulateLatency(createMockAuthResponse(user));
      },
      login: ({ account, password }) => {
        const user = mockUsers.find(
          (candidate) => candidate.username === account || candidate.email === account
        );

        if (!user || user.password !== password) {
          return Promise.reject(createMockAuthError("Invalid account or password.", 401));
        }

        return simulateLatency(createMockAuthResponse(user));
      },
      me: () => {
        const { user } = getStoredAuth();

        if (!user) {
          return Promise.reject(createMockAuthError("Authentication is required.", 401));
        }

        return simulateLatency(user);
      },
      logout: () => simulateLatency({ message: "Logged out. Please remove the token on the client." })
    },
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
      getReviewVocabulary: () => simulateLatency(reviewVocabularyMock),
      getVocabularyWordbookWords: () => simulateLatency(vocabularyWordbookWordsMock)
    },
    grammar: {
      getNotebookQuestions: () => simulateLatency(grammarNotebookQuestionsMock),
      getOverview: () => simulateLatency(grammarOverviewMock),
      getReviewGrammar: () => simulateLatency(reviewGrammarMock),
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
