import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
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
      listHistory: () => Promise.resolve([structuredClone(session)]),
      getFeedback: () => Promise.resolve({
        totalScore: 88,
        pronunciation: 91,
        fluency: 84,
        speed: "136 WPM",
        issueSentences: ["I want to review my meeting opening."],
        suggestions: [
          "Try adding more detail to make your responses fuller.",
          "Pay attention to sentence stress."
        ]
      })
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
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("opens the replay modal with chat records and controls", async () => {
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

  it("restarts replay from the beginning after a full playback finishes", async () => {
    const spokenTexts = [];
    vi.spyOn(window.speechSynthesis, "speak").mockImplementation((utterance) => {
      spokenTexts.push(utterance.text);
      window.setTimeout(() => utterance.onend?.(), 0);
    });

    renderWithProviders(<SpeakingFeedbackPage />, {
      path: "/speaking/:scenarioId/feedback",
      route: `/speaking/${defaultScenario.id}/feedback?sessionId=${session.id}`,
      services: createFeedbackServices(session)
    });

    await userEvent.click(await screen.findByRole("button", { name: /查看回放/ }));
    const dialog = await screen.findByRole("dialog", { name: /会话回放/ });

    await waitFor(() => {
      expect(spokenTexts).toHaveLength(replayMessages.length);
      expect(within(dialog).getByRole("button", { name: /开始/ })).toBeInTheDocument();
    });

    await userEvent.click(within(dialog).getByRole("button", { name: /开始/ }));

    await waitFor(() => {
      expect(spokenTexts[replayMessages.length]).toBe(replayMessages[0].content);
    });
  });

  it("displays feedback scores when feedback is available", async () => {
    renderWithProviders(<SpeakingFeedbackPage />, {
      path: "/speaking/:scenarioId/feedback",
      route: `/speaking/${defaultScenario.id}/feedback?sessionId=${session.id}`,
      services: createFeedbackServices(session)
    });

    expect(await screen.findByText("88")).toBeInTheDocument();
    expect(screen.getByText("91")).toBeInTheDocument();
    expect(screen.getByText("84")).toBeInTheDocument();
    expect(screen.getByText("136 WPM")).toBeInTheDocument();
    expect(screen.getByText(/总评分/)).toBeInTheDocument();
    expect(screen.getByText(/发音准确性/)).toBeInTheDocument();
    expect(screen.getByText(/流畅度/)).toBeInTheDocument();
    expect(screen.getByText(/语速/)).toBeInTheDocument();
  });

  it("shows feedback missing message when no feedback available", async () => {
    const services = createFeedbackServices(session);
    services.speaking.getFeedback = () => Promise.reject(new Error("No feedback"));
    renderWithProviders(<SpeakingFeedbackPage />, {
      path: "/speaking/:scenarioId/feedback",
      route: `/speaking/${defaultScenario.id}/feedback?sessionId=${session.id}`,
      services
    });

    expect(await screen.findByText(/评分数据暂未生成/)).toBeInTheDocument();
  });

  it("plays stored user recording audio during replay", async () => {
    const audioSources = [];
    vi.stubGlobal("Audio", class AudioMock {
      constructor(src) {
        audioSources.push(src);
        this.onended = null;
        this.onerror = null;
      }

      play() {
        return Promise.resolve();
      }

      pause() {}
    });

    const userAudioSession = {
      ...session,
      messages: [
        {
          id: 2,
          sender: "USER",
          content: "I want to review my meeting opening.",
          audioUrl: "speaking/42/2.webm",
          turnIndex: 1
        }
      ]
    };

    renderWithProviders(<SpeakingFeedbackPage />, {
      path: "/speaking/:scenarioId/feedback",
      route: `/speaking/${defaultScenario.id}/feedback?sessionId=${userAudioSession.id}`,
      services: createFeedbackServices(userAudioSession)
    });

    await userEvent.click(await screen.findByRole("button", { name: /查看回放/ }));

    await waitFor(() => {
      expect(audioSources[0]).toMatch(/\/uploads\/speaking\/42\/2\.webm$/);
    });
  });
});
