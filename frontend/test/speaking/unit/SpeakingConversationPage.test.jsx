import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SpeakingConversationPage } from "../../../src/pages/SpeakingConversationPage";
import { speakingScenariosMock } from "../../../src/services/mockData";
import { renderWithProviders } from "../utils/renderWithProviders";

const defaultScenario = speakingScenariosMock[0];

describe("SpeakingConversationPage", () => {
  afterEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  it("creates a backend-style session and speaks the opening message", async () => {
    renderWithProviders(<SpeakingConversationPage />, {
      path: "/speaking/:scenarioId/conversation",
      route: `/speaking/${defaultScenario.id}/conversation`
    });

    const playbackButtons = await screen.findAllByRole("button", { name: "播放此句音频" });
    expect(playbackButtons).toHaveLength(1);

    await userEvent.click(playbackButtons[0]);
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1);
  });

  it("does not render the removed text message composer", async () => {
    renderWithProviders(<SpeakingConversationPage />, {
      path: "/speaking/:scenarioId/conversation",
      route: `/speaking/${defaultScenario.id}/conversation`
    });

    await screen.findByText("Good morning. Could you briefly introduce today's agenda?");

    expect(screen.queryByPlaceholderText("输入你的英文回答...")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /发送/ })).not.toBeInTheDocument();
  });

  it("keeps one notice bubble and lets a new recording error replace the previous tip", async () => {
    const services = {
      speaking: {
        listScenarios: () => Promise.resolve([defaultScenario]),
        getScenario: () => Promise.resolve(defaultScenario),
        createSession: () => Promise.resolve({
          id: 1,
          scenario: defaultScenario,
          currentTurn: 1,
          messages: [
            {
              id: 1,
              sender: "AGENT",
              content: "Good morning. Could you briefly introduce today's agenda?",
              instantTip: "Try using a complete sentence.",
              turnIndex: 0
            }
          ]
        }),
        submitRecording: () => Promise.reject(new Error("没有识别到语音"))
      }
    };

    const { container } = renderWithProviders(<SpeakingConversationPage />, {
      path: "/speaking/:scenarioId/conversation",
      route: `/speaking/${defaultScenario.id}/conversation`,
      services
    });

    expect(await screen.findByText("Try using a complete sentence.")).toBeInTheDocument();
    expect(container.querySelectorAll(".speaking-alert")).toHaveLength(1);

    await userEvent.click(screen.getByRole("button", { name: /开始录音/ }));
    const stopButton = screen.getByRole("button", { name: /停止录音/ });
    await waitFor(() => {
      expect(stopButton).toBeEnabled();
    });
    await userEvent.click(stopButton);

    expect(await screen.findByText("没有识别到语音")).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByText("Try using a complete sentence.")).not.toBeInTheDocument();
      expect(container.querySelectorAll(".speaking-alert")).toHaveLength(1);
    });
  });
});
