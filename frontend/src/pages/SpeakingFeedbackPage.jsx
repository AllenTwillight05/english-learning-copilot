import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SoundOutlined
} from "@ant-design/icons";
import { Button, Modal, Space, Statistic, Typography } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Text, Title, Paragraph } = Typography;
const speakingHistoryKey = (scenarioId) => `speaking-history:${scenarioId}`;

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

export function SpeakingFeedbackPage() {
  const navigate = useNavigate();
  const { scenarioId } = useParams();
  const { speaking } = useAppServices();
  const loader = useCallback(() => speaking.getCatalog(), [speaking]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const [isReplayOpen, setIsReplayOpen] = useState(false);
  const [activeTurn, setActiveTurn] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const currentAudioRef = useRef(null);
  const stopReplayRef = useRef(false);
  const replayRunRef = useRef(0);

  const scenario = useMemo(
    () => data?.scenarios.find((item) => item.id === scenarioId),
    [data, scenarioId]
  );

  const replayMessages = useMemo(() => {
    if (!scenario) {
      return [];
    }
    const savedReplay = window.localStorage.getItem(speakingHistoryKey(scenario.id));
    if (savedReplay) {
      try {
        return JSON.parse(savedReplay).messages ?? scenario.prompts;
      } catch {
        return scenario.prompts;
      }
    }
    return scenario.prompts;
  }, [scenario]);

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
            <div className="feedback-layout">
              <div className="score-card">
                <Statistic
                  title={<Text className="panel-title">总分</Text>}
                  value={scenario.feedback.totalScore}
                  suffix="/ 100"
                />
                <CheckCircleOutlined className="score-card__icon" />
              </div>
              <div className="feedback-metrics">
                <div className="metric-chip">
                  <Text className="panel-title">发音准确性</Text>
                  <Title level={4}>{scenario.feedback.pronunciation}%</Title>
                </div>
                <div className="metric-chip">
                  <Text className="panel-title">流畅度</Text>
                  <Title level={4}>{scenario.feedback.fluency}%</Title>
                </div>
                <div className="metric-chip">
                  <Text className="panel-title">语速</Text>
                  <Title level={4}>{scenario.feedback.speed}</Title>
                </div>
              </div>
            </div>

            <div className="feedback-detail-grid">
              <div className="feedback-list">
                <Text className="panel-title">问题句子合集</Text>
                {scenario.feedback.issueSentences.map((sentence) => (
                  <Paragraph key={sentence}>{sentence}</Paragraph>
                ))}
              </div>
              <div className="feedback-list">
                <Text className="panel-title">改进建议</Text>
                {scenario.feedback.suggestions.map((suggestion) => (
                  <Paragraph key={suggestion}>{suggestion}</Paragraph>
                ))}
              </div>
            </div>

            <Space wrap>
              <Button onClick={() => navigate("/speaking")}>退出</Button>
              <Button icon={<SoundOutlined />} onClick={() => setIsReplayOpen(true)}>
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
              if (open) {
                playFromTurn(0);
              } else {
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
                    key={`${message.role}-${index}`}
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
                <div className="replay-segments" role="tablist" aria-label="回放进度">
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
