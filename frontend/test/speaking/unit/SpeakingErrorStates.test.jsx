import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { SpeakingConversationPage } from "../../../src/pages/SpeakingConversationPage";
import { SpeakingFeedbackPage } from "../../../src/pages/SpeakingFeedbackPage";
import { SpeakingScenarioDetailPage } from "../../../src/pages/SpeakingScenarioDetailPage";
import { speakingScenariosMock } from "../../../src/services/mockData";
import { renderWithProviders } from "../utils/renderWithProviders";

const defaultScenario = speakingScenariosMock[0];

const incompleteScenarios = [
  {
    id: "incomplete",
    title: "缺字段场景",
    level: "A1",
    accent: "基础表达",
    duration: "5 min",
    summary: "用于测试缺失字段。",
    tone: "blue",
    goal: "确认页面不会因为缺字段崩溃。"
  }
];

function createScenarioServices(scenarios) {
  let nextMessageId = 1;

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
        return Promise.resolve({
          id: 1,
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
            {
              id: nextMessageId++,
              sender: "AGENT",
              content: scenario.openingMessage ?? "Let's start.",
              instantTip: null,
              turnIndex: 0,
              createdAt: new Date().toISOString()
            }
          ]
        });
      },
      getSession: () => Promise.reject(new Error("Speaking session was not found.")),
      listHistory: () => Promise.resolve([]),
      }
    };
}

describe("speaking exceptional states", () => {
  it("shows the missing scenario state when the scenario id does not exist", async () => {
    renderWithProviders(<SpeakingScenarioDetailPage />, {
      route: "/speaking/not-found"
    });

    expect(await screen.findByText("没有找到这个情景")).toBeInTheDocument();
  });

  it("shows a visible notice when no backend replay session exists", async () => {
    renderWithProviders(<SpeakingFeedbackPage />, {
      path: "/speaking/:scenarioId/feedback",
      route: `/speaking/${defaultScenario.id}/feedback`
    });

    expect(await screen.findByText("评分结果")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("暂无历史会话记录");
  });

  it("degrades optional fields but still allows backend session entry when prompts are missing", async () => {
    const services = {
      ...createScenarioServices(incompleteScenarios)
    };

    renderWithProviders(<SpeakingScenarioDetailPage />, {
      route: "/speaking/incomplete",
      services
    });
    expect(await screen.findByText("缺字段场景")).toBeInTheDocument();
    expect(screen.getByText("暂无关键词")).toBeInTheDocument();
    expect(screen.getByText("对话示例")).toBeInTheDocument();
    expect(screen.getByText("暂无对话示例。你仍然可以进入会话，由后端开场白开始练习。")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /进入会话/ })).toBeEnabled();
  });

  it("renders backend sample dialogue when prompts are missing", async () => {
    const services = {
      ...createScenarioServices([
        {
          ...incompleteScenarios[0],
          sampleDialogue: "Coach: Welcome to the practice.\nLearner: I am ready to begin."
        }
      ])
    };

    renderWithProviders(<SpeakingScenarioDetailPage />, {
      route: "/speaking/incomplete",
      services
    });

    expect(await screen.findByText("Coach: Welcome to the practice.")).toBeInTheDocument();
    expect(screen.getByText("Learner: I am ready to begin.")).toBeInTheDocument();
  });

  it("starts a session even when prompt scripts are missing", async () => {
    const services = {
      ...createScenarioServices(incompleteScenarios)
    };

    renderWithProviders(<SpeakingConversationPage />, {
      path: "/speaking/:scenarioId/conversation",
      route: "/speaking/incomplete/conversation",
      services
    });
    expect(await screen.findByText("Let's start.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /开始录音/ })).toBeEnabled();
    expect(screen.getByRole("button", { name: /交卷/ })).toBeDisabled();

    await userEvent.click(screen.getByRole("button", { name: /开始录音/ }));
    expect(screen.getByText(/正在录音/)).toBeInTheDocument();
    expect(window.localStorage.getItem("speaking-history:incomplete")).toBeNull();
  });

  it("shows an explicit feedback error instead of a misleading zero score when feedback is missing", async () => {
    const services = {
      ...createScenarioServices(incompleteScenarios)
    };

    renderWithProviders(<SpeakingFeedbackPage />, {
      path: "/speaking/:scenarioId/feedback",
      route: "/speaking/incomplete/feedback",
      services
    });
    expect(await screen.findByText("评分结果")).toBeInTheDocument();
    expect(screen.getByText("暂无历史会话记录。请先进入会话完成一次文本练习。")).toBeInTheDocument();
    expect(screen.queryByText("N/A")).not.toBeInTheDocument();
  });
});
