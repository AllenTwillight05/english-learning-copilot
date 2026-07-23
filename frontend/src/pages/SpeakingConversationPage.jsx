import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowLeftOutlined,
  AudioOutlined,
  PauseCircleOutlined,
  SendOutlined,
  SoundOutlined
} from "@ant-design/icons";
import { Button, Flex, Progress, Space, Tag } from "antd";
import { useNavigate, useParams } from "react-router-dom";
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

function toChatMessage(message) {
  const isUser = message.sender === "USER";
  return {
    id: message.id,
    role: isUser ? "learner" : "coach",
    text: message.content,
    audioUrl: message.audioUrl,
    instantTip: message.instantTip,
    turnIndex: message.turnIndex
  };
}

function getMessagePlaybackKey(message) {
  return message.id ?? `${message.role}-${message.turnIndex ?? "unknown"}-${message.text}`;
}

export function SpeakingConversationPage() {
  const navigate = useNavigate();
  const { scenarioId } = useParams();
  const { speaking } = useAppServices();
  const sessionRequestRef = useRef(null);
  const loader = useCallback(() => {
    if (!sessionRequestRef.current) {
      sessionRequestRef.current = speaking.createSession(scenarioId);
    }
    return sessionRequestRef.current;
  }, [speaking, scenarioId]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const [session, setSession] = useState(null);
  const [isSending, setIsSending] = useState(false);
  const [sendError, setSendError] = useState(null);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingCount, setRecordingCount] = useState(0);
  const [playingMessageKey, setPlayingMessageKey] = useState(null);

  const mediaRecorderRef = useRef(null);
  const audioChunksRef = useRef([]);
  const activePlaybackRef = useRef({ key: null, audio: null, utterance: null });
  const autoPlayedOpeningKeyRef = useRef(null);

  const activeSession = session ?? data;
  const scenario = activeSession?.scenario;
  const messages = useMemo(
    () => (activeSession?.messages ?? []).map(toChatMessage),
    [activeSession]
  );
  const lastTip = [...messages].reverse().find((message) => message.instantTip)?.instantTip;
  const notice = sendError
    ? {
        role: "alert",
        text: sendError.message || "消息发送失败，请稍后重试。"
      }
    : lastTip
      ? {
          role: "status",
          text: lastTip
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

    const utterance = speakText(message.text);
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
    if (!openingMessage?.audioUrl) {
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
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mediaRecorder = new MediaRecorder(stream, {
        mimeType: MediaRecorder.isTypeSupported("audio/webm;codecs=opus")
          ? "audio/webm;codecs=opus"
          : "audio/webm"
      });

      audioChunksRef.current = [];
      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.onstop = async () => {
        // Stop all tracks to release the microphone
        stream.getTracks().forEach((track) => track.stop());

        if (audioChunksRef.current.length === 0 || !activeSession?.id) {
          return;
        }

        const audioBlob = new Blob(audioChunksRef.current, { type: "audio/webm" });
        audioChunksRef.current = [];

        setIsSending(true);
        setSendError(null);
        try {
          const turn = await speaking.submitRecording(activeSession.id, audioBlob);
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

      mediaRecorderRef.current = mediaRecorder;
      mediaRecorder.start();
      setIsRecording(true);
    } catch (err) {
      console.error("Failed to start recording:", err);
    }
  }, [activeSession, speaking, startPlayback]);

  const stopRecording = useCallback(() => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state === "recording") {
      mediaRecorderRef.current.stop();
      mediaRecorderRef.current = null;
    }
    setIsRecording(false);
  }, []);

  const finishSession = useCallback(() => {
    if (!scenario || !activeSession?.id) {
      return;
    }
    stopPlayback();
    navigate(`/speaking/${scenario.id}/feedback?sessionId=${encodeURIComponent(activeSession.id)}`);
  }, [activeSession, navigate, scenario, stopPlayback]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopPlayback();
      if (mediaRecorderRef.current && mediaRecorderRef.current.state === "recording") {
        mediaRecorderRef.current.stop();
      }
    };
  }, [stopPlayback]);

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
                    : isSending
                      ? "正在处理语音..."
                      : "点击「开始录音」录入语音"}
                </span>
              </div>
              <div className="waveform-bars" aria-hidden="true">
                {Array.from({ length: 28 }).map((_, index) => (
                  <span key={index} style={{ "--bar": `${18 + ((index * 13) % 34)}px` }} />
                ))}
              </div>
              {isRecording ? <Progress percent={66} showInfo={false} status="active" /> : null}
              {isSending ? <Progress percent={100} showInfo={false} status="active" /> : null}
            </div>

            <Flex justify="space-between" gap={12} wrap="wrap">
              <Space wrap>
                <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(`/speaking/${scenario.id}`)}>
                  返回
                </Button>
                <Button
                  type="primary"
                  icon={<AudioOutlined />}
                  onClick={startRecording}
                  disabled={isRecording || isSending}
                >
                  开始录音
                </Button>
                <Button
                  icon={<PauseCircleOutlined />}
                  onClick={stopRecording}
                  disabled={!isRecording}
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
