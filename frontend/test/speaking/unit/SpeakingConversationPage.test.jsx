import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SpeakingConversationPage } from "../../../src/pages/SpeakingConversationPage";
import { speakingScenariosMock } from "../../../src/services/mockData";
import { renderWithProviders } from "../utils/renderWithProviders";

const defaultScenario = speakingScenariosMock[0];
const defaultAudioMock = globalThis.Audio;

function createConversationServices(messages) {
  return {
    speaking: {
      listScenarios: () => Promise.resolve([defaultScenario]),
      getScenario: () => Promise.resolve(defaultScenario),
      createSession: () => Promise.resolve({
        id: 1,
        scenario: defaultScenario,
        currentTurn: 1,
        messages
      }),
      submitRecording: () => Promise.reject(new Error("not used"))
    }
  };
}

describe("SpeakingConversationPage", () => {
  afterEach(() => {
    globalThis.Audio = defaultAudioMock;
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

  it("auto-plays the backend TTS audio for the opening agent message", async () => {
    const audioInstances = [];
    globalThis.Audio = class AudioMock {
      constructor(src) {
        this.src = src;
        this.onended = null;
        this.onerror = null;
        this.play = vi.fn().mockResolvedValue();
        this.pause = vi.fn();
        audioInstances.push(this);
      }
    };

    renderWithProviders(<SpeakingConversationPage />, {
      path: "/speaking/:scenarioId/conversation",
      route: `/speaking/${defaultScenario.id}/conversation`,
      services: createConversationServices([
        {
          id: 1,
          sender: "AGENT",
          content: "Generated opening from agent.",
          audioUrl: "/uploads/speaking/1/1.mp3",
          turnIndex: 0
        }
      ])
    });

    await screen.findByText("Generated opening from agent.");

    await waitFor(() => {
      expect(audioInstances).toHaveLength(1);
      expect(audioInstances[0].play).toHaveBeenCalledTimes(1);
    });
    expect(audioInstances[0].src).toMatch(/\/uploads\/speaking\/1\/1\.mp3$/);
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled();
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

  it("keeps the inline instant tip and shows a recording error separately", async () => {
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
    expect(container.querySelectorAll(".chat-instant-tip")).toHaveLength(1);
    expect(container.querySelectorAll(".speaking-alert")).toHaveLength(0);

    await userEvent.click(screen.getByRole("button", { name: /开始录音/ }));
    const stopButton = screen.getByRole("button", { name: /停止录音/ });
    await waitFor(() => {
      expect(stopButton).toBeEnabled();
    });
    await userEvent.click(stopButton);

    expect(await screen.findByText("没有识别到语音")).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText("Try using a complete sentence.")).toBeInTheDocument();
      expect(container.querySelectorAll(".chat-instant-tip")).toHaveLength(1);
      expect(container.querySelectorAll(".speaking-alert")).toHaveLength(1);
    });
  });

  it("releases the microphone and restores the controls when recorder creation fails", async () => {
    const stopTrack = vi.fn();
    const getUserMedia = vi.fn().mockResolvedValue({ getTracks: () => [{ stop: stopTrack }] });
    const originalMediaDevices = navigator.mediaDevices;
    const originalMediaRecorder = globalThis.MediaRecorder;
    Object.defineProperty(navigator, "mediaDevices", {
      configurable: true,
      value: { getUserMedia }
    });
    globalThis.MediaRecorder = class UnsupportedRecorder {
      static isTypeSupported() {
        return true;
      }

      constructor() {
        throw new DOMException("The requested MIME type is not supported.", "NotSupportedError");
      }
    };

    renderWithProviders(<SpeakingConversationPage />, {
      path: "/speaking/:scenarioId/conversation",
      route: `/speaking/${defaultScenario.id}/conversation`
    });

    const startButton = await screen.findByRole("button", { name: /开始录音/ });
    await userEvent.click(startButton);

    expect(await screen.findByText("The requested MIME type is not supported.")).toBeInTheDocument();
    expect(stopTrack).toHaveBeenCalledTimes(1);
    expect(startButton).toBeEnabled();
    expect(screen.getByRole("button", { name: /停止录音/ })).toBeDisabled();

    Object.defineProperty(navigator, "mediaDevices", {
      configurable: true,
      value: originalMediaDevices
    });
    globalThis.MediaRecorder = originalMediaRecorder;
  });

  it("disables other playback buttons while a speech-synthesis message is playing", async () => {
    renderWithProviders(<SpeakingConversationPage />, {
      path: "/speaking/:scenarioId/conversation",
      route: `/speaking/${defaultScenario.id}/conversation`,
      services: createConversationServices([
        {
          id: 1,
          sender: "AGENT",
          content: "Good morning. Could you briefly introduce today's agenda?",
          turnIndex: 0
        },
        {
          id: 2,
          sender: "USER",
          content: "I want to discuss the project timeline.",
          audioUrl: "speaking/1/2.webm",
          turnIndex: 1
        }
      ])
    });

    const playbackButtons = await screen.findAllByRole("button", { name: "播放此句音频" });
    await userEvent.click(playbackButtons[0]);

    expect(screen.getByRole("button", { name: "停止播放" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "播放此句音频" })).toBeDisabled();

    await userEvent.click(screen.getByRole("button", { name: "停止播放" }));

    const restoredButtons = screen.getAllByRole("button", { name: "播放此句音频" });
    expect(restoredButtons).toHaveLength(2);
    expect(restoredButtons[0]).toBeEnabled();
    expect(restoredButtons[1]).toBeEnabled();
  });

  it("stops the current audio recording when its playback button is clicked again", async () => {
    const audioInstances = [];
    globalThis.Audio = class AudioMock {
      constructor(src) {
        this.src = src;
        this.onended = null;
        this.onerror = null;
        this.play = vi.fn().mockResolvedValue();
        this.pause = vi.fn();
        audioInstances.push(this);
      }
    };

    renderWithProviders(<SpeakingConversationPage />, {
      path: "/speaking/:scenarioId/conversation",
      route: `/speaking/${defaultScenario.id}/conversation`,
      services: createConversationServices([
        {
          id: 1,
          sender: "USER",
          content: "First recording.",
          audioUrl: "speaking/1/1.webm",
          turnIndex: 1
        },
        {
          id: 2,
          sender: "USER",
          content: "Second recording.",
          audioUrl: "speaking/1/2.webm",
          turnIndex: 2
        }
      ])
    });

    const playbackButtons = await screen.findAllByRole("button", { name: "播放此句音频" });
    await userEvent.click(playbackButtons[0]);

    expect(audioInstances).toHaveLength(1);
    expect(audioInstances[0].play).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "播放此句音频" })).toBeDisabled();

    await userEvent.click(screen.getByRole("button", { name: "停止播放" }));

    expect(audioInstances[0].pause).toHaveBeenCalledTimes(1);
    expect(audioInstances[0].play).toHaveBeenCalledTimes(1);
    expect(screen.getAllByRole("button", { name: "播放此句音频" })).toHaveLength(2);
  });
});
