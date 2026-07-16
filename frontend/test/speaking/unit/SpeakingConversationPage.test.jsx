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

  it("sends a user message, renders the agent reply, and opens backend feedback", async () => {
    renderWithProviders(<SpeakingConversationPage />, {
      path: "/speaking/:scenarioId/conversation",
      route: `/speaking/${defaultScenario.id}/conversation`
    });

    await screen.findByText("Good morning. Could you briefly introduce today's agenda?");
    await userEvent.type(screen.getByPlaceholderText("输入你的英文回答..."), "I would like to review the timeline.");
    await userEvent.click(screen.getByRole("button", { name: /发送/ }));

    expect(await screen.findByText("Nice answer. Could you add one more detail?")).toBeInTheDocument();
    const submitButton = screen.getByRole("button", { name: /交卷/ });
    expect(submitButton).toBeEnabled();
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(window.localStorage.getItem(`speaking-history:${defaultScenario.id}`)).toBeNull();
    });
    expect(await screen.findByText("反馈页占位")).toBeInTheDocument();
  });
});
