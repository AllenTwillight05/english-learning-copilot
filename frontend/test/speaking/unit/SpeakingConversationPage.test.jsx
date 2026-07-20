import { screen } from "@testing-library/react";
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
});
