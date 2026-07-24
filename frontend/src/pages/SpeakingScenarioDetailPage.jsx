import { useCallback, useMemo, useState } from "react";
import { ArrowLeftOutlined, HistoryOutlined, PlayCircleOutlined } from "@ant-design/icons";
import { Button, Modal, Space, Tag, Typography } from "antd";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { ScriptPreview } from "../components/Speaking/ScriptPreview";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { useAsyncData } from "../hooks/useAsyncData";
import { getStoredAuth } from "../services/authStorage";
import { useAppServices } from "../services/ServiceContext";

const { Text, Title, Paragraph } = Typography;

function toSampleDialogueLines(scenario) {
  if (scenario?.sampleDialogue?.trim()) {
    return scenario.sampleDialogue
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean);
  }

  return (scenario?.prompts ?? []).map((message) => message.text);
}

export function SpeakingScenarioDetailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { scenarioId } = useParams();
  const { speaking } = useAppServices();
  const loader = useCallback(() => speaking.getScenario(scenarioId), [speaking, scenarioId]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);
  const isMissingScenario = error?.status === 404 || error?.message === "Speaking scenario was not found.";

  const scenario = data;
  const backPath = location.state?.speakingBackPath || "/speaking";
  const sampleDialogueLines = useMemo(() => toSampleDialogueLines(scenario), [scenario]);
  const keywords = scenario?.keywords ?? [];
  const hasSampleDialogue = sampleDialogueLines.length > 0;
  const enterConversation = useCallback(() => {
    const { token, user } = getStoredAuth();
    if (!token || !user) {
      Modal.warning({
        title: "用户未登录",
        content: "请先登录后再进入口语会话。",
        okText: "知道了"
      });
      return;
    }

    navigate(`/speaking/${scenario.id}/conversation`, {
      state: { speakingBackPath: `/speaking/${scenario.id}` }
    });
  }, [navigate, scenario]);
  const openLatestHistory = useCallback(async () => {
    const { token, user } = getStoredAuth();
    if (!token || !user) {
      Modal.warning({
        title: "用户未登录",
        content: "请先登录后再查看历史记录。",
        okText: "知道了"
      });
      return;
    }
    if (!scenario) {
      return;
    }

    setIsHistoryLoading(true);
    try {
      const history = await speaking.listHistory();
      const latestSession = history.find((session) => session.scenario?.id === scenario.id);
      if (!latestSession) {
        Modal.info({
          title: "暂无历史记录",
          content: "当前情景还没有可回放的练习记录。",
          okText: "知道了"
        });
        return;
      }

      navigate(`/speaking/${scenario.id}/feedback?sessionId=${encodeURIComponent(latestSession.id)}`);
    } catch (error) {
      Modal.error({
        title: "历史记录读取失败",
        content: error.message || "请稍后重试。",
        okText: "知道了"
      });
    } finally {
      setIsHistoryLoading(false);
    }
  }, [navigate, scenario, speaking]);

  return (
    <AsyncPage loading={loading} error={isMissingScenario ? null : error}>
      {scenario ? (
        <div className="page-stack">
          <section className="glass-panel speaking-detail-hero">
            <PageSectionHeader
              eyebrow=""
              title={scenario.title}
              description={scenario.summary}
              extra={
                <Space wrap>
                  <Tag bordered={false} className="soft-tag soft-tag--dark">
                    {scenario.level}
                  </Tag>
                  <Tag bordered={false} className="soft-tag">
                    {scenario.duration}
                  </Tag>
                </Space>
              }
            />
            <div className="speaking-keywords">
              {keywords.length > 0 ? (
                keywords.map((keyword) => (
                  <Tag bordered={false} className="soft-tag" key={keyword}>
                    {keyword}
                  </Tag>
                ))
              ) : (
                <Text type="secondary">暂无关键词</Text>
              )}
            </div>
            <div className="scenario-detail-grid">
              <div className="scenario-detail-block">
                <Text className="panel-title">练习目标</Text>
                <Title level={4}>{scenario.goal}</Title>
              </div>
              <div className="scenario-detail-block">
                <Text className="panel-title">发音与表达</Text>
                <Title level={4}>{scenario.accent}</Title>
              </div>
            </div>
            <Space wrap>
              <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(backPath)}>
                返回
              </Button>
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={enterConversation}
              >
                进入会话
              </Button>
              <Button
                icon={<HistoryOutlined />}
                loading={isHistoryLoading}
                onClick={openLatestHistory}
              >
                历史记录
              </Button>
            </Space>
          </section>

          <section className="glass-panel">
            <PageSectionHeader
              eyebrow=""
              title={`对话示例`}
              description=""
            />
            {hasSampleDialogue ? (
              <ScriptPreview lines={sampleDialogueLines} />
            ) : (
              <div className="speaking-alert" role="alert">
                暂无对话示例。你仍然可以进入会话，由后端开场白开始练习。
              </div>
            )}
          </section>
        </div>
      ) : isMissingScenario ? (
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
