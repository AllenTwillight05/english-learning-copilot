import { useCallback, useMemo } from "react";
import { ArrowLeftOutlined, PlayCircleOutlined } from "@ant-design/icons";
import { Button, Space, Tag, Typography } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import { ScriptPreview } from "../components/Speaking/ScriptPreview";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Text, Title, Paragraph } = Typography;

export function SpeakingScenarioDetailPage() {
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
          <section className="glass-panel speaking-detail-hero">
            <PageSectionHeader
              eyebrow=""
              title={scenario.title}
              description={scenario.goal}
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
              {scenario.keywords.map((keyword) => (
                <Tag bordered={false} className="soft-tag" key={keyword}>
                  {keyword}
                </Tag>
              ))}
            </div>
            <div className="scenario-detail-grid">
              <div className="scenario-detail-block">
                <Text className="panel-title">练习目标</Text>
                <Title level={4}>{scenario.goal}</Title>
                <Paragraph>{scenario.summary}</Paragraph>
              </div>
              <div className="scenario-detail-block">
                <Text className="panel-title">发音与表达</Text>
                <Title level={4}>{scenario.accent}</Title>
              </div>
            </div>
            <Space wrap>
              <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/speaking")}>
                返回
              </Button>
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={() => navigate(`/speaking/${scenario.id}/conversation`)}
              >
                进入会话
              </Button>
            </Space>
          </section>

          <section className="glass-panel">
            <PageSectionHeader
              eyebrow=""
              title={`对话示例`}
              description="当前为前端 mock 脚本，后续可接入剧本详情接口。"
            />
            <ScriptPreview lines={scenario.prompts.map((message) => message.text)} />
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
