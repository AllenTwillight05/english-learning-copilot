import { useCallback, useMemo } from "react";
import { ArrowLeftOutlined, CheckCircleOutlined, ReloadOutlined } from "@ant-design/icons";
import { Button, Space, Statistic, Typography } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Text, Title, Paragraph } = Typography;

export function SpeakingFeedbackPage() {
  const navigate = useNavigate();
  const { scenarioId } = useParams();
  const { speaking } = useAppServices();
  const loader = useCallback(() => speaking.getCatalog(), [speaking]);
  const { data, loading, error } = useAsyncData(loader, [loader]);

  const scenario = useMemo(
    () => data?.scenarios.find((item) => item.id === scenarioId),
    [data, scenarioId]
  );

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
              <Button
                type="primary"
                icon={<ReloadOutlined />}
                onClick={() => navigate(`/speaking/${scenario.id}/conversation`)}
              >
                再练一次
              </Button>
            </Space>
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
            退出
          </Button>
        </section>
      ) : null}
    </AsyncPage>
  );
}
