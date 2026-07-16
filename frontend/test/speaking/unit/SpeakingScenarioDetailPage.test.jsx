import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SpeakingScenarioDetailPage } from "../../../src/pages/SpeakingScenarioDetailPage";
import { setStoredAuth } from "../../../src/services/authStorage";
import { speakingScenariosMock } from "../../../src/services/mockData";
import { renderWithProviders } from "../utils/renderWithProviders";

const defaultScenario = speakingScenariosMock[0];

function createHistoryServices(history = []) {
  return {
    speaking: {
      getScenario: (scenarioId) => {
        const scenario = speakingScenariosMock.find((item) => item.id === scenarioId);
        return scenario
          ? Promise.resolve(structuredClone(scenario))
          : Promise.reject(new Error("Speaking scenario was not found."));
      },
      listHistory: vi.fn(() => Promise.resolve(structuredClone(history)))
    }
  };
}

describe("SpeakingScenarioDetailPage", () => {
  afterEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  it("shows a no-history notice when backend has no prior practice", async () => {
    setStoredAuth({
      token: "test-token",
      user: { id: 1, username: "learner" }
    });
    const services = createHistoryServices([]);

    renderWithProviders(<SpeakingScenarioDetailPage />, { services });

    const historyButton = await screen.findByRole("button", { name: /历史记录/ });
    expect(historyButton).toBeEnabled();
    await userEvent.click(historyButton);

    expect(await screen.findAllByText("暂无历史记录")).not.toHaveLength(0);
    expect(services.speaking.listHistory).toHaveBeenCalledTimes(1);
  });

  it("opens the latest backend feedback session from history", async () => {
    setStoredAuth({
      token: "test-token",
      user: { id: 1, username: "learner" }
    });
    const services = createHistoryServices([
      {
        id: 42,
        scenario: defaultScenario,
        messages: []
      }
    ]);

    renderWithProviders(<SpeakingScenarioDetailPage />, { services });

    const historyButton = await screen.findByRole("button", { name: /历史记录/ });
    await userEvent.click(historyButton);

    await waitFor(() => {
      expect(screen.getByText("反馈页占位")).toBeInTheDocument();
    });
  });

  it("shows a login warning and stays on detail page when entering conversation anonymously", async () => {
    renderWithProviders(<SpeakingScenarioDetailPage />);

    await userEvent.click(await screen.findByRole("button", { name: /进入会话/ }));

    expect(await screen.findAllByText("用户未登录")).not.toHaveLength(0);
    expect(screen.queryByText("会话页占位")).not.toBeInTheDocument();
  });
});
