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
      createSession: (scenarioId) => {
        const scenario = scenarios.find((item) => item.id === scenarioId);
        if (!scenario) {
          return Promise.reject(new Error("Speaking scenario was not found."));
        }
        const session = {
          id: nextSessionId++,
          userId: 1,
          scenario: structuredClone(scenario),
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
      addMessage: (sessionId, content) => {
        const session = sessions.get(Number(sessionId));
        if (!session) {
          return Promise.reject(new Error("Speaking session was not found."));
        }
        const turnIndex = session.currentTurn + 1;
        const userMessage = createMessage({ sender: "USER", content, turnIndex });
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
          session: updatedSession
        }));
      }
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
            <Route path="/speaking" element={<div>口语页占位</div>} />
          </Routes>
        </MemoryRouter>
      </AppServicesProvider>
    </ConfigProvider>
  );
}
