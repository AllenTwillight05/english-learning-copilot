import {
  dashboardOverviewMock,
  dashboardRecommendedTaskMock,
  dashboardStudyPlanMock,
  dashboardWeeklyOverviewMock,
  grammarOverviewMock,
  grammarProgressMock,
  grammarSnapshotMock,
  grammarTopicsMock,
  profileSnapshotMock,
  reviewGrammarMock,
  reviewVocabularyMock,
  speakingScenariosMock,
  vocabularyMemoryMock,
  vocabularyPracticeProgressMock,
  vocabularySnapshotMock
} from "./mockData";
import { getStoredAuth } from "./authStorage";
import { createHttpServices } from "./httpServices";

const httpServices = createHttpServices(import.meta.env.VITE_API_BASE_URL ?? "");

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

let nextSpeakingSessionId = 1;
let nextSpeakingMessageId = 1;
const mockSpeakingSessions = new Map();

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

function toSpeakingMessageResponse({ sender, content, instantTip = null, turnIndex }) {
  return {
    id: nextSpeakingMessageId++,
    sender,
    content,
    instantTip,
    turnIndex,
    createdAt: new Date().toISOString()
  };
}

function toSpeakingSessionResponse({ scenario, messages, currentTurn = 0 }) {
  return {
    id: nextSpeakingSessionId++,
    userId: getStoredAuth().user?.id ?? 1,
    scenario,
    status: "ACTIVE",
    startedAt: new Date().toISOString(),
    completedAt: null,
    currentTurn,
    targetTurns: scenario.targetTurns ?? 6,
    messages
  };
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
      getCommunityLearningTrends: () => httpServices.dashboard.getCommunityLearningTrends()
    },
    speaking: {
      listScenarios: () => simulateLatency(speakingScenariosMock),
      getScenario: (scenarioId) => {
        const scenario = speakingScenariosMock.find((item) => item.id === scenarioId);
        return scenario
          ? simulateLatency(scenario)
          : Promise.reject(new Error("Speaking scenario was not found."));
      },
      createSession: (scenarioId) => {
        const scenario = speakingScenariosMock.find((item) => item.id === scenarioId);
        if (!scenario) {
          return Promise.reject(new Error("Speaking scenario was not found."));
        }

        const openingMessage = scenario.prompts?.find((message) => message.role === "coach")?.text
          ?? scenario.openingMessage
          ?? "Let's start this speaking practice.";
        const session = toSpeakingSessionResponse({
          scenario,
          messages: [
            toSpeakingMessageResponse({
              sender: "AGENT",
              content: openingMessage,
              turnIndex: 0
            })
          ]
        });
        mockSpeakingSessions.set(session.id, session);
        return simulateLatency(session);
      },
      getSession: (sessionId) => {
        const session = mockSpeakingSessions.get(Number(sessionId));
        return session
          ? simulateLatency(session)
          : Promise.reject(new Error("Speaking session was not found."));
      },
      listHistory: () => {
        const sessions = Array.from(mockSpeakingSessions.values())
          .sort((left, right) => new Date(right.startedAt).getTime() - new Date(left.startedAt).getTime());
        return simulateLatency(sessions);
      },
      addMessage: (sessionId, content) => {
        const session = mockSpeakingSessions.get(Number(sessionId));
        if (!session) {
          return Promise.reject(new Error("Speaking session was not found."));
        }

        const turnIndex = session.currentTurn + 1;
        const userMessage = toSpeakingMessageResponse({
          sender: "USER",
          content,
          turnIndex
        });
        const agentMessage = toSpeakingMessageResponse({
          sender: "AGENT",
          content: "Nice answer. Could you add one more detail and make it sound more natural?",
          instantTip: "Try adding a reason or example to make your response fuller.",
          turnIndex
        });
        const updatedSession = {
          ...session,
          currentTurn: turnIndex,
          messages: [...session.messages, userMessage, agentMessage]
        };
        mockSpeakingSessions.set(updatedSession.id, updatedSession);

        return simulateLatency({
          userMessage,
          agentMessage,
          session: updatedSession
        });
      }
    },
    vocabulary: {
      getSnapshot: () => simulateLatency(vocabularySnapshotMock),
      getVocabularyMemory: () => simulateLatency(vocabularyMemoryMock),
      getVocabularyPracticeProgress: () => simulateLatency(vocabularyPracticeProgressMock),
      getVocabularyPracticeWords: (options) =>
        httpServices.vocabulary.getVocabularyPracticeWords(options),
      submitVocabularyRating: (payload) => httpServices.vocabulary.submitVocabularyRating(payload),
      getReviewVocabulary: () => simulateLatency(reviewVocabularyMock),
      getVocabularyWordbookWords: () => httpServices.vocabulary.getVocabularyWordbookWords(),
      toggleVocabularyFavorite: (payload) => httpServices.vocabulary.toggleVocabularyFavorite(payload)
    },
    grammar: {
      getNotebookQuestions: () => httpServices.grammar.getNotebookQuestions(),
      submitGrammarPracticeResult: (payload) =>
        httpServices.grammar.submitGrammarPracticeResult(payload),
      submitGrammarRating: (payload) => httpServices.grammar.submitGrammarRating(payload),
      toggleGrammarFavorite: (payload) => httpServices.grammar.toggleGrammarFavorite(payload),
      getOverview: () => simulateLatency(grammarOverviewMock),
      getReviewGrammar: () => simulateLatency(reviewGrammarMock),
      getPracticeQuestions: (options) => httpServices.grammar.getPracticeQuestions(options),
      getProgress: () => simulateLatency(grammarProgressMock),
      getTopics: () => simulateLatency(grammarTopicsMock),
      getSnapshot: () => simulateLatency(grammarSnapshotMock)
    },
    profile: {
      getSnapshot: () => simulateLatency(profileSnapshotMock)
    }
  };
}
