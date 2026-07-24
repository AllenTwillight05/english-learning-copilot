import { ArrowLeftOutlined, ArrowRightOutlined } from "@ant-design/icons";
import { Button, Space, Tag, Typography } from "antd";
import { useNavigate } from "react-router-dom";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { IELTS_SCENARIOS } from "../services/speakingCatalog";

const { Paragraph, Title } = Typography;

const ieltsParts = [
  { key: "part1", path: "/speaking/ielts/part1" },
  { key: "part2", path: "/speaking/ielts/part2" },
  { key: "part3", path: "/speaking/ielts/part3" },
  {
    key: "mock",
    path: `/speaking/${IELTS_SCENARIOS.mock.scenarioId}/conversation`,
    state: { speakingBackPath: "/speaking/ielts" }
  }
];

export function SpeakingIeltsPage() {
  const navigate = useNavigate();

  return (
    <div className="page-stack">
      <section className="glass-panel">
        <PageSectionHeader
          eyebrow="IELTS Speaking"
          title="雅思口语"
          description="按考试结构进入 Part 1、Part 2、Part 3 或完整模拟。"
          extra={
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/speaking")}>
              返回
            </Button>
          }
        />
      </section>

      <section className="feature-grid">
        {ieltsParts.map((item) => {
          const part = IELTS_SCENARIOS[item.key];

          return (
            <article
              className="scenario-card scenario-card--button"
              key={item.key}
              onClick={() => navigate(item.path, item.state ? { state: item.state } : undefined)}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  navigate(item.path, item.state ? { state: item.state } : undefined);
                }
              }}
              role="button"
              tabIndex={0}
            >
              <div className={`scenario-card__tone scenario-card__tone--${part.tone}`} />
              <div className="scenario-card__body scenario-card__body--spacious">
                <div className="scenario-card__header">
                  <Title level={3} className="scenario-card__title">
                    {part.title}
                  </Title>
                  <Space size={8} wrap>
                    <Tag bordered={false} className="soft-tag soft-tag--dark">
                      {part.level}
                    </Tag>
                    <Tag bordered={false} className="soft-tag">
                      {part.duration}
                    </Tag>
                  </Space>
                </div>
                <Paragraph>{part.subtitle}</Paragraph>
                <Button type="primary" icon={<ArrowRightOutlined />}>
                  进入
                </Button>
              </div>
            </article>
          );
        })}
      </section>
    </div>
  );
}
