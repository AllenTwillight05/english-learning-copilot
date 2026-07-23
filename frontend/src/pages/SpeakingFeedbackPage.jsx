import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowLeftOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SoundOutlined,
  TrophyOutlined
} from "@ant-design/icons";
import { Button, Modal, Space, Typography } from "antd";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { SpeakingTurnFeedbackCard } from "../components/Speaking/SpeakingTurnFeedbackCard";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";
import { toBackendAssetUrl } from "../services/assetUrls";

const { Text, Title } = Typography;

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
    let session;
    if (sessionId) {
      session = await speaking.getSession(sessionId);
    } else {
      const history = await speaking.listHistory();
      session = findLatestScenarioSession(history, scenarioId);
    }

    let feedback = null;
    if (session?.id) {
      try {
        feedback = await speaking.getFeedback(session.id);
      } catch (err) {
        console.error("Failed to fetch speaking feedback:", err);
      }
    }

    return { scenario, session, feedback };
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
  const feedback = data?.feedback;
  const replayMessages = useMemo(
    () => (session?.messages ?? []).map(toReplayMessage),
    [session]
  );

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
            const audio = new Audio(toBackendAssetUrl(message.audioUrl));
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
        setActiveTurn(0);
      }
    },
    [replayMessages, stopReplay]
  );

  useEffect(() => () => stopReplay(), [stopReplay]);

  const hasFeedback = feedback && typeof feedback.totalScore === "number";

  return (
    <AsyncPage loading={loading} error={error}>
      {scenario ? (
        <div className="page-stack">
          {/* ---- Scoring section ---- */}
          <section className="glass-panel">
            <PageSectionHeader
              eyebrow=""
              title="评分结果"
              description=""
            />

            {hasFeedback ? (
              <>
                <div className="feedback-layout">
                  {/* Total score card */}
                  <div className="glass-panel score-card">
                    <div className="score-card__main">
                      <Text type="secondary" className="score-card__label">总评分</Text>
                      <div className="score-card__score-row">
                        <Title level={2} className="score-card__value" style={{ margin: 0 }}>
                          {feedback.totalScore}
                        </Title>
                        <Text type="secondary" className="score-card__unit">/ 100</Text>
                      </div>
                    </div>
                    <TrophyOutlined className="score-card__icon" />
                  </div>

                  {/* Sub-metrics */}
                  <div className="feedback-metrics feedback-metrics--summary">
                    <div className="glass-panel feedback-metric-item">
                      <Text type="secondary" className="metric-label">发音准确性</Text>
                      <div className="metric-score-row">
                        <Title level={4} className="metric-value" style={{ margin: 0 }}>
                          {feedback.pronunciation}
                        </Title>
                        <Text type="secondary" className="metric-unit">/ 100</Text>
                      </div>
                    </div>
                    <div className="glass-panel feedback-metric-item">
                      <Text type="secondary" className="metric-label">流畅度</Text>
                      <div className="metric-score-row">
                        <Title level={4} className="metric-value" style={{ margin: 0 }}>
                          {feedback.fluency}
                        </Title>
                        <Text type="secondary" className="metric-unit">/ 100</Text>
                      </div>
                    </div>
                    <div className="glass-panel feedback-metric-item">
                      <Text type="secondary" className="metric-label">完整度</Text>
                      <div className="metric-score-row">
                        <Title level={4} className="metric-value" style={{ margin: 0 }}>
                          {feedback.integrity}
                        </Title>
                        <Text type="secondary" className="metric-unit">/ 100</Text>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Issue sentences */}
                <div className="feedback-list glass-panel" style={{ marginBottom: 12 }}>
                  <Text strong className="panel-title">问题句子</Text>
                  <ul className="feedback-issue-list">
                    {(feedback.issueSentences?.length ? feedback.issueSentences : ["无"]).map((sentence, idx) => (
                      <li key={idx} className="feedback-issue-item">
                        <span className="feedback-bullet" aria-hidden="true" />
                        <Text>{sentence}</Text>
                      </li>
                    ))}
                  </ul>
                </div>

                {/* Suggestions */}
                {feedback.suggestions && feedback.suggestions.length > 0 && (
                  <div className="feedback-list glass-panel" style={{ marginBottom: 12 }}>
                    <Text strong className="panel-title">改进建议</Text>
                    <ul className="feedback-suggestion-list">
                      {feedback.suggestions.map((suggestion, idx) => (
                        <li key={idx} className="feedback-suggestion-item">
                          <span className="feedback-bullet" aria-hidden="true" />
                          <Text>{suggestion}</Text>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {feedback.turns && feedback.turns.length > 0 && (
                  <div className="feedback-turns">
                    <Text strong className="panel-title">每轮发音明细</Text>
                    <div className="feedback-turn-list">
                      {feedback.turns.map((turn) => (
                        <SpeakingTurnFeedbackCard turn={turn} key={turn.turnIndex} />
                      ))}
                    </div>
                  </div>
                )}
              </>
            ) : session ? (
              <div className="speaking-alert" role="alert" style={{ marginBottom: 16 }}>
                评分数据暂未生成。
              </div>
            ) : null}

            {!session ? (
              <div className="speaking-alert" role="alert">
                暂无历史会话记录。请先进入会话完成一次文本练习。
              </div>
            ) : null}

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

          {/* ---- Replay modal ---- */}
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
