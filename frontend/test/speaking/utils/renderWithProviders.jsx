import { render } from "@testing-library/react";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { appTheme } from "../../../src/app/theme";
import { AppServicesProvider } from "../../../src/services/ServiceContext";
import { speakingScenariosMock } from "../../../src/services/mockData";

function createTestServices(scenarios = speakingScenariosMock) {
  let nextSessionId = 1;
  let nextMessageId = 1;
  const sessions = new Map();

  function createMessage({ sender, content, instantTip = null, turnIndex }) {
    return {
      id: nextMessageId++,
      sender,
      content,
      instantTip,
      turnIndex,
      createdAt: new Date().toISOString()
    };
  }

  return {
    speaking: {
      listScenarios: () => Promise.resolve(structuredClone(scenarios)),
      getScenario: (scenarioId) => {
        const scenario = scenarios.find((item) => item.id === scenarioId);
        return scenario
          ? Promise.resolve(structuredClone(scenario))
          : Promise.reject(new Error("Speaking scenario was not found."));
      },
      createSession: (input, selectedTopicArg) => {
        const payload = typeof input === "object" && input !== null
          ? input
          : {
              scenarioId: input,
              ...(selectedTopicArg ? { selectedTopic: selectedTopicArg } : {})
            };
        const scenario = scenarios.find((item) => item.id === payload.scenarioId);
        if (!scenario) {
          return Promise.reject(new Error("Speaking scenario was not found."));
        }
        const session = {
          id: nextSessionId++,
          userId: 1,
          scenario: structuredClone(payload.selectedTopic
            ? { ...scenario, selectedTopic: payload.selectedTopic }
            : scenario),
          status: "ACTIVE",
          startedAt: new Date().toISOString(),
          completedAt: null,
          currentTurn: 0,
          targetTurns: scenario.targetTurns ?? 6,
          messages: [
            createMessage({
              sender: "AGENT",
              content: scenario.openingMessage ?? scenario.prompts?.[0]?.text ?? "Let's start.",
              turnIndex: 0
            })
          ]
        };
        sessions.set(session.id, session);
        return Promise.resolve(structuredClone(session));
      },
      getSession: (sessionId) => {
        const session = sessions.get(Number(sessionId));
        return session
          ? Promise.resolve(structuredClone(session))
          : Promise.reject(new Error("Speaking session was not found."));
      },
      listHistory: () => {
        const history = Array.from(sessions.values())
          .sort((left, right) => new Date(right.startedAt).getTime() - new Date(left.startedAt).getTime());
        return Promise.resolve(structuredClone(history));
      },
      submitRecording: (sessionId, _audioBlob) => {
        const session = sessions.get(Number(sessionId));
        if (!session) {
          return Promise.reject(new Error("Speaking session was not found."));
        }
        const turnIndex = session.currentTurn + 1;
        const transcribedText = "This is a mock transcript of the recorded speech.";
        const userMessage = {
          ...createMessage({ sender: "USER", content: transcribedText, turnIndex }),
          audioUrl: `speaking/${sessionId}/99.webm`,
          transcribedText,
          pronunciationScore: 85,
          pronunciationDetail: JSON.stringify({ totalScore: 85, accuracy: 87, fluency: 82, integrity: 88, speed: 120 })
        };
        const agentMessage = createMessage({
          sender: "AGENT",
          content: "Nice answer. Could you add one more detail?",
          instantTip: "Add a reason or example.",
          turnIndex
        });
        const updatedSession = {
          ...session,
          currentTurn: turnIndex,
          messages: [...session.messages, userMessage, agentMessage]
        };
        sessions.set(updatedSession.id, updatedSession);
        return Promise.resolve(structuredClone({
          userMessage,
          agentMessage,
          pronunciationScore: { totalScore: 85, accuracy: 87, fluency: 82, integrity: 88, speed: 120 },
          session: updatedSession
        }));
      },
      getFeedback: (sessionId) => Promise.resolve({
        totalScore: 85,
        pronunciation: 87,
        fluency: 82,
        integrity: 88,
        speed: "120 WPM",
        issueSentences: ["Test sentence."],
        suggestions: ["Suggestion 1", "Suggestion 2", "Suggestion 3"],
        scenarioTitle: "Test Scenario",
        totalTurns: 1,
        averagePronunciationScore: 85,
        turns: [{ turnIndex: 1, userText: "test", agentText: "reply", score: null }],
        agentOverallComment: "Keep practicing!"
      })
    }
  };
}

export function renderWithProviders(
  ui,
  {
    route = `/speaking/${speakingScenariosMock[0].id}`,
    path = "/speaking/:scenarioId",
    services = createTestServices()
  } = {}
) {
  return render(
    <ConfigProvider locale={zhCN} theme={appTheme}>
      <AppServicesProvider services={services}>
        <MemoryRouter initialEntries={[route]}>
          <Routes>
            <Route path={path} element={ui} />
            <Route path="/speaking/:scenarioId/feedback" element={<div>反馈页占位</div>} />
            <Route path="/speaking/:scenarioId/conversation" element={<div>会话页占位</div>} />
            <Route path="/speaking/:scenarioId" element={<div>详情页占位</div>} />
            <Route path="/speaking/daily" element={<div>日常口语页占位</div>} />
            <Route path="/speaking/ielts" element={<div>雅思口语页占位</div>} />
            <Route path="/speaking" element={<div>口语页占位</div>} />
          </Routes>
        </MemoryRouter>
      </AppServicesProvider>
    </ConfigProvider>
  );
}
