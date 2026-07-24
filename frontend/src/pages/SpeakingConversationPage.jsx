import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowLeftOutlined,
  AudioOutlined,
  PauseCircleOutlined,
  SendOutlined,
  SoundOutlined
} from "@ant-design/icons";
import { Button, Flex, Progress, Select, Space, Tag } from "antd";
import { useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";
import { toBackendAssetUrl } from "../services/assetUrls";

function speakText(text) {
  if (!window.speechSynthesis || !text) {
    return null;
  }
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  window.speechSynthesis.speak(utterance);
  return utterance;
}

function playAudioUrl(audioUrl, onEnd) {
  if (!audioUrl) {
    onEnd();
    return null;
  }
  const audio = new Audio(toBackendAssetUrl(audioUrl));
  audio.onended = onEnd;
  audio.onerror = onEnd;
  audio.play().catch(onEnd);
  return audio;
}

function getSupportedRecordingMimeType() {
  if (typeof MediaRecorder === "undefined") {
    return "";
  }

  const candidates = [
    "audio/webm;codecs=opus",
    "audio/webm",
    "audio/mp4",
    "audio/ogg;codecs=opus"
  ];

  if (typeof MediaRecorder.isTypeSupported !== "function") {
    return "";
  }

  return candidates.find((type) => MediaRecorder.isTypeSupported(type)) ?? "";
}

function getRecordingFileName(mimeType) {
  if (mimeType.includes("mp4")) {
    return "recording.m4a";
  }
  if (mimeType.includes("ogg")) {
    return "recording.ogg";
  }
  return "recording.webm";
}

function toRecordingErrorMessage(error) {
  switch (error?.name) {
    case "NotAllowedError":
    case "SecurityError":
      return "无法使用麦克风。请在浏览器地址栏中允许本网站使用麦克风后重试。";
    case "NotFoundError":
    case "DevicesNotFoundError":
      return "没有检测到可用麦克风。请连接或启用麦克风后重试。";
    case "NotReadableError":
    case "TrackStartError":
      return "麦克风正被其他程序占用。请关闭其他录音或通话程序后重试。";
    default:
      return error?.message || "录音无法启动，请检查浏览器麦克风权限后重试。";
  }
}

function toChatMessage(message) {
  const isUser = message.sender === "USER";
  return {
    id: message.id,
    role: isUser ? "learner" : "coach",
    text: message.content,
    spokenText: message.spokenText,
    audioUrl: message.audioUrl,
    autoPlay: message.autoPlay,
    instantTip: message.instantTip,
    turnIndex: message.turnIndex
  };
}

function getMessagePlaybackKey(message) {
  return message.id ?? `${message.role}-${message.turnIndex ?? "unknown"}-${message.text}`;
}

function getDefaultBackPath(scenarioId) {
  switch (scenarioId) {
    case "IELTS-P1-practice":
      return "/speaking/ielts/part1";
    case "IELTS-P2-practice":
      return "/speaking/ielts/part2";
    case "IELTS-P3-practice":
      return "/speaking/ielts/part3";
    case "IELTS-mock-test":
      return "/speaking/ielts";
    default:
      return `/speaking/${scenarioId}`;
  }
}

export function SpeakingConversationPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { scenarioId } = useParams();
  const { speaking } = useAppServices();
  const selectedTopic = searchParams.get("topic") || location.state?.selectedTopic || "";
  const backPath = location.state?.speakingBackPath || getDefaultBackPath(scenarioId);
  const sessionRequestRef = useRef(null);
  const loader = useCallback(() => {
    if (!sessionRequestRef.current) {
      sessionRequestRef.current = speaking.createSession({
        scenarioId,
        ...(selectedTopic ? { selectedTopic } : {})
      });
    }
    return sessionRequestRef.current;
  }, [speaking, scenarioId, selectedTopic]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const [session, setSession] = useState(null);
  const [isSending, setIsSending] = useState(false);
  const [sendError, setSendError] = useState(null);
  const [isRecording, setIsRecording] = useState(false);
  const [isPreparingRecording, setIsPreparingRecording] = useState(false);
  const [recordingCount, setRecordingCount] = useState(0);
  const [playingMessageKey, setPlayingMessageKey] = useState(null);
  const [audioInputs, setAudioInputs] = useState([]);
  const [selectedInputDeviceId, setSelectedInputDeviceId] = useState(undefined);
  const [inputLevel, setInputLevel] = useState(0);

  const mediaRecorderRef = useRef(null);
  const mediaStreamRef = useRef(null);
  const audioChunksRef = useRef([]);
  const recordingStartedAtRef = useRef(null);
  const activePlaybackRef = useRef({ key: null, audio: null, utterance: null });
  const autoPlayedOpeningKeyRef = useRef(null);
  const isMountedRef = useRef(true);
  const recordingStartCancelledRef = useRef(false);
  const audioContextRef = useRef(null);
  const audioSourceRef = useRef(null);
  const audioAnimationFrameRef = useRef(null);
  const inputMonitorAvailableRef = useRef(false);
  const recordedInputSignalRef = useRef(false);

  const activeSession = session ?? data;
  const scenario = activeSession?.scenario;
  const messages = useMemo(
    () => (activeSession?.messages ?? []).map(toChatMessage),
    [activeSession]
  );
  const notice = sendError
    ? {
        role: "alert",
        text: sendError.message || "消息发送失败，请稍后重试。"
      }
    : null;

  const clearPlaybackIfCurrent = useCallback((key) => {
    if (activePlaybackRef.current.key !== key) {
      return;
    }
    activePlaybackRef.current = { key: null, audio: null, utterance: null };
    setPlayingMessageKey(null);
  }, []);

  const stopPlayback = useCallback(() => {
    const current = activePlaybackRef.current;
    if (current.audio) {
      current.audio.onended = null;
      current.audio.onerror = null;
      current.audio.pause();
    }
    if (current.utterance && window.speechSynthesis) {
      current.utterance.onend = null;
      current.utterance.onerror = null;
      window.speechSynthesis.cancel();
    }
    activePlaybackRef.current = { key: null, audio: null, utterance: null };
    setPlayingMessageKey(null);
  }, []);

  const stopInputLevelMonitor = useCallback(() => {
    if (audioAnimationFrameRef.current !== null) {
      window.cancelAnimationFrame?.(audioAnimationFrameRef.current);
      audioAnimationFrameRef.current = null;
    }
    audioSourceRef.current?.disconnect?.();
    audioSourceRef.current = null;
    const audioContext = audioContextRef.current;
    audioContextRef.current = null;
    if (audioContext && audioContext.state !== "closed") {
      audioContext.close().catch(() => {});
    }
    if (isMountedRef.current) {
      setInputLevel(0);
    }
  }, []);

  const startInputLevelMonitor = useCallback((stream) => {
    inputMonitorAvailableRef.current = false;
    recordedInputSignalRef.current = false;
    const AudioContextConstructor = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextConstructor) {
      return;
    }

    try {
      const audioContext = new AudioContextConstructor();
      const analyser = audioContext.createAnalyser();
      const source = audioContext.createMediaStreamSource(stream);
      analyser.fftSize = 1024;
      analyser.smoothingTimeConstant = 0.7;
      source.connect(analyser);
      audioContextRef.current = audioContext;
      audioSourceRef.current = source;
      inputMonitorAvailableRef.current = true;
      audioContext.resume?.().catch(() => {});

      const floatSamples = new Float32Array(analyser.fftSize);
      const byteSamples = new Uint8Array(analyser.fftSize);
      const updateLevel = () => {
        let peak = 0;
        if (typeof analyser.getFloatTimeDomainData === "function") {
          analyser.getFloatTimeDomainData(floatSamples);
          for (const sample of floatSamples) {
            peak = Math.max(peak, Math.abs(sample));
          }
        } else {
          analyser.getByteTimeDomainData(byteSamples);
          for (const sample of byteSamples) {
            peak = Math.max(peak, Math.abs(sample - 128) / 128);
          }
        }

        // -54 dBFS: clearly above the near-silent recordings currently reaching ASR.
        if (peak >= 0.002) {
          recordedInputSignalRef.current = true;
        }
        if (isMountedRef.current) {
          setInputLevel(Math.min(100, Math.round(peak * 800)));
        }
        audioAnimationFrameRef.current = window.requestAnimationFrame(updateLevel);
      };
      updateLevel();
    } catch {
      inputMonitorAvailableRef.current = false;
    }
  }, []);

  const releaseRecordingStream = useCallback((stream = mediaStreamRef.current) => {
    if (mediaStreamRef.current === stream) {
      stopInputLevelMonitor();
    }
    stream?.getTracks?.().forEach((track) => track.stop());
    if (mediaStreamRef.current === stream) {
      mediaStreamRef.current = null;
    }
  }, [stopInputLevelMonitor]);

  const refreshAudioInputs = useCallback(async (activeDeviceId) => {
    if (!navigator.mediaDevices?.enumerateDevices) {
      return;
    }
    try {
      const inputs = (await navigator.mediaDevices.enumerateDevices())
        .filter((device) => device.kind === "audioinput")
        .map((device, index) => ({
          value: device.deviceId,
          label: device.label || `麦克风 ${index + 1}`
        }));
      if (!isMountedRef.current) {
        return;
      }
      setAudioInputs(inputs);
      setSelectedInputDeviceId((currentDeviceId) => {
        if (currentDeviceId && inputs.some((input) => input.value === currentDeviceId)) {
          return currentDeviceId;
        }
        if (activeDeviceId && inputs.some((input) => input.value === activeDeviceId)) {
          return activeDeviceId;
        }
        return inputs[0]?.value;
      });
    } catch {
      // Recording still works when the browser does not allow device enumeration.
    }
  }, []);

  const startPlayback = useCallback((message) => {
    const key = getMessagePlaybackKey(message);
    stopPlayback();

    const handleEnd = () => clearPlaybackIfCurrent(key);
    if (message.audioUrl) {
      const audio = playAudioUrl(message.audioUrl, handleEnd);
      if (!audio) {
        handleEnd();
        return;
      }
      activePlaybackRef.current = { key, audio, utterance: null };
      setPlayingMessageKey(key);
      return;
    }

    const utterance = speakText(message.spokenText || message.text);
    if (!utterance) {
      handleEnd();
      return;
    }
    utterance.onend = handleEnd;
    utterance.onerror = handleEnd;
    activePlaybackRef.current = { key, audio: null, utterance };
    setPlayingMessageKey(key);
  }, [clearPlaybackIfCurrent, stopPlayback]);

  const playMessageAudio = useCallback((message) => {
    const key = getMessagePlaybackKey(message);
    if (activePlaybackRef.current.key === key) {
      stopPlayback();
      return;
    }
    startPlayback(message);
  }, [startPlayback, stopPlayback]);

  useEffect(() => {
    const openingMessage = messages.find(
      (message) => message.role === "coach" && message.turnIndex === 0
    );
    if (!openingMessage?.autoPlay && !openingMessage?.audioUrl) {
      return;
    }

    const openingKey = getMessagePlaybackKey(openingMessage);
    if (autoPlayedOpeningKeyRef.current === openingKey) {
      return;
    }

    autoPlayedOpeningKeyRef.current = openingKey;
    startPlayback(openingMessage);
  }, [messages, startPlayback]);

  const startRecording = useCallback(async () => {
    if (isRecording || isPreparingRecording || isSending) {
      return;
    }

    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") {
      setSendError(new Error("当前浏览器不支持网页录音。请使用最新版 Chrome、Edge 或 Safari。"));
      return;
    }

    let stream = null;
    recordingStartCancelledRef.current = false;
    setIsPreparingRecording(true);
    setSendError(null);
    try {
      const audioConstraints = selectedInputDeviceId
        ? { deviceId: { exact: selectedInputDeviceId } }
        : true;
      stream = await navigator.mediaDevices.getUserMedia({ audio: audioConstraints });
      if (!isMountedRef.current || recordingStartCancelledRef.current) {
        releaseRecordingStream(stream);
        return;
      }

      mediaStreamRef.current = stream;
      const activeDeviceId = stream.getAudioTracks?.()[0]?.getSettings?.().deviceId;
      refreshAudioInputs(activeDeviceId);
      startInputLevelMonitor(stream);
      const mimeType = getSupportedRecordingMimeType();
      const mediaRecorder = mimeType
        ? new MediaRecorder(stream, { mimeType })
        : new MediaRecorder(stream);
      const sessionId = activeSession?.id;

      audioChunksRef.current = [];
      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.onstop = async () => {
        const durationMs = recordingStartedAtRef.current ? Date.now() - recordingStartedAtRef.current : undefined;
        const noInputSignal = inputMonitorAvailableRef.current
          && !recordedInputSignalRef.current
          && (durationMs ?? 0) >= 800;
        releaseRecordingStream(stream);
        mediaRecorderRef.current = null;
        recordingStartedAtRef.current = null;
        setIsRecording(false);

        if (audioChunksRef.current.length === 0) {
          setSendError(new Error("没有录到有效语音，请靠近麦克风后再试一次。"));
          setIsSending(false);
          return;
        }
        if (noInputSignal) {
          audioChunksRef.current = [];
          setSendError(new Error("未检测到有效麦克风声音。请在下方选择正确的录音设备，或检查系统输入音量后重试。"));
          setIsSending(false);
          return;
        }
        if (!sessionId) {
          setSendError(new Error("当前会话已失效，请返回后重新进入练习。"));
          setIsSending(false);
          return;
        }

        const recordingMimeType = mediaRecorder.mimeType || mimeType || audioChunksRef.current[0]?.type || "audio/webm";
        const audioBlob = new Blob(audioChunksRef.current, { type: recordingMimeType });
        audioChunksRef.current = [];

        try {
          const turn = await speaking.submitRecording(
            sessionId,
            audioBlob,
            durationMs,
            getRecordingFileName(recordingMimeType)
          );
          if (!isMountedRef.current) {
            return;
          }
          setSession(turn.session);
          setRecordingCount((current) => current + 1);

          // Auto-play agent audio if available, else fallback to browser TTS
          const agentMsg = turn.agentMessage;
          if (agentMsg?.content) {
            startPlayback(toChatMessage(agentMsg));
          }
        } catch (err) {
          setSendError(err);
        } finally {
          setIsSending(false);
        }
      };

      mediaRecorder.onerror = (event) => {
        releaseRecordingStream(stream);
        mediaRecorderRef.current = null;
        recordingStartedAtRef.current = null;
        setIsRecording(false);
        setIsSending(false);
        setSendError(new Error(event.error?.message || "录音过程中发生错误，请重试。"));
      };

      mediaRecorderRef.current = mediaRecorder;
      recordingStartedAtRef.current = Date.now();
      mediaRecorder.start();
      setIsRecording(true);
    } catch (err) {
      console.error("Failed to start recording:", err);
      releaseRecordingStream(stream);
      mediaRecorderRef.current = null;
      recordingStartedAtRef.current = null;
      setIsRecording(false);
      setSendError(new Error(toRecordingErrorMessage(err)));
    } finally {
      if (isMountedRef.current) {
        setIsPreparingRecording(false);
      }
    }
  }, [
    activeSession,
    isPreparingRecording,
    isRecording,
    isSending,
    refreshAudioInputs,
    releaseRecordingStream,
    selectedInputDeviceId,
    speaking,
    startInputLevelMonitor,
    startPlayback
  ]);

  const stopRecording = useCallback(() => {
    const mediaRecorder = mediaRecorderRef.current;
    if (!mediaRecorder) {
      recordingStartCancelledRef.current = true;
      releaseRecordingStream();
      setIsRecording(false);
      setIsPreparingRecording(false);
      return;
    }
    if (mediaRecorder.state === "inactive") {
      releaseRecordingStream();
      setIsRecording(false);
      return;
    }

    setIsRecording(false);
    setIsSending(true);
    try {
      mediaRecorder.stop();
    } catch (err) {
      releaseRecordingStream();
      mediaRecorderRef.current = null;
      recordingStartedAtRef.current = null;
      setIsSending(false);
      setSendError(new Error(toRecordingErrorMessage(err)));
    }
  }, [releaseRecordingStream]);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const cancelPendingRecording = useCallback((updateUi = true) => {
    recordingStartCancelledRef.current = true;
    const mediaRecorder = mediaRecorderRef.current;
    if (mediaRecorder?.state === "recording") {
      mediaRecorder.onstop = null;
      mediaRecorder.stop();
    }
    mediaRecorderRef.current = null;
    recordingStartedAtRef.current = null;
    audioChunksRef.current = [];
    releaseRecordingStream();
    if (updateUi) {
      setIsRecording(false);
      setIsPreparingRecording(false);
    }
  }, [releaseRecordingStream]);

  const finishSession = useCallback(() => {
    if (!scenario || !activeSession?.id) {
      return;
    }
    stopPlayback();
    cancelPendingRecording();
    navigate(`/speaking/${scenario.id}/feedback?sessionId=${encodeURIComponent(activeSession.id)}`);
  }, [activeSession, cancelPendingRecording, navigate, scenario, stopPlayback]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopPlayback();
      cancelPendingRecording(false);
    };
  }, [cancelPendingRecording, stopPlayback]);

  return (
    <AsyncPage loading={loading} error={error}>
      {activeSession && scenario ? (
        <div className="page-stack">
          <section className="glass-panel speaking-session-panel">
            <PageSectionHeader
              eyebrow=""
              title={scenario.title}
              description=""
              extra={
                <Space wrap>
                  <Tag bordered={false} className="soft-tag">
                    当前为第 {activeSession.currentTurn ?? recordingCount} 轮
                  </Tag>
                  <Tag bordered={false} className="soft-tag soft-tag--dark">
                    {scenario.level}
                  </Tag>
                  {selectedTopic ? (
                    <Tag bordered={false} className="soft-tag">
                      {selectedTopic}
                    </Tag>
                  ) : null}
                </Space>
              }
            />

            <div className="chat-window chat-window--page">
              {messages.map((message, index) => {
                const playbackKey = getMessagePlaybackKey(message);
                const isThisPlaying = playingMessageKey === playbackKey;
                const isOtherPlaying = playingMessageKey !== null && playingMessageKey !== playbackKey;
                return (
                  <div
                    className={`chat-bubble-row chat-bubble-row--${message.role}`}
                    key={message.id ?? `${message.role}-${index}`}
                  >
                    <div className={`chat-message-stack chat-message-stack--${message.role}`}>
                      <div className={`chat-bubble chat-bubble--${message.role}`}>
                        <span>{message.text}</span>
                        <Button
                          aria-label={isThisPlaying ? "停止播放" : "播放此句音频"}
                          className={`chat-audio-button ${isThisPlaying ? "chat-audio-button--playing" : ""}`}
                          icon={<SoundOutlined />}
                          shape="circle"
                          size="small"
                          disabled={isOtherPlaying}
                          onClick={() => playMessageAudio(message)}
                        />
                      </div>
                      {message.instantTip ? (
                        <div className="chat-instant-tip" role="note">
                          {message.instantTip}
                        </div>
                      ) : null}
                    </div>
                  </div>
                );
              })}
            </div>

            {notice ? (
              <div className="speaking-alert" role={notice.role}>
                {notice.text}
              </div>
            ) : null}

            <div className={`recorder-strip ${isRecording ? "recorder-strip--recording" : ""}`}>
              <div className="recorder-strip__label">
                <SoundOutlined />
                <span>
                  {isRecording
                    ? "正在录音中..."
                    : isPreparingRecording
                      ? "正在连接麦克风..."
                    : isSending
                      ? "正在处理语音..."
                      : "点击「开始录音」录入语音"}
                </span>
              </div>
              <div className="waveform-bars" aria-hidden="true">
                {Array.from({ length: 28 }).map((_, index) => (
                  <span
                    key={index}
                    style={{
                      "--bar": `${isRecording ? Math.max(4, 4 + inputLevel * (0.24 + ((index % 7) * 0.04))) : 4}px`
                    }}
                  />
                ))}
              </div>
              {isRecording ? <div className="recorder-strip__level">输入电平 {inputLevel}%</div> : null}
              {isRecording ? <Progress percent={66} showInfo={false} status="active" /> : null}
              {isSending ? <Progress percent={100} showInfo={false} status="active" /> : null}
            </div>

            {audioInputs.length > 1 ? (
              <div className="recorder-input-select">
                <span>录音设备</span>
                <Select
                  value={selectedInputDeviceId}
                  options={audioInputs}
                  disabled={isRecording || isPreparingRecording || isSending}
                  onChange={setSelectedInputDeviceId}
                />
              </div>
            ) : null}

            <Flex justify="space-between" gap={12} wrap="wrap">
              <Space wrap>
                <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(backPath || `/speaking/${scenario.id}`)}>
                  返回
                </Button>
                <Button
                  type="primary"
                  icon={<AudioOutlined />}
                  onClick={startRecording}
                  disabled={isRecording || isPreparingRecording || isSending}
                >
                  开始录音
                </Button>
                <Button
                  icon={<PauseCircleOutlined />}
                  onClick={stopRecording}
                  disabled={!isRecording && !isPreparingRecording}
                >
                  停止录音
                </Button>
              </Space>
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={finishSession}
                disabled={messages.length <= 1}
              >
                交卷
              </Button>
            </Flex>
          </section>
        </div>
      ) : null}
    </AsyncPage>
  );
}
