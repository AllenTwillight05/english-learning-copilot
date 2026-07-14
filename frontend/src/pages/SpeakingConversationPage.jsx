import { useCallback, useMemo, useRef, useState } from "react";
import {
  ArrowLeftOutlined,
  AudioOutlined,
  PauseCircleOutlined,
  SendOutlined,
  SoundOutlined
} from "@ant-design/icons";
import { Button, Flex, Progress, Space, Tag, Typography } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Text, Title } = Typography;
const speakingHistoryKey = (scenarioId) => `speaking-history:${scenarioId}`;

function speakText(text) {
  if (!window.speechSynthesis || !text) {
    return;
  }
  window.speechSynthesis.cancel();
  window.speechSynthesis.speak(new SpeechSynthesisUtterance(text));
}

export function SpeakingConversationPage() {
  const navigate = useNavigate();
  const { scenarioId } = useParams();
  const { speaking } = useAppServices();
  const loader = useCallback(() => speaking.getScenario(scenarioId), [speaking, scenarioId]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingCount, setRecordingCount] = useState(0);
  const [recordedAudio, setRecordedAudio] = useState({});
  const mediaRecorderRef = useRef(null);
  const audioChunksRef = useRef([]);
  const activeLearnerIndexRef = useRef(0);
  const streamRef = useRef(null);

  const scenario = data;
  const prompts = scenario?.prompts ?? [];
  const hasPromptScript = prompts.length > 0;

  const learnerPromptIndexes = useMemo(
    () =>
      prompts.reduce((indexes, message, index) => {
        if (message.role === "learner") {
          indexes.push(index);
        }
        return indexes;
      }, []),
    [prompts]
  );
  const canRecord = hasPromptScript && learnerPromptIndexes.length > 0;

  const startRecording = useCallback(async () => {
    if (!canRecord) {
      return;
    }

    setIsRecording(true);
    setRecordingCount((current) => current + 1);
    const promptIndex = learnerPromptIndexes[activeLearnerIndexRef.current % learnerPromptIndexes.length];
    activeLearnerIndexRef.current += 1;

    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder || promptIndex === undefined) {
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      audioChunksRef.current = [];
      const recorder = new MediaRecorder(stream);
      mediaRecorderRef.current = recorder;
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };
      recorder.onstop = () => {
        const blob = new Blob(audioChunksRef.current, { type: recorder.mimeType || "audio/webm" });
        const reader = new FileReader();
        reader.onloadend = () => {
          setRecordedAudio((current) => ({ ...current, [promptIndex]: reader.result }));
        };
        reader.readAsDataURL(blob);
        stream.getTracks().forEach((track) => track.stop());
        streamRef.current = null;
      };
      recorder.start();
    } catch {
      mediaRecorderRef.current = null;
      streamRef.current = null;
    }
  }, [canRecord, learnerPromptIndexes]);

  const stopRecording = useCallback(() => {
    setIsRecording(false);
    if (mediaRecorderRef.current?.state === "recording") {
      mediaRecorderRef.current.stop();
      return;
    }
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
  }, []);

  const playMessageAudio = useCallback(
    (message, index) => {
      const audioUrl = message.audioUrl || recordedAudio[index];
      if (audioUrl) {
        new Audio(audioUrl).play();
        return;
      }
      speakText(message.text);
    },
    [recordedAudio]
  );

  const submitSession = useCallback(() => {
    if (!scenario || !canRecord) {
      return;
    }

    const replay = {
      savedAt: new Date().toISOString(),
      messages: prompts.map((message, index) => ({
        ...message,
        audioUrl: message.audioUrl || recordedAudio[index] || ""
      }))
    };
    window.localStorage.setItem(speakingHistoryKey(scenario.id), JSON.stringify(replay));
    navigate(`/speaking/${scenario.id}/feedback`);
  }, [canRecord, navigate, prompts, recordedAudio, scenario]);

  const canSubmit = canRecord && !isRecording && recordingCount >= 3;
  const submitDisabledReason = isRecording
    ? "正在录音时不可交卷"
    : !canRecord
      ? "对话脚本数据缺失，暂时无法交卷"
    : "录音次数超过 3 次后才可交卷";

  return (
    <AsyncPage loading={loading} error={error}>
      {scenario ? (
        <div className="page-stack">
          <section className="glass-panel speaking-session-panel">
            <PageSectionHeader
              eyebrow=""
              title={scenario.title}
              description=""
              extra={
                <Space wrap>
                  <Tag bordered={false} className="soft-tag">
                    当前为第 {recordingCount} 轮
                  </Tag>
                  <Tag bordered={false} className="soft-tag soft-tag--dark">
                    {scenario.level}
                  </Tag>
                </Space>
              }
            />

            {canRecord ? (
              <div className="chat-window chat-window--page">
                {prompts.map((message, index) => (
                  <div
                    className={`chat-bubble-row chat-bubble-row--${message.role}`}
                    key={`${message.role}-${index}`}
                  >
                    <div className={`chat-bubble chat-bubble--${message.role}`}>
                      <span>{message.text}</span>
                      <Button
                        aria-label="播放此句音频"
                        className="chat-audio-button"
                        icon={<SoundOutlined />}
                        shape="circle"
                        size="small"
                        onClick={() => playMessageAudio(message, index)}
                      />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="speaking-alert" role="alert">
                对话脚本数据缺失，当前会话无法开始录音或交卷。请返回情景详情页重新选择。
              </div>
            )}

            <div className={`recorder-strip ${isRecording ? "recorder-strip--recording" : ""}`}>
              <div className="recorder-strip__label">
                <SoundOutlined />
                <span>{isRecording ? "正在录音" : "至少录音 3 轮，方可交卷"}</span>
              </div>
              <div className="waveform-bars" aria-hidden="true">
                {Array.from({ length: 28 }).map((_, index) => (
                  <span key={index} style={{ "--bar": `${18 + ((index * 13) % 34)}px` }} />
                ))}
              </div>
              {isRecording ? <Progress percent={66} showInfo={false} status="active" /> : null}
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
                  disabled={isRecording || !canRecord}
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
                onClick={submitSession}
                disabled={!canSubmit}
                title={canSubmit ? undefined : submitDisabledReason}
              >
                交卷
              </Button>
            </Flex>
            <Text type="secondary">当前为前端模拟录音流程，不会调用真实麦克风或上传音频。</Text>
          </section>
        </div>
      ) : data ? (
        <section className="glass-panel">
          <PageSectionHeader
            eyebrow="Scenario Missing"
            title="没有找到这个情景"
            description="请返回口语页重新选择一个情景模块。"
          />
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/speaking")}>
            返回
          </Button>
        </section>
      ) : null}
    </AsyncPage>
  );
}
