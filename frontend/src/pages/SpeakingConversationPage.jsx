import { useCallback, useMemo, useRef, useState } from "react";
import {
  ArrowLeftOutlined,
  AudioOutlined,
  PauseCircleOutlined,
  SendOutlined,
  SoundOutlined
} from "@ant-design/icons";
import { Button, Flex, Input, Progress, Space, Tag, Typography } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Text } = Typography;
const { TextArea } = Input;

function speakText(text) {
  if (!window.speechSynthesis || !text) {
    return;
  }
  window.speechSynthesis.cancel();
  window.speechSynthesis.speak(new SpeechSynthesisUtterance(text));
}

function toChatMessage(message) {
  const isUser = message.sender === "USER";
  return {
    id: message.id,
    role: isUser ? "learner" : "coach",
    text: message.content,
    instantTip: message.instantTip
  };
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
  const [draftMessage, setDraftMessage] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [sendError, setSendError] = useState(null);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingCount, setRecordingCount] = useState(0);

  const activeSession = session ?? data;
  const scenario = activeSession?.scenario;
  const messages = useMemo(
    () => (activeSession?.messages ?? []).map(toChatMessage),
    [activeSession]
  );
  const lastTip = [...messages].reverse().find((message) => message.instantTip)?.instantTip;
  const canSend = draftMessage.trim().length > 0 && !isSending && Boolean(activeSession?.id);

  const playMessageAudio = useCallback((message) => {
    speakText(message.text);
  }, []);

  const startRecording = useCallback(() => {
    setIsRecording(true);
    setRecordingCount((current) => current + 1);
  }, []);

  const stopRecording = useCallback(() => {
    setIsRecording(false);
  }, []);

  const sendMessage = useCallback(async () => {
    if (!canSend) {
      return;
    }

    const content = draftMessage.trim();
    setDraftMessage("");
    setIsSending(true);
    setSendError(null);

    try {
      const turn = await speaking.addMessage(activeSession.id, content);
      setSession(turn.session);
    } catch (error) {
      setDraftMessage(content);
      setSendError(error);
    } finally {
      setIsSending(false);
    }
  }, [activeSession, canSend, draftMessage, speaking]);

  const finishSession = useCallback(() => {
    if (!scenario || !activeSession?.id) {
      return;
    }

    navigate(`/speaking/${scenario.id}/feedback?sessionId=${encodeURIComponent(activeSession.id)}`);
  }, [activeSession, navigate, scenario]);

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
              {messages.map((message, index) => (
                <div
                  className={`chat-bubble-row chat-bubble-row--${message.role}`}
                  key={message.id ?? `${message.role}-${index}`}
                >
                  <div className={`chat-bubble chat-bubble--${message.role}`}>
                    <span>{message.text}</span>
                    <Button
                      aria-label="播放此句音频"
                      className="chat-audio-button"
                      icon={<SoundOutlined />}
                      shape="circle"
                      size="small"
                      onClick={() => playMessageAudio(message)}
                    />
                  </div>
                </div>
              ))}
            </div>

            {lastTip ? (
              <div className="speaking-alert" role="status">
                {lastTip}
              </div>
            ) : null}
            {sendError ? (
              <div className="speaking-alert" role="alert">
                {sendError.message || "消息发送失败，请稍后重试。"}
              </div>
            ) : null}

            <div className={`recorder-strip ${isRecording ? "recorder-strip--recording" : ""}`}>
              <div className="recorder-strip__label">
                <SoundOutlined />
                <span>{isRecording ? "正在录音" : "录音仍未实现，可用打字检查交互是否正常"}</span>
              </div>
              <div className="waveform-bars" aria-hidden="true">
                {Array.from({ length: 28 }).map((_, index) => (
                  <span key={index} style={{ "--bar": `${18 + ((index * 13) % 34)}px` }} />
                ))}
              </div>
              {isRecording ? <Progress percent={66} showInfo={false} status="active" /> : null}
            </div>

            <div className="speaking-message-composer">
              <TextArea
                autoSize={{ minRows: 2, maxRows: 4 }}
                maxLength={4000}
                placeholder="输入你的英文回答..."
                value={draftMessage}
                onChange={(event) => setDraftMessage(event.target.value)}
                onPressEnter={(event) => {
                  if (!event.shiftKey) {
                    event.preventDefault();
                    sendMessage();
                  }
                }}
              />
              <Button
                type="primary"
                icon={<SendOutlined />}
                loading={isSending}
                disabled={!canSend}
                onClick={sendMessage}
              >
                发送
              </Button>
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
                  disabled={isRecording}
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
            <Text type="secondary">当前会话消息已接入后端 session。</Text>
          </section>
        </div>
      ) : null}
    </AsyncPage>
  );
}
