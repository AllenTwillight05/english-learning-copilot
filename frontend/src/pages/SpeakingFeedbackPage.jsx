import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowLeftOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SoundOutlined
} from "@ant-design/icons";
import { Button, Modal, Space, Typography } from "antd";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Text, Paragraph } = Typography;

function speakText(text, onEnd) {
  if (!window.speechSynthesis || !text) {
    onEnd();
    return null;
  }
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.onend = onEnd;
  utterance.onerror = onEnd;
  window.speechSynthesis.speak(utterance);
  return utterance;
}

function toReplayMessage(message) {
  const isUser = message.sender === "USER";
  return {
    id: message.id,
    role: isUser ? "learner" : "coach",
    text: message.content,
    audioUrl: message.audioUrl,
    instantTip: message.instantTip
  };
}

function findLatestScenarioSession(history, scenarioId) {
  return history.find((session) => session.scenario?.id === scenarioId) ?? null;
}

export function SpeakingFeedbackPage() {
  const navigate = useNavigate();
  const { scenarioId } = useParams();
  const [searchParams] = useSearchParams();
  const { speaking } = useAppServices();
  const sessionId = searchParams.get("sessionId");
  const loader = useCallback(async () => {
    const scenario = await speaking.getScenario(scenarioId);
    if (sessionId) {
      const session = await speaking.getSession(sessionId);
      return { scenario, session };
    }

    const history = await speaking.listHistory();
    return {
      scenario,
      session: findLatestScenarioSession(history, scenarioId)
    };
  }, [scenarioId, sessionId, speaking]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const [isReplayOpen, setIsReplayOpen] = useState(false);
  const [activeTurn, setActiveTurn] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const currentAudioRef = useRef(null);
  const stopReplayRef = useRef(false);
  const replayRunRef = useRef(0);

  const scenario = data?.scenario;
  const session = data?.session;
  const replayMessages = useMemo(
    () => (session?.messages ?? []).map(toReplayMessage),
    [session]
  );

  const replayTurns = useMemo(() => {
    const turns = [];
    for (let index = 0; index < replayMessages.length; index += 2) {
      turns.push(replayMessages.slice(index, index + 2));
    }
    return turns;
  }, [replayMessages]);

  const stopReplay = useCallback(() => {
    stopReplayRef.current = true;
    replayRunRef.current += 1;
    currentAudioRef.current?.pause();
    currentAudioRef.current = null;
    window.speechSynthesis?.cancel();
    setIsPlaying(false);
  }, []);

  const playFromTurn = useCallback(
    async (turnIndex = 0) => {
      if (!replayMessages.length) {
        return;
      }
      stopReplay();
      stopReplayRef.current = false;
      const runId = replayRunRef.current;
      setIsPlaying(true);
      setActiveTurn(turnIndex);

      for (let index = turnIndex * 2; index < replayMessages.length; index += 1) {
        if (stopReplayRef.current || runId !== replayRunRef.current) {
          return;
        }
        setActiveTurn(Math.floor(index / 2));
        const message = replayMessages[index];
        await new Promise((resolve) => {
          if (message.audioUrl) {
            const audio = new Audio(message.audioUrl);
            currentAudioRef.current = audio;
            audio.onended = resolve;
            audio.onerror = resolve;
            audio.play().catch(resolve);
          } else {
            speakText(message.text, resolve);
          }
        });
      }
      if (!stopReplayRef.current && runId === replayRunRef.current) {
        setIsPlaying(false);
      }
    },
    [replayMessages, stopReplay]
  );

  useEffect(() => () => stopReplay(), [stopReplay]);

  return (
    <AsyncPage loading={loading} error={error}>
      {scenario ? (
        <div className="page-stack">
          <section className="glass-panel">
            <PageSectionHeader
              eyebrow=""
              title="评分结果"
              description=""
            />
            {session ? (
              <div className="feedback-detail-grid">
                <div className="feedback-list">
                  <Text className="panel-title">本次文本回放</Text>
                  <Paragraph>
                    已从后端读取最新会话记录，共 {replayMessages.length} 条消息。语音录制、音频文件和发音评分接入后，可继续挂载到这些消息记录上。
                  </Paragraph>
                </div>
                <div className="feedback-list">
                  <Text className="panel-title">会话状态</Text>
                  <Paragraph>当前状态：{session.status}</Paragraph>
                  <Paragraph>完成轮次：{session.currentTurn} / {session.targetTurns}</Paragraph>
                </div>
              </div>
            ) : (
              <div className="speaking-alert" role="alert">
                暂无历史会话记录。请先进入会话完成一次文本练习。
              </div>
            )}

            <Space wrap>
              <Button onClick={() => navigate("/speaking")}>退出</Button>
              <Button
                icon={<SoundOutlined />}
                disabled={replayMessages.length === 0}
                onClick={() => {
                  setIsReplayOpen(true);
                  playFromTurn(0);
                }}
              >
                查看回放
              </Button>
              <Button
                type="primary"
                icon={<ReloadOutlined />}
                onClick={() => navigate(`/speaking/${scenario.id}/conversation`)}
              >
                再练一次
              </Button>
            </Space>
          </section>
          <Modal
            title="会话回放"
            open={isReplayOpen}
            footer={null}
            width={760}
            afterOpenChange={(open) => {
              if (!open) {
                stopReplay();
              }
            }}
            onCancel={() => setIsReplayOpen(false)}
          >
            <div className="replay-modal-body">
              <div className="chat-window replay-chat-window">
                {replayMessages.map((message, index) => (
                  <div
                    className={`chat-bubble-row chat-bubble-row--${message.role}`}
                    key={message.id ?? `${message.role}-${index}`}
                  >
                    <div className={`chat-bubble chat-bubble--${message.role}`}>{message.text}</div>
                  </div>
                ))}
              </div>
              <div className="replay-controls">
                <Button
                  icon={isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                  onClick={() => {
                    if (isPlaying) {
                      stopReplay();
                    } else {
                      playFromTurn(activeTurn);
                    }
                  }}
                >
                  {isPlaying ? "暂停" : "开始"}
                </Button>
                <div className="replay-segments" role="group" aria-label="回放进度">
                  {replayTurns.map((_, index) => (
                    <button
                      className={index === activeTurn ? "replay-segment replay-segment--active" : "replay-segment"}
                      key={index}
                      type="button"
                      onClick={() => playFromTurn(index)}
                    >
                      第 {index + 1} 轮
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </Modal>
        </div>
      ) : data ? (
        <section className="glass-panel">
          <PageSectionHeader
            eyebrow="Scenario Missing"
            title="没有找到这个情景"
            description="请返回口语页重新选择一个情景模块。"
          />
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/speaking")}>
            退出
          </Button>
        </section>
      ) : null}
    </AsyncPage>
  );
}
