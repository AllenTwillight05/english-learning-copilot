import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { SpeakingFeedbackPage } from "../../../src/pages/SpeakingFeedbackPage";
import { speakingScenariosMock } from "../../../src/services/mockData";
import { renderWithProviders } from "../utils/renderWithProviders";

const defaultScenario = speakingScenariosMock[0];

const replayMessages = [
  { id: 1, sender: "AGENT", content: "Welcome back. What would you like to practice first?", audioUrl: "", turnIndex: 0 },
  { id: 2, sender: "USER", content: "I want to review my meeting opening.", audioUrl: "", turnIndex: 1 },
  { id: 3, sender: "AGENT", content: "Great. Please start with a short agenda.", audioUrl: "", turnIndex: 1 },
  { id: 4, sender: "USER", content: "First, I will share the project update.", audioUrl: "", turnIndex: 2 }
];

function createFeedbackServices(session) {
  return {
    speaking: {
      getScenario: (scenarioId) => {
        const scenario = speakingScenariosMock.find((item) => item.id === scenarioId);
        return scenario
          ? Promise.resolve(structuredClone(scenario))
          : Promise.reject(new Error("Speaking scenario was not found."));
      },
      getSession: () => Promise.resolve(structuredClone(session)),
      listHistory: () => Promise.resolve([structuredClone(session)])
    }
  };
}

const session = {
  id: 42,
  userId: 1,
  scenario: defaultScenario,
  status: "ACTIVE",
  startedAt: "2026-07-09T00:00:00.000Z",
  completedAt: null,
  currentTurn: 2,
  targetTurns: 6,
  messages: replayMessages
};

describe("SpeakingFeedbackPage", () => {
  it("opens the replay modal with chat records and turn segments", async () => {
    renderWithProviders(<SpeakingFeedbackPage />, {
      path: "/speaking/:scenarioId/feedback",
      route: `/speaking/${defaultScenario.id}/feedback?sessionId=${session.id}`,
      services: createFeedbackServices(session)
    });

    await userEvent.click(await screen.findByRole("button", { name: /查看回放/ }));

    const dialog = await screen.findByRole("dialog", { name: /会话回放/ });
    expect(within(dialog).getByText("Welcome back. What would you like to practice first?")).toBeInTheDocument();
    expect(within(dialog).getByText("I want to review my meeting opening.")).toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: /暂停/ })).toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: "第 1 轮" })).toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: "第 2 轮" })).toBeInTheDocument();
  });

  it("can pause and resume replay from the modal controls", async () => {
    renderWithProviders(<SpeakingFeedbackPage />, {
      path: "/speaking/:scenarioId/feedback",
      route: `/speaking/${defaultScenario.id}/feedback?sessionId=${session.id}`,
      services: createFeedbackServices(session)
    });

    await userEvent.click(await screen.findByRole("button", { name: /查看回放/ }));
    const dialog = await screen.findByRole("dialog", { name: /会话回放/ });

    await userEvent.click(within(dialog).getByRole("button", { name: /暂停/ }));
    expect(within(dialog).getByRole("button", { name: /开始/ })).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole("button", { name: /开始/ }));
    await waitFor(() => {
      expect(within(dialog).getByRole("button", { name: /暂停/ })).toBeInTheDocument();
    });
  });
});
